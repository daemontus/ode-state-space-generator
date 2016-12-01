package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.Colors
import com.microsoft.z3.*

val ff = SMTColors(z3False, ctx, false)
val tt = SMTColors(z3True, ctx, true)

class SMTContext(
        val ctx: Context,
        val tactic: Tactic,
        val purify: Tactic,
        val goal: Goal,
        val solver: Solver
)

class SMTColors(
        private var formula: BoolExpr,
        private val context: SMTContext,
        private var sat: Boolean? = null
) : Colors<SMTColors> {

    private var normalized = false

    override fun isEmpty(): Boolean {
        return (sat ?: run {
            if (Math.random() > 0.6) {
                normalize()
            } else {
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

    fun purify(): SMTColors {
        context.goal.add(formula)
        formula = context.purify.apply(context.goal, context.ctx.mkParams().apply {
            this.add("arith_lhs", true)
        }).subgoals[0].AsBoolExpr()
        context.goal.reset()
        return this
    }

    fun strict(): SMTColors {
        return SMTColors(this.formula.asStrict(), ctx, null)
    }

    override fun minus(other: SMTColors): SMTColors {
        return SMTColors(mkAnd(formula, mkNot(other.formula)), context, sat weakMinus other.sat)
    }

    override fun plus(other: SMTColors): SMTColors {
        return SMTColors(mkOr(formula, other.formula), context, sat weakOr other.sat)
    }

    override fun intersect(other: SMTColors): SMTColors {
        return SMTColors(mkAnd(formula, other.formula), context, sat weakAnd other.sat)
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

    fun solverEquals(other: SMTColors): Boolean {
        val f1 = this.formula//this.purify().strict().formula
        val f2 = other.formula//other.purify().strict().formula
        val thisAndNotThat = SMTColors(
                context.ctx.mkAnd(f1, context.ctx.mkNot(f2)),
                context, null
        )
        val thatAndNotThis = SMTColors(
                context.ctx.mkAnd(f2, context.ctx.mkNot(f1)),
                context, null
        )
        return thisAndNotThat.isEmpty() && thatAndNotThis.isEmpty()
    }

    override fun toString(): String{
        return "SMTColors(sat=$sat, formula=$formula)"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SMTColors) return false
        if (this.context != other.context) return false
        return formula == other.formula
    }

    override fun hashCode(): Int {
        return this.context.hashCode() * 31 + formula.hashCode()
    }

}