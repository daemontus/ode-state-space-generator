package com.github.sybila.ode.generator.smt

import com.microsoft.z3.ArithExpr
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import java.util.*

//Z3 doesn't support any parallelism or anything, so we might as well just make it all static...

val z3 = Context()

fun BoolExpr.not() = z3.mkNot(this)
fun Status.isUnsat() = this == Status.UNSATISFIABLE
fun Status.isSat() = this == Status.SATISFIABLE

val z3True = z3.mkTrue()
val z3False = z3.mkFalse()

fun String.toZ3() = z3.mkRealConst(this)
fun Double.toZ3() = z3.mkReal(this.toString())
fun Int.toZ3() = z3.mkReal(this)

infix fun BoolExpr.and(other: BoolExpr) = z3.mkAnd(this, other)
infix fun BoolExpr.or(other: BoolExpr) = z3.mkOr(this, other)

infix fun ArithExpr.gt(other: ArithExpr) = z3.mkGt(this, other)
infix fun ArithExpr.ge(other: ArithExpr) = z3.mkGe(this, other)
infix fun ArithExpr.lt(other: ArithExpr) = z3.mkLt(this, other)
infix fun ArithExpr.le(other: ArithExpr) = z3.mkLe(this, other)

infix fun ArithExpr.plus(other: ArithExpr) = z3.mkAdd(this, other)
infix fun ArithExpr.times(other: ArithExpr) = z3.mkMul(this, other)

//stats
var timeInSimplify = 0L
var simplifyCacheHit = 0L
var simplifyCalls = 0
val simplifyCache = HashMap<CNF, CNF>()

/** Classes implementing a conjunctive normal form and related normalisation procedures **/


class Clause(
        val literals: Set<BoolExpr>,
        val order: PartialOrderSet,
        val simplified: Boolean = false
) {

    fun asFormula() = if (literals.isEmpty()) z3.mkFalse() else z3.mkOr(*literals.toTypedArray())

    infix fun or(other: Clause): Clause = Clause(literals + other.literals, order, false)

    infix fun and(other: Clause): CNF = CNF(setOf(this, other), order)

    fun not(): CNF = CNF(literals.map { Clause(setOf(z3.mkNot(it).simplify() as BoolExpr), order, true) }.toSet(), order, true)

    fun simplify(): Clause? {
        if (simplified) return this
        //add literals if necessary (can come from negation or messages) and filter out unsat
        val working = literals.filter { order.add(it) }.toMutableList()
        if (working.any { a -> working.any { b ->
            a == z3.mkNot(b)
        } }) return null        // a || !a - tautology
        working.removeAll { a ->
            literals.any { b -> order.bigger(a, b) == b }
        }
        return Clause(working.toSet(), order, true)
    }

    //true if this is superset of other
    fun covers(other: Clause): Boolean {
        if (this.literals.isEmpty()) return false
        if (other.literals.isEmpty()) return true
        return other.literals.all { o ->
            this.literals.any { t ->
                order.bigger(t, o) == t
            }
        }
    }

    override fun hashCode(): Int {
        return literals.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Clause) this.literals.equals(other.literals)
        else false
    }

    override fun toString(): String {
        return "("+literals.joinToString(" or ")+")"
    }
}

class CNF(
        val clauses: Set<Clause>,
        val order: PartialOrderSet,
        val simplified: Boolean = false
) {

    fun asFormula() = if (clauses.isEmpty()) z3.mkTrue() else z3.mkAnd(*clauses.map { it.asFormula() }.toTypedArray())

    infix fun and(other: CNF): CNF {
        return CNF(this.clauses + other.clauses, order, false)
    }

    infix fun or(other: CNF): CNF {
        return CNF(this.clauses.flatMap { a ->
            other.clauses.map { b ->
                (a or b)
            }
        }.toSet(), order, false)
    }

    fun not(): CNF {
        if (this.clauses.isEmpty()) return CNF(setOf(Clause(setOf(), order)), order, true)
        return clauses.map { it.not() }.fold(CNF(setOf(Clause(setOf(), order)), order, true)) { a, b -> a or b }
    }

    fun simplify(): CNF {
        if (this in simplifyCache) {
            simplifyCacheHit += 1
            return simplifyCache[this]!!
        }
        if (simplified) return this
        val start = System.nanoTime()
        val simplified = clauses.map { it.simplify() }.filterNotNull()
        val working = simplified.toMutableList()
        working.removeAll { a ->
            simplified.any { b -> a != b && a.covers(b) }
        }
        val r = CNF(working.toSet(), order, true)
        simplifyCache[this] = r
        timeInSimplify += System.nanoTime() - start
        return r
    }

    override fun equals(other: Any?): Boolean {
        return if (other is CNF) this.clauses.equals(other.clauses)
        else false
    }

    override fun hashCode(): Int {
        return clauses.hashCode()
    }

    override fun toString(): String {
        return "("+clauses.joinToString(" and ")+")"
    }
}
