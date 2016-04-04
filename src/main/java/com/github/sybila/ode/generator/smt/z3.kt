package com.github.sybila.ode.generator.smt

import com.microsoft.z3.*
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

fun BoolExpr.calculateBufferSize(): Int {
    if (!this.isGT && !this.isGE && !this.isLE && ! this.isLT) {
        throw IllegalStateException("Cannot serialize $this")
    }
    val left = this.args[0] as ArithExpr
    val right = this.args[1] as ArithExpr
    return left.calculateBufferSize() + right.calculateBufferSize() + 1
}

fun BoolExpr.serialize(buffer: LongArray, start: Int, params: List<RealExpr>): Int {
    when {
        this.isGT -> buffer[start] = 1L
        this.isGE -> buffer[start] = 2L
        this.isLT -> buffer[start] = 3L
        this.isLE -> buffer[start] = 4L
    }
    val left = this.args[0] as ArithExpr
    val right = this.args[1] as ArithExpr
    var newStart = start + 1
    newStart = left.serialize(buffer, newStart, params)
    newStart = right.serialize(buffer, newStart, params)
    return newStart
}

fun ArithExpr.calculateBufferSize(): Int {
    when {
        this.isAdd || this.isMul -> {
            return this.args.sumBy { (it as ArithExpr).calculateBufferSize() } + 1
        }
        this.isRatNum -> {
            return 3
        }
        this.isReal -> {
            return 1
        }
        else -> throw IllegalStateException("Cannot serialize $this")
    }
}

fun ArithExpr.serialize(buffer: LongArray, start: Int, params: List<RealExpr>): Int {
    return when {
        this.isAdd -> {
            buffer[start] = params.size + args.size.toLong() + 1
            var newStart = start + 1
            args.forEach { arg ->
                newStart = (arg as ArithExpr).serialize(buffer, newStart, params)
            }
            newStart
        }
        this.isMul -> {
            buffer[start] = -1L * args.size.toLong()
            var newStart = start + 1
            args.forEach { arg ->
                newStart = (arg as ArithExpr).serialize(buffer, newStart, params)
            }
            newStart
        }
        this.isRatNum -> {
            var r = this as RatNum
            buffer[start] = 0
            buffer[start+1] = r.bigIntNumerator.toLong()
            buffer[start+2] = r.bigIntDenominator.toLong()
            start + 3
        }
        this.isReal -> {
            buffer[start] = params.indexOf(this).toLong() + 1
            if (buffer[start] < 1) {
                throw IllegalStateException("Real not found: $this")
            }
            start + 1
        }
        else -> throw IllegalStateException("Cannot serialize $this")
    }
}

fun LongArray.readArithExpr(start: Int, params: List<RealExpr>): Pair<ArithExpr, Int> {
    val key = this[start]
    if (key == 0L) { //rat num
        val numerator = this[start+1].toDouble()
        val denominator = this[start+2].toDouble()
        return Pair((numerator/denominator).toZ3(), start + 3)
    } else if (key > 0L && key <= params.size) {
        val index = key - 1
        return Pair(params[index.toInt()], start + 1)
    } else if (key > 0L) {
        val arguments = key.toInt() - params.size - 1
        var newStart = start + 1
        val args = Array(arguments) {
            val (e, s) = this.readArithExpr(newStart, params)
            newStart = s
            e
        }
        return Pair(z3.mkAdd(*args), newStart)
    } else if (key < 0L) {
        val arguments = -1 * key.toInt()
        var newStart = start + 1
        val args = Array(arguments) {
            val (e, s) = this.readArithExpr(newStart, params)
            newStart = s
            e
        }
        return Pair(z3.mkMul(*args), newStart)
    } else {
        throw IllegalStateException("Unknown key: $key")
    }
}

fun LongArray.readBoolExpr(start: Int, params: List<RealExpr>): Pair<BoolExpr, Int> {
    val (left, rightStart) = this.readArithExpr(start + 1, params)
    val (right, newStart) = this.readArithExpr(rightStart, params)
    val e = when {
        this[start] == 1L -> left gt right
        this[start] == 2L -> left ge right
        this[start] == 3L -> left lt right
        this[start] == 4L -> left le right
        else -> throw IllegalStateException("Unknown operation: ${this[start]}")
    }
    return e to newStart
}

fun LongArray.readClause(start: Int, order: PartialOrderSet): Pair<Clause, Int> {
    val args = this[start]
    var newStart = start + 1
    val literals = (1..args).map {
        val (l, s) = this.readBoolExpr(newStart, order.params)
        newStart = s
        l
    }
    return Clause(literals.toSet(), order) to newStart
}

fun LongArray.readCNF(start: Int, order: PartialOrderSet): CNF {
    val args = this[start]
    var newStart = start + 1
    val clauses = (1..args).map {
        val (l, s) = this.readClause(newStart, order)
        newStart = s
        l
    }
    return CNF(clauses.toSet(), order)
}


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

    fun not(): CNF = CNF(literals.map {
        try {
            Clause(setOf(if (it.isNot) it.args[0] as BoolExpr else z3.mkNot(it)/*.simplify() as BoolExpr*/), order, true)
        } catch (e: Exception) {
            println("Problem simplifying $it as ${z3.mkNot(it)}")
            throw e
        }
    }.toSet(), order, true)

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

    fun calculateBufferSize(): Int {
        return literals.sumBy { it.calculateBufferSize() } + 1
    }

    fun serialize(buffer: LongArray, start: Int): Int {
        buffer[start] = literals.size.toLong()
        var newStart = start + 1
        literals.forEach {
            newStart = it.serialize(buffer, newStart, order.params)
        }
        return newStart
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
        simplifyCalls += 1
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

    fun calculateBufferSize(): Int {
        return clauses.sumBy { it.calculateBufferSize() } + 1
    }

    fun serialize(buffer: LongArray, start: Int): Int {
        buffer[start] = clauses.size.toLong()
        var newStart = start + 1
        clauses.forEach {
            newStart = it.serialize(buffer, newStart)
        }
        return newStart
    }

}
