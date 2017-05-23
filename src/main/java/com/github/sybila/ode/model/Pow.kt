package com.github.sybila.ode.model

/**
 * Power function: https://en.wikipedia.org/wiki/Power_function
 *
 * Takes the degree of resulting polynomial function.
 *
 */
data class Pow constructor(
        override val varIndex: Int,
        val degree: Double
) : Evaluable {

    override fun eval(value: Double): Double {
        return Math.pow(value,degree)
    }

    override fun toString(): String
        = "Pow($varIndex,$degree)"

}