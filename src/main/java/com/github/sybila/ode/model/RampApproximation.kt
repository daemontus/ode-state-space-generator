package com.github.sybila.ode.model

import java.util.*

/**
 * Approximation of continuous function using linear ramps.
 */
data class RampApproximation(
        override val varIndex: Int,
        val thresholds: DoubleArray,   //thresholds between ramps - SORTED!
        val values: DoubleArray        //value of function in corresponding threshold
) : Evaluable {

    override fun eval(value: Double): Double {
        if (value <= thresholds.first()) return values.first()
        if (value >= thresholds.last()) return values.last()
        val position = Arrays.binarySearch(thresholds, value)
        if (position >= 0) {    //evaluated value is one of the thresholds
            return values[position]
        } else {                //position points to -1 * (upper threshold)
            val iH = -position-1  //note that this must be a valid index, otherwise some conditions above would fire
            val iL = iH-1
            return values[iL] + (value - thresholds[iL]) / (thresholds[iH] - thresholds[iL]) * (values[iH] - values[iL])
        }
    }

    override fun toString(): String
        = "Approx($varIndex)[${thresholds.joinToString()}]{${values.joinToString()}}"

    override fun equals(other: Any?): Boolean {
        return other is RampApproximation &&
                this.varIndex == other.varIndex &&
                Arrays.equals(this.thresholds, other.thresholds) &&
                Arrays.equals(this.values, other.values)
    }

    override fun hashCode(): Int {
        return varIndex + 31 * Arrays.hashCode(thresholds) + 47 * Arrays.hashCode(values)
    }
}