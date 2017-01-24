package com.github.sybila.ode.generator.smt.local

import com.github.sybila.checker.Solver
import com.github.sybila.checker.solver.SolverStats
import com.microsoft.z3.ArithExpr
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import java.nio.ByteBuffer

class Z3Solver(bounds: List<Pair<Double, Double>>, names: List<String> = bounds.indices.map { "p$it" })
    : Solver<Z3Params>, Z3SolverBase {

    override val z3 = Context()
    private val simplifyTactic = z3.mkTactic("ctx-solver-simplify")
    private val goal = z3.mkGoal(false, false, false)
    private val solver = z3.mkSolver(z3.mkTactic("qflra"))

    override val ff: Z3Params = Z3Params(z3.mkFalse(), false, true)
    override val tt: Z3Params = Z3Params(z3.mkTrue(), true, true)

    override val params: List<ArithExpr> = names.map { it.toZ3() }

    val paramSymbols = bounds.indices.map { z3.mkSymbol("p$it") }.toTypedArray()
    val paramSorts = bounds.indices.map { z3.mkRealSort() }.toTypedArray()

    val bounds: BoolExpr = z3.mkAnd(*bounds.mapIndexed { i, pair ->
        val (low, high) = pair
        listOf(params[i] gt low.toZ3(), params[i] lt high.toZ3())
    }.flatMap { it }.toTypedArray())

    override fun Z3Params.isSat(): Boolean {
        SolverStats.solverCall()
        if (Math.random() > 0.8) minimize()
        return this.sat ?: run {
            solver.add(bounds)
            solver.add(formula)
            val status = solver.check()
            solver.reset()
            if (status == Status.UNKNOWN) throw IllegalStateException("Unknown solver result")
            val sat = status == Status.SATISFIABLE
            this.sat = sat
            sat
        }
    }

    override fun Z3Params.minimize() {
        if (!minimal) {
            goal.add(bounds)
            goal.add(formula)
            val simplified = simplifyTactic.apply(goal)
            goal.reset()
            if (simplified.numSubgoals != 1) throw IllegalStateException("Unexpected simplification result")
            formula = simplified.subgoals[0].AsBoolExpr()
            sat = !formula.isFalse
            minimal = true
        }
    }

    override fun Z3Params.andNot(other: Z3Params): Boolean {
        solver.add(bounds)
        solver.add(formula)
        solver.add(z3.mkNot(other.formula))
        val status = solver.check()
        solver.reset()
        return status == Status.SATISFIABLE
    }

    override fun Z3Params.and(other: Z3Params): Z3Params {
        return if (this.formula.isFalse || this.sat == false) ff
        else if (this.formula.isTrue) other
        else if (other.formula.isFalse || other.sat == false) ff
        else if (other.formula.isTrue) this
        else {
            return Z3Params(this.formula and other.formula, null)
        }
    }

    override fun Z3Params.not(): Z3Params {
        return if (this.formula.isTrue) ff
        else if (this.sat == false || this.formula.isFalse) tt
        else {
            return Z3Params(z3.mkNot(this.formula), null)
        }
    }

    override fun Z3Params.or(other: Z3Params): Z3Params {
        return if (this.formula.isTrue || other.formula.isTrue) tt
        else if (this.formula.isFalse || this.sat == false) other
        else if (other.formula.isFalse || other.sat == false) this
        else {
            return Z3Params(this.formula or other.formula, if (this.sat == true || other.sat == true) true else null)
        }
    }

    override fun Z3Params.prettyPrint(): String {
        //this.minimize()
        return "{$sat: $formula}"
    }

    override fun Z3Params.byteSize(): Int = ensureBytes().size + 4 + 1

    override fun ByteBuffer.putColors(colors: Z3Params): ByteBuffer {
        val bytes = colors.ensureBytes()
        this.putInt(bytes.size)
        bytes.forEach { this.put(it) }
        colors.asString = null  //release bytes - chance we will be sending the same data again is very small
        when (colors.sat) {
            null -> put((-1).toByte())
            true -> put(1.toByte())
            false -> put(0.toByte())
        }
        return this
    }

    override fun ByteBuffer.getColors(): Z3Params {
        val size = this.int
        val array = ByteArray(size)
        (0 until size).forEach { array[it] = this.get() }
        val string = String(array)
        return Z3Params(
                z3.parseSMTLIB2String(string, paramSymbols, paramSorts, null, null),
                when (this.get().toInt()) {
                    1 -> true
                    0 -> false
                    else -> null
                }
        )
    }

    private fun Z3Params.ensureBytes(): ByteArray {
        return asString ?: formula.toString().toByteArray().apply { asString = this }
    }

    override fun Z3Params.transferTo(solver: Solver<Z3Params>): Z3Params {
        solver as Z3Solver
        val f = this.formula.translate(solver.z3) as BoolExpr
        return Z3Params(f, this.sat, this.minimal)
    }

}

interface Z3SolverBase {

    val z3: Context
    val params: List<ArithExpr>

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

}