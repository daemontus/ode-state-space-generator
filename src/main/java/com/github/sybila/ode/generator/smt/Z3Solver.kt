package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.Solver
import com.github.sybila.ode.generator.smt.bridge.RemoteZ3
import com.github.sybila.ode.generator.smt.bridge.SMT
import com.github.sybila.ode.generator.smt.bridge.readSMT
import com.github.sybila.ode.model.OdeModel
import java.io.Closeable
import java.nio.ByteBuffer


private fun pow (a: Int, b: Int): Int {
    if ( b == 0)        return 1
    if ( b == 1)        return a
    if ( b % 2 == 0)    return pow (a * a, b / 2)       //even a=(a^2)^b/2
    else                return a * pow (a * a, b / 2)   //odd  a=a*(a^2)^b/2
}

class Z3Solver(bounds: List<Pair<Double, Double>>, names: List<String> = bounds.indices.map { "p$it" })
    : Solver<Z3Params>, Closeable, Z3SolverBase {

    private val z3 = RemoteZ3(bounds.zip(names).map { OdeModel.Parameter(it.second, it.first) }, verbose = false)

    private val cornerPoints = (0 until pow(2, bounds.size)).map { mask ->
        bounds.mapIndexed { i, pair -> if (mask.shl(i).and(1) == 1) pair.second else pair.first }
    }.map { names.zip(it).toMap() }.plus(names.zip(bounds.map { (it.first + it.second) / 2 }).toMap())

    init {
        println(cornerPoints)
    }

    override val bounds: String
        get() = z3.bounds

    override fun close() {
        z3.close()
    }

    override val ff: Z3Params = Z3Params("false", false, true)
    override val tt: Z3Params = Z3Params("true", true, true)

    private var coreSize = 0

    override fun Z3Params.isSat(): Boolean {
        if (this.formula.checkCorners()) {
            return true
        }
        this.sat ?: run {
            val sat = z3.checkSat(this.formula)
            this.sat = sat
            sat
        }
        return this.sat ?: true
    }

    private fun String.checkCorners(): Boolean {
        val smt = this.readSMT()
        for (point in cornerPoints) {
            val sat = smt.checkAt(point)
            if (sat) return true
        }
        return false
    }

    private fun SMT.checkAt(point: Map<String, Double>): Boolean {
        return when (this) {
            is SMT.Terminal -> data == "true"
            is SMT.Expression -> when (this.funName) {
                "not" -> !this.funArgs.first().checkAt(point)
                "and" -> this.funArgs.fold(true) { a, i -> a && i.checkAt(point) }
                "or" -> this.funArgs.fold(false) { a, i -> a || i.checkAt(point) }
                ">" -> this.funArgs[0].evalExpr(point) > this.funArgs[1].evalExpr(point)
                "<" -> this.funArgs[0].evalExpr(point) < this.funArgs[1].evalExpr(point)
                ">=" -> this.funArgs[0].evalExpr(point) >= this.funArgs[1].evalExpr(point)
                "<=" -> this.funArgs[0].evalExpr(point) <= this.funArgs[1].evalExpr(point)
                else -> throw IllegalArgumentException(this.toString())
            }
        }
    }

    private fun SMT.evalExpr(point: Map<String, Double>): Double {
        return when (this) {
            is SMT.Terminal -> point[data] ?: data.toDouble()
            is SMT.Expression -> when (this.funName) {
                "+" -> funArgs.map { it.evalExpr(point) }.sum()
                "-" -> funArgs.map { it.evalExpr(point) }.fold(0.0) { a, i -> a - i }
                "*" -> funArgs.map { it.evalExpr(point) }.fold(1.0) { a, i -> a * i }
                "/" -> funArgs[0].evalExpr(point) / funArgs[1].evalExpr(point)
                else -> throw IllegalArgumentException(this.toString())
            }
        }
    }

    override fun Z3Params.minimize(force: Boolean) {
        if (force || (!minimal && this.formula.length > 16 * coreSize)) {
            val isSat = this.sat ?: z3.checkSat(this.formula)
            this.sat = isSat
            if (!isSat) {
                this.formula = "false"
                this.minimal = true
                return
            }
            val simplified = z3.minimize(this.formula)
            this.formula = simplified
            sat = simplified != "false"
            if (simplified.length > coreSize) {
                coreSize = simplified.length
            }
            minimal = true
        }
    }

    override fun Z3Params.minimize() {
        this.minimize(false)
    }

    override fun Z3Params.and(other: Z3Params): Z3Params {
        return Z3Params("(and ${this.formula} ${other.formula})", null)
        /*
        WHAT THE SHIT?!
        val r = Z3Params("(and ${this.formula} ${other.formula})", null)
        println("R: ${r.formula.length}")
        c1 += 1
        return if (r equals this) {
            c2 += 1
            this.apply { minimize() }
        } else r.apply { minimize() }*/
    }

    override fun Z3Params.not(): Z3Params {
        //note: double negation is not very common, we don't have to care...
        return Z3Params("(not ${this.formula})", null).apply { minimize() }
    }

    override fun Z3Params.or(other: Z3Params): Z3Params {
        return Z3Params("(or ${this.formula} ${other.formula})", if (this.sat == true || other.sat == true) true else null).apply { minimize() }
        /*c1 += 1
        //happens quite often, but won't help us when minimizing
        if (z3.checkSat("(and (not (and ${this.formula} ${other.formula})) (or ${this.formula} ${other.formula}))")) {
            c2 += 1
            return Z3Params("(or ${this.formula} ${other.formula})", if (this.sat == true || other.sat == true) true else null).apply { minimize() }
        } else {
            return this
        }*/
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
        colors.bytes = null  //release bytes - chance we will be sending the same data again is very small
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
        return Z3Params(
                String(array),
                when (this.get().toInt()) {
                    1 -> true
                    0 -> false
                    else -> null
                }
        )
    }

    override fun Z3Params.transferTo(solver: Solver<Z3Params>): Z3Params {
        return Z3Params(this.formula, this.sat, this.minimal)
    }

    private fun Z3Params.ensureBytes(): ByteArray {
        val bytes = this.formula.toByteArray()
        this.bytes = bytes
        return bytes
    }

}

interface Z3SolverBase {

    val bounds: String

    infix fun String.and(other: String) = "(and $this $other)"
    infix fun String.or(other: String) = "(or $this $other)"

    infix fun String.gt(other: String) = "(> $this $other)"
    infix fun String.ge(other: String) = "(>= $this $other)"
    infix fun String.lt(other: String) = "(< $this $other)"
    infix fun String.le(other: String) = "(<= $this $other)"

    infix fun String.plus(other: String) = "(+ $this $other)"
    infix fun String.times(other: String) = "(* $this $other)"
    infix fun String.div(other: String) = "(/ $this $other)"

    fun Z3Params.minimize(force: Boolean)
}