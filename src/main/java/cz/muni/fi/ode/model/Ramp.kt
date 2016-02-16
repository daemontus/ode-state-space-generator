package cz.muni.fi.ode.model

data class Ramp private constructor(
        val varIndex: Int,
        val lowThreshold: Double,
        val highThreshold: Double,
        val a: Double,
        val b: Double
) : Evaluable {

    companion object {

        fun positive(varIndex: Int, lowThreshold: Double, highThreshold: Double, a: Double, b: Double)
            = Ramp(varIndex, lowThreshold, highThreshold, Math.min(a,b), Math.max(a,b))

        fun negative(varIndex: Int, lowThreshold: Double, highThreshold: Double, a: Double, b: Double)
                = Ramp(varIndex, lowThreshold, highThreshold, Math.max(a,b), Math.min(a,b))

        fun positiveCoordinate(varIndex: Int, lowThreshold: Double, highThreshold: Double, a: Double, b: Double): Ramp {
            val a1 = lowThreshold * a + b
            val b1 = highThreshold * a + b
            return Ramp(varIndex, lowThreshold, highThreshold, Math.min(a1,b1), Math.max(a1,b1))
        }

        fun negativeCoordinate(varIndex: Int, lowThreshold: Double, highThreshold: Double, a: Double, b: Double): Ramp {
            val a1 = lowThreshold * a + b
            val b1 = highThreshold * a + b
            return Ramp(varIndex, lowThreshold, highThreshold, Math.max(a1,b1), Math.min(a1,b1))
        }

    }

    init {
        if (lowThreshold >= highThreshold) {
            throw IllegalArgumentException("Invalid Ramp function - wrong thresholds: $lowThreshold >= $highThreshold")
        }
    }

    override fun eval(value: Double): Double
        = when {
            value <= lowThreshold -> a
            value >= highThreshold -> b
            else -> {
                a + (value - lowThreshold) / (highThreshold - lowThreshold) * (b - a)
            }
        }

    override fun toString(): String
            = "R(${if (a <= b) '+' else '-'})($varIndex, $lowThreshold, $highThreshold, $a, $b)"

}