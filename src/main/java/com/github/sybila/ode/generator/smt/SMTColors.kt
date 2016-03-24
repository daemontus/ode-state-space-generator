package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.Colors
import com.microsoft.z3.*

class SMTContext(
        val ctx: Context,
        val tactic: Tactic,
        val goal: Goal,
        val solver: Solver,
        val timeToSolve: Int,
        val timeToNormalize: Int
)

class SMTColors(
        private var formula: BoolExpr,
        private val context: SMTContext,
        private var sat: Boolean? = null,
        private val timeToSolve: Int = context.timeToSolve,
        private val timeToNormalize: Int = context.timeToNormalize
) : Colors<SMTColors> {

    private var normalized = false

    override fun intersect(other: SMTColors): SMTColors {
        return SMTColors(mkAnd(formula, other.formula), context, sat weakAnd other.sat, nextSolve(), nextNormalize())
    }

    override fun isEmpty(): Boolean {
        return (sat ?: run {
            if (timeToNormalize < 0) {
                normalize()
            } else if (timeToSolve < 0) {
                solve()
            }
            sat ?: true
        }).not() //un-sat ~ empty
    }

    fun solve() {
        if (sat == null) {
            context.solver.add(formula)
            sat = context.solver.check() == Status.SATISFIABLE
            context.solver.reset()
        }
    }

    fun normalize(): SMTColors {
        if (!normalized) {
            context.goal.add(formula)
            val result = context.tactic.apply(context.goal).subgoals
            assert(result.size == 1)
            formula = result[0].AsBoolExpr()
            sat = !formula.isFalse
            context.goal.reset()
            normalized = true
        }
        return this
    }

    override fun minus(other: SMTColors): SMTColors {
        return SMTColors(mkAnd(formula, mkNot(other.formula)), context, sat weakMinus other.sat, nextSolve(), nextNormalize())
    }

    override fun plus(other: SMTColors): SMTColors {
        return SMTColors(mkOr(formula, other.formula), context, sat weakOr other.sat, nextSolve(), nextNormalize())
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

    private fun nextSolve(): Int {
        return if (sat != null) {   //this formula is solved, rest the counter
            context.timeToSolve
        } else {
            timeToSolve - 1
        }
    }

    private fun nextNormalize(): Int {
        return if (normalized) {
            context.timeToNormalize
        } else {
            timeToNormalize - 1
        }
    }

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
        return "SMTColors(sat=$sat, formula=$formula)"
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