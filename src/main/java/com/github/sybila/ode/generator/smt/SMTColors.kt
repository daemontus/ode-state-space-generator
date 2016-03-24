package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.Colors
import com.microsoft.z3.*

class SMTContext(
        val ctx: Context,
        val tactic: Tactic,
        val goal: Goal
)

class SMTColors(
        private var formula: BoolExpr,
        private val context: SMTContext,
        private var sat: Boolean? = null
) : Colors<SMTColors> {

    override fun intersect(other: SMTColors): SMTColors {
        return SMTColors(mkAnd(formula, other.formula), context, sat weakAnd other.sat)
    }

    override fun isEmpty(): Boolean {
        return (sat ?: run {
            normalize()
            sat!!
        }).not() //un-sat ~ empty
    }

    fun normalize(): SMTColors {
        context.goal.add(formula)
        val result = context.tactic.apply(context.goal).subgoals
        assert(result.size == 1)
        formula = result[0].AsBoolExpr()
        sat = !formula.isFalse
        context.goal.reset()
        return this
    }

    override fun minus(other: SMTColors): SMTColors {
        return SMTColors(mkAnd(formula, mkNot(other.formula)), context, sat weakMinus other.sat)
    }

    override fun plus(other: SMTColors): SMTColors {
        return SMTColors(mkOr(formula, other.formula), context, sat weakOr other.sat)
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