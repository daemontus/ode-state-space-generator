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
        val yield: Double
) : Evaluable {

    constructor(varIndex: Int, theta: Double, yield: Double):
            this(varIndex, theta, yield)

    override fun eval(value: Double): Double {
        return (value / (yield * (theta + value)))
    }

    override fun toString(): String
        = "Monod($varIndex, $theta, $yield)"

}
