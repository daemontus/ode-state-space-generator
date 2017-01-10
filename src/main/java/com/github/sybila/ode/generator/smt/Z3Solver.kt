package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.Solver
import com.github.sybila.huctl.CompareOp
import com.github.sybila.ode.generator.smt.bridge.RemoteZ3
import com.github.sybila.ode.generator.smt.bridge.readSMT
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.safeString
import java.io.Closeable
import java.nio.ByteBuffer

private var lastProgressPrint = 0L
private var solverCalls = 0L

class Z3Solver(bounds: List<Pair<Double, Double>>, names: List<String> = bounds.indices.map { "p$it" })
    : Solver<Z3Params>, Closeable, Z3SolverBase {

    private val z3 = RemoteZ3(bounds.zip(names).map { OdeModel.Parameter(it.second, it.first) }, verbose = false)

    override val bounds: Z3Formula
        get() = z3.bounds

    override fun close() {
        z3.close()
    }

    override val ff: Z3Params = Z3Params(Z3Formula.False, false, true)
    override val tt: Z3Params = Z3Params(Z3Formula.True, true, true)

    override fun Z3Params.isSat(): Boolean {
        solverCalls += 1
        if (System.currentTimeMillis() > lastProgressPrint + 2000) {
            System.err.println("Processing: ${solverCalls / 2.0} per second")
            solverCalls = 0
            lastProgressPrint = System.currentTimeMillis()
        }
        //if (Math.random() > 0.8) minimize()
        return this.sat ?: run {
            val sat = z3.checkSat(this.formula)
            this.sat = sat
            sat
        }
    }

    override fun Z3Params.minimize() {
        if (!minimal) {
            val simplified = z3.minimize(this.formula)
            this.formula = simplified
            sat = simplified != Z3Formula.False
            minimal = true
        }
    }

    override fun Z3Params.and(other: Z3Params): Z3Params {
        return if (this.formula.isFalse || this.sat == false) ff
        else if (this.formula.isTrue) other
        else if (other.formula.isFalse || other.sat == false) ff
        else if (other.formula.isTrue) this
        else {
            return Z3Params(Z3Formula.And(listOf(this.formula, other.formula)), null)
        }
    }

    override fun Z3Params.not(): Z3Params {
        return if (this.formula.isTrue) ff
        else if (this.sat == false || this.formula.isFalse) tt
        else {
            return Z3Params(Z3Formula.Not(this.formula), null)
        }
    }

    override fun Z3Params.or(other: Z3Params): Z3Params {
        return if (this.formula.isTrue || other.formula.isTrue) tt
        else if (this.formula.isFalse || this.sat == false) other
        else if (other.formula.isFalse || other.sat == false) this
        else {
            return Z3Params(Z3Formula.Or(listOf(this.formula, other.formula)), if (this.sat == true || other.sat == true) true else null)
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
                string.readSMT().asZ3Formula(),
                when (this.get().toInt()) {
                    1 -> true
                    0 -> false
                    else -> null
                }
        )
    }

    private fun Z3Params.ensureBytes(): ByteArray {
        return asString ?: formula.asCommand().toByteArray().apply { asString = this }
    }

    override fun Z3Params.transferTo(solver: Solver<Z3Params>): Z3Params {
        return this
    }

}

interface Z3SolverBase {

    val bounds: Z3Formula

    fun String.toZ3() = Z3Formula.Value(this)
    fun Double.toZ3() = Z3Formula.Value(this.safeString())
    fun Int.toZ3() = Z3Formula.Value(this.toString())

    infix fun Z3Formula.and(other: Z3Formula) = Z3Formula.And(listOf(this, other))
    infix fun Z3Formula.or(other: Z3Formula) = Z3Formula.Or(listOf(this, other))

    infix fun Z3Formula.gt(other: Z3Formula) = Z3Formula.Compare(this, CompareOp.GT, other)
    infix fun Z3Formula.ge(other: Z3Formula) = Z3Formula.Compare(this, CompareOp.GE, other)
    infix fun Z3Formula.lt(other: Z3Formula) = Z3Formula.Compare(this, CompareOp.LT, other)
    infix fun Z3Formula.le(other: Z3Formula) = Z3Formula.Compare(this, CompareOp.LE, other)

    infix fun Z3Formula.plus(other: Z3Formula) = Z3Formula.Plus(listOf(this, other))
    infix fun Z3Formula.times(other: Z3Formula) = Z3Formula.Times(listOf(this, other))
    infix fun Z3Formula.div(other: Z3Formula) = Z3Formula.Div(listOf(this, other))

}