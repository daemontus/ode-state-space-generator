package com.github.sybila.ode.model

/**
 * Sine function: https://en.wikipedia.org/wiki/Sine
 *
 */
data class Sine private constructor(
        override val varIndex: Int
) : Evaluable {

    override fun eval(value: Double): Double {
        return Math.sin(value)
    }

    override fun toString(): String
        = "Sin($varIndex)"
}
