package com.github.sybila.ode.generator.det

import com.github.sybila.checker.Solver
import com.github.sybila.checker.solver.SolverStats
import com.github.sybila.ode.generator.IntervalSolver
import java.nio.ByteBuffer
import java.util.*

/**
 * Solver for working with [RectangleSet]
 */
class RectangleSetSolver(
        boundsX: Pair<Double, Double>,
        boundsY: Pair<Double, Double>
) : Solver<RectangleSet>, IntervalSolver<RectangleSet> {

    // empty parameter set has no thresholds and no rectangles
    override val ff: RectangleSet = RectangleSet(
            thresholdsX = DoubleArray(0),
            thresholdsY = DoubleArray(0),
            values = BitSet()
    )

    // full parameter set has the bound thresholds and exactly one rectangle
    override val tt: RectangleSet = RectangleSet(
            thresholdsX = doubleArrayOf(boundsX.first, boundsX.second),
            thresholdsY = doubleArrayOf(boundsY.first, boundsY.second),
            values = BitSet(1).apply { set(0) }
    )

    override fun RectangleSet.and(other: RectangleSet): RectangleSet {
        val cutLeft = this.cut(other.thresholdsX, other.thresholdsY)
        val cutRight = other.cut(this.thresholdsX, this.thresholdsY)
        val newValues: BitSet = cutLeft.values.clone() as BitSet
        newValues.and(cutRight.values)
        return RectangleSet(cutLeft.thresholdsX, cutLeft.thresholdsY, newValues).simplify()
    }

    override fun RectangleSet.not(): RectangleSet {
        val cutWithTrue = this.cut(tt.thresholdsX, tt.thresholdsY)
        val newValues = cutWithTrue.values.clone() as BitSet    // we have to clone, because cut might not copy
        newValues.flip(0, (cutWithTrue.thresholdsX.size - 1) * (cutWithTrue.thresholdsY.size - 1))
        return RectangleSet(cutWithTrue.thresholdsX, cutWithTrue.thresholdsY, newValues).simplify()
    }

    override fun RectangleSet.or(other: RectangleSet): RectangleSet {
        val cutLeft = this.cut(other.thresholdsX, other.thresholdsY)
        val cutRight = other.cut(this.thresholdsX, this.thresholdsY)
        val newValues: BitSet = cutLeft.values.clone() as BitSet
        newValues.or(cutRight.values)
        return RectangleSet(cutLeft.thresholdsX, cutLeft.thresholdsY, newValues).simplify()
    }

    override fun RectangleSet.isSat(): Boolean {
        SolverStats.solverCall()
        return !this.values.isEmpty
    }

    override fun RectangleSet.minimize() {
        // do nothing
    }

    override fun RectangleSet.byteSize(): Int {
        //3x size + size of array (all are 8 byte types)
        return 3 * 4 + (thresholdsX.size + thresholdsY.size + values.toLongArray().size) * 8
    }

    override fun ByteBuffer.getColors(): RectangleSet {
        val xThresholds = DoubleArray(int) { double }
        val yThresholds = DoubleArray(int) { double }
        val values = LongArray(int) { long }
        return RectangleSet(xThresholds, yThresholds, BitSet.valueOf(values))
    }

    override fun ByteBuffer.putColors(colors: RectangleSet): ByteBuffer {
        putInt(colors.thresholdsX.size)
        colors.thresholdsX.forEach { putDouble(it) }
        putInt(colors.thresholdsY.size)
        colors.thresholdsY.forEach { putDouble(it) }
        val values = colors.values.toLongArray()
        putInt(values.size)
        values.forEach { putLong(it) }
        return this
    }

    override fun RectangleSet.prettyPrint(): String {
        return toString()
    }

    override fun RectangleSet.transferTo(solver: Solver<RectangleSet>): RectangleSet {
        return this
    }

    override fun RectangleSet.asIntervals(): Array<Array<DoubleArray>> = this.makeIntervals()

}