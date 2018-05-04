package com.github.sybila.ode.generator.det

import java.util.*
import java.util.stream.Collectors

/**
 * Adaptive 2D grid representation of parameter space.
 */
class RectangleSet(
        var thresholdsX: DoubleArray,
        var thresholdsY: DoubleArray,
        var values: BitSet
) {

    private val modifier = thresholdsX.size - 1

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
                val newXLow = newX.binarySearch(thresholdsX[X])
                val newYLow = newY.binarySearch(thresholdsY[Y])
                val newXHigh = newX.binarySearch(thresholdsX[X + 1])
                val newYHigh = newY.binarySearch(thresholdsY[Y + 1])
                if (newXLow + 1 == newXHigh && newYLow + 1 == newYHigh) {
                    // the rectangle just moved, it did not split
                    newValues.set(newYLow * newModifier + newXLow)
                } else {
                    // the rectangle is split into several smaller ones
                    for (allY in newYLow..newYHigh-1) {
                        // note: set is not inclusive on the last index, hence no -1
                        newValues.set(allY * newModifier + newXLow, allY * newModifier + newXHigh)
                    }
                }
            }
            return RectangleSet(newX, newY, newValues)
        }
    }

    fun simplify(): RectangleSet {
        if (values.isEmpty) return RectangleSet(DoubleArray(0), DoubleArray(0), BitSet())

        val ySections = thresholdsY.size - 1
        val xSections = thresholdsX.size - 1

        // prune dimension X
        val newX = ArrayList<Double>()
        // first threshold is valid if any rectangle is not empty
        if ((0 until ySections).any { y ->
            values[y * modifier]
        }) newX.add(thresholdsX[0])
        // other thresholds are valid if there is any difference between left and right states
        for (x in 1..(thresholdsX.size - 2)) {
            // Note that thresholdsX.size - 2 is index of last section
            if ((0 until ySections).any { y ->
                values[y * modifier + x - 1] != values[y * modifier + x]
            }) newX.add(thresholdsX[x])
        }
        // last threshold is valid if there is any non empty rectangle in last column
        if ((0 until ySections).any { y ->
            values[y * modifier + xSections - 1]
        }) newX.add(thresholdsX.last())

        val newY = ArrayList<Double>()
        // first threshold is valid if there is anything in the first row
        if ((0 until xSections).any { x ->
            values[x]
        }) newY.add(thresholdsY[0])
        // rest is valid if there is a difference
        for (y in 1..(thresholdsY.size - 2)) {
            if ((0 until xSections).any { x ->
                values[(y - 1) * modifier + x] != values[y * modifier + x]
            }) newY.add(thresholdsY[y])
        }
        // last threshold is valid if there is anything in the last row
        if ((0 until xSections).any { x ->
            values[(ySections - 1) * modifier + x]
        }) newY.add(thresholdsY.last())

        val newThresholdsX = newX.toDoubleArray()
        val newThresholdsY = newY.toDoubleArray()

        if (Arrays.equals(newThresholdsX, thresholdsX) && Arrays.equals(newThresholdsY, thresholdsY)) {
            return this
        } else {
            val newValues = BitSet()
            val newModifier = newThresholdsX.size - 1
            values.stream().forEach { value ->
                val X = value % modifier
                val Y = value / modifier
                val newXLow = newThresholdsX.binarySearch(thresholdsX[X])
                val newYLow = newThresholdsY.binarySearch(thresholdsY[Y])
                // Note: If reduction is correct, then the lowest rectangle of the merged area must
                // be present and matched. Everything else can be safely ignored.
                if (newXLow >= 0 && newYLow >= 0) {
                    newValues.set(newYLow * newModifier + newXLow)
                }
            }
            return RectangleSet(newThresholdsX, newThresholdsY, newValues)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as RectangleSet

        if (!Arrays.equals(thresholdsX, other.thresholdsX)) return false
        if (!Arrays.equals(thresholdsY, other.thresholdsY)) return false
        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(thresholdsX)
        result = 31 * result + Arrays.hashCode(thresholdsY)
        result = 31 * result + values.hashCode()
        return result
    }

    override fun toString(): String {
        val builder = StringBuilder()
        values.stream().forEach { value ->
            val X = value % modifier
            val Y = value / modifier
            builder.append("[[${thresholdsX[X]}, ${thresholdsX[X+1]}], [${thresholdsY[Y]}, ${thresholdsY[Y+1]}]]")
            builder.append(",")
        }
        return builder.toString()
    }

    fun makeIntervals(): Array<Array<DoubleArray>> {
        return values.stream().mapToObj { value ->
            val X = value % modifier
            val Y = value / modifier
            arrayOf(doubleArrayOf(thresholdsX[X], thresholdsX[X+1]), doubleArrayOf(thresholdsY[Y], thresholdsY[Y+1]))
        }.collect(Collectors.toList()).toTypedArray()
    }
}

