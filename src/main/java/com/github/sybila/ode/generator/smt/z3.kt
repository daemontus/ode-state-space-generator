package com.github.sybila.ode.generator.smt

import com.microsoft.z3.ArithExpr
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import com.sun.org.apache.xpath.internal.operations.Bool

//Z3 doesn't support any parallelism or anything, so we might as well just make it all static...

val z3 = Context()

val ctx = SMTContext(
        ctx = z3,
        tactic = z3.mkTactic("ctx-solver-simplify"),
        purify = z3.mkTactic("purify-arith"),
        goal = z3.mkGoal(false, false, false),
        solver = z3.mkSolver("qflra")
)

fun BoolExpr.asColors(): SMTColors = SMTColors(
        this, ctx, null
)

fun BoolExpr.not() = z3.mkNot(this)!!
fun Status.isUnsat() = this == Status.UNSATISFIABLE
fun Status.isSat() = this == Status.SATISFIABLE

val z3True = z3.mkTrue()!!
val z3False = z3.mkFalse()!!

fun String.toZ3() = z3.mkRealConst(this)!!
fun Double.toZ3() = z3.mkReal(this.toString())!!
fun Int.toZ3() = z3.mkReal(this)!!

infix fun BoolExpr.and(other: BoolExpr) = z3.mkAnd(this, other)!!
infix fun BoolExpr.or(other: BoolExpr) = z3.mkOr(this, other)!!

infix fun ArithExpr.gt(other: ArithExpr) = z3.mkGt(this, other)!!
infix fun ArithExpr.ge(other: ArithExpr) = z3.mkGe(this, other)!!
infix fun ArithExpr.lt(other: ArithExpr) = z3.mkLt(this, other)!!
infix fun ArithExpr.le(other: ArithExpr) = z3.mkLe(this, other)!!

infix fun ArithExpr.plus(other: ArithExpr) = z3.mkAdd(this, other)!!
infix fun ArithExpr.times(other: ArithExpr) = z3.mkMul(this, other)!!
infix fun ArithExpr.div(other: ArithExpr) = z3.mkDiv(this, other)!!

fun BoolExpr.asStrict(): BoolExpr {
    val weakInequalities = weakInequalities()
    val strictInequalities = weakInequalities.map {
        if (it.isGE) it.args[0] as ArithExpr gt it.args[1] as ArithExpr
        else if (it.isLE) it.args[0] as ArithExpr lt it.args[1] as ArithExpr
        else error("WTF?! $it")
    }
    return this.substitute(weakInequalities.toTypedArray(), strictInequalities.toTypedArray()) as BoolExpr
}

fun BoolExpr.weakInequalities(): List<BoolExpr> {
    return if (this.isGE || this.isLE) {
        listOf(this)
    } else {
        this.args.flatMap { if (it is BoolExpr) it.weakInequalities() else listOf() }
    }
}