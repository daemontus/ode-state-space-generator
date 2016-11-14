package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.Colors
import java.util.*

var solverCalls = 0
var timeInSolver = 0L
val solverCache = HashMap<CNF, Boolean>()
var solverCacheHit = 0


class SMTColors(
        val cnf: CNF,
        private val order: PartialOrderSet,
        private var sat: Boolean? = null
) : Colors<SMTColors> {

    override fun isEmpty(): Boolean {
        return (sat ?: solverCache[cnf].apply { solverCacheHit += 1 } ?: run {
            solve()
            sat ?: true
        }).not() //un-sat ~ empty
    }

    fun solve() {
        if (sat == null) {
            val start = System.nanoTime()
            sat = order.solver.check(cnf.asFormula()).isSat()
            timeInSolver += System.nanoTime() - start
            solverCalls += 1
            solverCache[cnf] = sat ?: true
        }
    }

    override fun minus(other: SMTColors): SMTColors {
        val new = cnf.and(other.cnf.not()).simplify()
        return SMTColors(new, order, sat weakMinus other.sat)
    }

    override fun plus(other: SMTColors): SMTColors {
        val new = cnf.or(other.cnf).simplify()
        return SMTColors(new, order, sat weakOr other.sat)
    }

    override fun intersect(other: SMTColors): SMTColors {
        val new = cnf.and(other.cnf).simplify()
        return SMTColors(new, order, sat weakAnd other.sat)
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
        return "SMTColors(sat=$sat, cnf=$cnf)"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SMTColors) return false
        return cnf == other.cnf
    }

    override fun hashCode(): Int {
        return cnf.hashCode()
    }

    fun calculateBufferSize(): Int {
        return 1 + cnf.calculateBufferSize()
    }

    fun serialize(buffer: LongArray) {
        buffer[0] = when (sat) {
            true -> 1
            false -> -1
            null -> 0
        }
        cnf.serialize(buffer, 1)
    }

}


fun LongArray.readSMTColors(order: PartialOrderSet): SMTColors {
    val sat = when(this[0]) {
        0L -> null
        1L -> true
        -1L -> false
        else -> throw IllegalStateException("Unknown value: ${this[0]}")
    }
    return SMTColors(this.readCNF(1, order), order, sat)
}