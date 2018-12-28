package com.github.sybila.ode.model

/**
 * Hill function: https://en.wikipedia.org/wiki/Hill_equation_(biochemistry)
 *
 * Original Hill function has image <0,1)
 * Values a and b serve as scaling factor so that the image is stretched to <a, b).
 * In case the function is negative, a and b are switched, and then the
 * image of the function is <b, a) and whole function is decreasing.
 *
 */
data class Hill constructor(
        override val varIndex: Int,
        val theta: Double,
        val n: Double,
        val a: Double,
        val b: Double
) : Evaluable {

    constructor(varIndex: Int, theta: Double, n: Double, a: Double, b: Double, positive: Boolean):
            this(varIndex, theta, n,
                    if (positive) Math.min(a,b) else Math.max(a,b),
                    if (positive) Math.max(a,b) else Math.min(a,b)
            )

    override fun eval(value: Double): Double {
        return a + (b - a) * (1 / (1 + Math.pow(theta/value, n)))
    }

    override fun toString(): String
        = "Hill(${if(a <= b) '+' else '-'})($varIndex, $theta, $n, $a, $b)"

}