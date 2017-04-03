package com.github.sybila.ode.generator.det

import com.github.sybila.checker.Solver
import java.nio.ByteBuffer
import java.util.*

class Encoder(
        val thresholds: Array<DoubleArray>
) {


    private val dimensions = thresholds.size
    private val sizes = IntArray(dimensions) { thresholds[it].size - 1 }
    private val multipliers = IntArray(dimensions)

    init {
        var rectangleCount = 1L
        for (t in thresholds.indices) {
            multipliers[t] = rectangleCount.toInt()
            rectangleCount *= sizes[t]
        }
        if (rectangleCount > Int.MAX_VALUE) {
            throw IllegalStateException("Parameter set too big! ${rectangleCount}")
        }
    }

    fun lowThreshold(of: Int, dim: Int): Int {
        return (of / multipliers[dim]) % sizes[dim]
    }

}

class RectangleSet(
        val thresholdsX: DoubleArray,
        val thresholdsY: DoubleArray,
        val values: BitSet
) {

    val modifier = thresholdsX.size - 1

    // Split this rectangle set along additional cut points
    // Return a new rectangle set instance with thresholds and
    // rectangle values adjusted accordingly.
    fun cut(cutX: DoubleArray, cutY: DoubleArray): RectangleSet {
        val newX = (thresholdsX + cutX).toSet().sorted().toDoubleArray()
        val newY = (thresholdsY + cutY).toSet().sorted().toDoubleArray()
        val newModifier = newX.size - 1
        if (Arrays.equals(thresholdsX, newX) && Arrays.equals(thresholdsY, newY)) {
            return this
        } else {
            val newValues = BitSet(values.size())
            values.stream().forEach { value ->
                val X = value % modifier
                val Y = value / modifier
                val newXlow = newX.binarySearch(thresholdsX[X])
                val newYlow = newY.binarySearch(thresholdsY[Y])
                val newXhigh = newX.binarySearch(thresholdsX[X + 1])
                val newYhigh = newY.binarySearch(thresholdsY[Y + 1])
                if (newXlow + 1 == newXhigh && newYlow + 1 == newYhigh) {
                    // the rectangle just moved, it did not split
                    newValues.set(newYlow * newModifier + newXlow)
                } else {
                    // the rectangle is split into several smaller ones
                    for (allY in newYlow..newYhigh-1) {
                        // note: set is not inclusive on the last index, hence no -1
                        newValues.set(allY * newModifier + newXlow, allY * modifier + newXhigh)
                    }
                }
            }
            return RectangleSet(newX, newY, newValues)
        }
    }


}

class RectangleSetSolver(
        bounds: List<Pair<Double, Double>>
) : Solver<RectangleSet> {

    val parameterCount = bounds.size

    // empty parameter set has no thresholds and no rectangles
    override val ff: RectangleSet = RectangleSet(
            thresholds = Array(parameterCount) { DoubleArray(0) },
            values = BitSet()
    )

    // full parameter set has the bound thresholds and exactly one rectangle
    override val tt: RectangleSet = RectangleSet(
            thresholds = Array(parameterCount) { dim -> doubleArrayOf(bounds[dim].first, bounds[dim].second) },
            values = BitSet(1).apply { set(0) }
    )

    override fun RectangleSet.and(other: RectangleSet): RectangleSet {

    }

    override fun RectangleSet.byteSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun ByteBuffer.getColors(): RectangleSet {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun RectangleSet.isSat(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun RectangleSet.minimize() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun RectangleSet.not(): RectangleSet {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun RectangleSet.or(other: RectangleSet): RectangleSet {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun RectangleSet.prettyPrint(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun ByteBuffer.putColors(colors: RectangleSet): ByteBuffer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun RectangleSet.transferTo(solver: Solver<RectangleSet>): RectangleSet {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}