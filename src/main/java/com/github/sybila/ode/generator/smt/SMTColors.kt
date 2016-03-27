package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.Colors
import com.microsoft.z3.*
import java.util.*

class SMTContext(
        val ctx: Context,
        val simplifyTactic: Tactic,
        val solveTactic: Tactic,
        val goal: Goal,
        val timeToSolve: Int,
        val timeToNormalize: Int
)

var timeToNormalize = -1
var solverCalls = 0
var simplifyCalls = 0
var timeInSimplify = 0L
var timeInSolver = 0L

val formulaCache = HashMap<BoolExpr, BoolExpr>()
val solverCache = HashMap<CNF, Boolean>()
val simplifyCache = HashMap<CNF, CNF>()
var cacheHit = 0

class Clause(
        val literals: Set<BoolExpr>,
        val order: PartialOrderSet,
        val simplified: Boolean = false
) {

    fun asFormula() = if (literals.isEmpty()) z3.mkFalse() else z3.mkOr(*literals.toTypedArray())

    infix fun or(other: Clause): Clause = Clause(literals + other.literals, order, false)

    infix fun and(other: Clause): CNF = CNF(setOf(this, other), order)

    fun not(): CNF {
        if (literals.isEmpty()) return CNF(setOf(), order)
        else return CNF(literals.map { Clause(setOf(z3.mkNot(it).simplify() as BoolExpr), order, true) }.toSet(), order, true)
    }

    fun simplify(): Clause? {
        if (simplified) return this
        val working = literals.filter { order.add(it) }.toMutableList() //add negated literals if necessary
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
        return literals.toString()
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
            return simplifyCache[this]!!
        }
        if (simplified) return this
        val start = System.nanoTime()
        val simplified = clauses.map { it.simplify() }.filterNotNull()
        val working = simplified.toMutableList()
        working.removeAll { a ->
            simplified.any { b -> a != b && a.covers(b) }
        }
        timeInSimplify += System.nanoTime() - start
        val r = CNF(working.toSet(), order, true)
        simplifyCache[this] = r
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
        return clauses.toString()
    }
}

class SMTColors(
        internal var formula: BoolExpr,
        val cnf: CNF,
        private val context: SMTContext,
        private val order: PartialOrderSet,
        private var sat: Boolean? = null
        //private val timeToSolve: Int = context.timeToSolve,
        //private val timeToNormalize: Int = context.timeToNormalize
) : Colors<SMTColors> {

    private var normalized = false

    override fun isEmpty(): Boolean {
        return (sat ?: solverCache[cnf].apply { cacheHit += 1 } ?: run {
            /*if (timeToNormalize < 0) {
                normalize()
                timeToNormalize = context.timeToNormalize
            } else {
                timeToNormalize -= 1
                solve()
            }*/
            solve()
            sat ?: true
        }).not() //un-sat ~ empty
    }

    fun solve() {
        if (sat == null) {
            context.goal.add(z3.mkAnd(cnf.asFormula(), z3.mkAnd(*order.paramBounds)))
            val start = System.nanoTime()
            val result = context.solveTactic.apply(context.goal).subgoals
            timeInSolver += System.nanoTime() - start
            assert(result.size == 1)
            solverCalls += 1
            if (solverCalls % 100 == 0) println("Calls $solverCalls")
            context.goal.reset()
            sat = result.first().isDecidedSat
            solverCache[cnf] = result.first().isDecidedSat
        }
    }

    fun normalize(): SMTColors {
        if (!normalized) {
            if (formula in formulaCache) {
                cacheHit += 1
                formula = formulaCache[formula]!!
            } else {
                context.goal.add(formula)
                val start = System.nanoTime()
                val result = context.simplifyTactic.apply(context.goal).subgoals
                timeInSimplify += System.nanoTime() - start
                assert(result.size == 1)
                formulaCache[formula] = result[0].AsBoolExpr()
                formula = result[0].AsBoolExpr()
                simplifyCalls += 1
                context.goal.reset()
                if (simplifyCalls % 100 == 0) println("Calls $simplifyCalls")
            }
            sat = !formula.isFalse
            normalized = true
        }
        return this
    }

    fun validate() {
        if (this.minus(SMTColors(z3.mkAnd(cnf.asFormula(), z3.mkAnd(*order.paramBounds)), cnf, context, order, null)).isNotEmpty()) {
            throw IllegalStateException("Inconsistency! $this")
        }
        if (SMTColors(z3.mkAnd(cnf.asFormula(), z3.mkAnd(*order.paramBounds)), cnf, context, order, null).minus(this).isNotEmpty()) {
            throw IllegalStateException("Inconsistency! $this")
        }
    }

    override fun minus(other: SMTColors): SMTColors {
        val new = cnf.and(other.cnf.not()).simplify()
        val colors = SMTColors(formula, new, context, order, sat weakMinus other.sat)
       /* if (colors.isNotEmpty()) {
            println("Subtract from ${this.cnf} ${other.cnf} into $new, before simplify: ${cnf.and(other.cnf.not())}, negation: ${other.cnf.not()}")
        }*/
        return colors
    }

    override fun plus(other: SMTColors): SMTColors {
        val new = cnf.or(other.cnf).simplify()
       // println("Union ${this.cnf} and ${other.cnf} into $new")
        return SMTColors(formula, new, context, order, sat weakOr other.sat)
    }

    override fun intersect(other: SMTColors): SMTColors {
        val new = cnf.and(other.cnf).simplify()
       // println("Intersect ${this.cnf} and ${other.cnf} into $new")
        return SMTColors(formula, new, context, order, sat weakAnd other.sat)
    }

    private fun mkAnd(f1: BoolExpr, f2: BoolExpr): BoolExpr {
        return context.ctx.mkAnd(f1, f2)
    }

    private fun mkOr(f1: BoolExpr, f2: BoolExpr): BoolExpr {
        return context.ctx.mkOr(f1, f2)
    }

    private fun mkNot(f1: BoolExpr): BoolExpr {
        return context.ctx.mkNot(f1)
    }
/*
    private fun nextSolve(other: SMTColors): Int {
        return if (sat != null || other.sat != null) {   //this formula is solved, rest the counter
            context.timeToSolve
        } else {
            Math.min(timeToSolve, other.timeToSolve) - 1
        }
    }

    private fun nextNormalize(other: SMTColors): Int {
        return if (normalized || other.normalized) {
            context.timeToNormalize
        } else {
            Math.min(timeToNormalize, other.timeToNormalize) - 1
        }
    }*/

    private infix fun Boolean?.weakAnd(other: Boolean?): Boolean? = when {
        this == false || other == false -> false
        else -> null
    }

    private infix fun Boolean?.weakOr(other: Boolean?): Boolean? = when {
        this == true || other == true -> true
        else -> null
    }

    private infix fun Boolean?.weakMinus(other: Boolean?): Boolean? = when {
        this == false -> false
        other == false -> this
        else -> null
    }

    override fun toString(): String{
        return "SMTColors(sat=$sat, formula=$formula, cnf=$cnf)"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SMTColors) return false
        if (this.context != other.context) return false
        return formula.equals(other.formula)
    }

    override fun hashCode(): Int {
        return this.context.hashCode() * 31 + formula.hashCode()
    }

}