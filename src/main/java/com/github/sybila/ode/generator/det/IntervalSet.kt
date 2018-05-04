package com.github.sybila.ode.generator.det

import java.util.*
import java.util.stream.Collectors

/**
 * Adaptive 1D parameter grid.
 */
class IntervalSet(
        var thresholds: DoubleArray,
        var values: BitSet
) {

    // Split this interval set along additional cut points
    // Return a new interval set instance with thresholds and
    // interval values adjusted accordingly.
    fun cut(cut: DoubleArray): IntervalSet {
        val new = (thresholds + cut).toSet().sorted().toDoubleArray()
        return if (Arrays.equals(new, thresholds)) this else {
            val newValues = BitSet(values.size())
            values.stream().forEach { x ->
                val newLow = new.binarySearch(thresholds[x])
                val newHigh = new.binarySearch(thresholds[x+1])
                newValues.set(newLow, newHigh)
            }
            IntervalSet(new, newValues)
        }
    }

    fun simplify(): IntervalSet {
        if (values.isEmpty) return this

        val newThresholds = arrayListOf<Double>()
        val newValues = BitSet()

        values.stream().forEach { i ->
            //add low threshold if previous block is not set
            if (i == 0 || !values[i-1]) {
                newValues.set(newThresholds.size)   // next valid region starts on a newly added threshold.
                newThresholds.add(thresholds[i])
            }
            //add high threshold if next block is not set
            if (i == values.size()-1 || !values[i+1]) {
                newThresholds.add(thresholds[i+1])
            }
        }

        // Note that by construction, newThresholds are sorted.

        return IntervalSet(newThresholds.toDoubleArray(), newValues)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntervalSet

        if (!Arrays.equals(thresholds, other.thresholds)) return false
        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(thresholds)
        result = 31 * result + values.hashCode()
        return result
    }

    override fun toString(): String {
        val builder = StringBuilder()
        values.stream().forEach { x ->
            builder.append("[${thresholds[x]}, ${thresholds[x+1]}]")
            builder.append(",")
        }
        return builder.toString()
    }

    fun makeIntervals(): Array<Array<DoubleArray>> {
        return values.stream().mapToObj { x ->
            arrayOf(doubleArrayOf(thresholds[x], thresholds[x+1]))
        }.collect(Collectors.toList()).toTypedArray()
    }


}