package com.github.sybila.ode.generator.det

import com.github.sybila.checker.Solver
import com.github.sybila.checker.solver.SolverStats
import com.github.sybila.ode.generator.IntervalSolver
import java.nio.ByteBuffer
import java.util.*

/**
 * Solver for working with [IntervalSet]
 */
class IntervalSetSolver(
        bounds: Pair<Double, Double>
) : Solver<IntervalSet>, IntervalSolver<IntervalSet> {

    // empty parameter set has no thresholds and no rectangles
    override val ff: IntervalSet = IntervalSet(thresholds = DoubleArray(0), values = BitSet())

    // full parameter set has the bound thresholds and exactly one rectangle
    override val tt: IntervalSet = IntervalSet(
            thresholds = doubleArrayOf(bounds.first, bounds.second),
            values = BitSet(1).apply { set(0) }
    )

    override fun IntervalSet.and(other: IntervalSet): IntervalSet {
        val cutThis = this.cut(other.thresholds)
        val cutThat = other.cut(this.thresholds)
        val newValues: BitSet = cutThis.values.clone() as BitSet
        newValues.and(cutThat.values)
        return IntervalSet(cutThis.thresholds, newValues).simplify()
    }

    override fun IntervalSet.not(): IntervalSet {
        val cutWithTrue = this.cut(tt.thresholds)
        val newValues = cutWithTrue.values.clone() as BitSet    // we have to clone, because cut might not copy
        newValues.flip(0, cutWithTrue.thresholds.size - 1)
        return IntervalSet(cutWithTrue.thresholds, newValues).simplify()
    }

    override fun IntervalSet.or(other: IntervalSet): IntervalSet {
        val cutThis = this.cut(other.thresholds)
        val cutThat = other.cut(this.thresholds)
        val newValues: BitSet = cutThis.values.clone() as BitSet
        newValues.or(cutThat.values)
        return IntervalSet(cutThis.thresholds, newValues).simplify()
    }

    override fun IntervalSet.isSat(): Boolean {
        SolverStats.solverCall()
        return !this.values.isEmpty
    }

    override fun IntervalSet.minimize() {
        // do nothing
    }

    override fun IntervalSet.byteSize(): Int {
        //3x size + size of array (all are 8 byte types)
        return 2 * 4 + (thresholds.size + values.toLongArray().size) * 8
    }

    override fun ByteBuffer.getColors(): IntervalSet {
        val thresholds = DoubleArray(int) { double }
        val values = LongArray(int) { long }
        return IntervalSet(thresholds, BitSet.valueOf(values))
    }

    override fun ByteBuffer.putColors(colors: IntervalSet): ByteBuffer {
        putInt(colors.thresholds.size)
        colors.thresholds.forEach { putDouble(it) }
        val values = colors.values.toLongArray()
        putInt(values.size)
        values.forEach { putLong(it) }
        return this
    }

    override fun IntervalSet.prettyPrint(): String {
        return toString()
    }

    override fun IntervalSet.transferTo(solver: Solver<IntervalSet>): IntervalSet {
        return this
    }

    override fun IntervalSet.asIntervals(): Array<Array<DoubleArray>> = this.makeIntervals()
}