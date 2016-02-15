package cz.muni.fi.ode.model

data class Ramp(
        val varIndex: Int,
        val lowThreshold: Double,
        val highThreshold: Double,
        val a: Double,
        val b: Double
) : Evaluable {

    constructor(varIndex: Int, lowThreshold: Double, highThreshold: Double, a: Double, b: Double, positive: Boolean):
    this(varIndex, lowThreshold, highThreshold,
            if (positive) Math.min(a,b) else Math.max(a,b),
            if (positive) Math.max(a,b) else Math.min(a,b)
    )

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