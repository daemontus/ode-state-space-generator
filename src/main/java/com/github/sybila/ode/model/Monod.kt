package com.github.sybila.ode.model

/**
 * Monod equation: https://en.wikipedia.org/wiki/Monod_equation
 *
 * Original Monod function has image <0,1)
 * It has two purposes. The first one, with yield equals 1, defines a rate of bacterial population growth.
 * The second one, with yield differs from 1, defines a rate of utilisation of substrate by bacteria - usually, it is scaled differently.
 */
data class Monod private constructor(
        override val varIndex: Int,
        val theta: Double,
        val y: Double
) : Evaluable {

    constructor(varIndex: Int, theta: Double, y: Double):
            this(varIndex, theta, y)

    override fun eval(value: Double): Double {
        return (value / (y * (theta + value)))
    }

    override fun toString(): String
        = "Monod($varIndex, $theta, $y)"

}
