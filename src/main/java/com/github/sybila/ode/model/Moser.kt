package com.github.sybila.ode.model

/**
 * Moser function for bacterial growth looks like: k_max * ( S^n / (K_s + S^n ) )
 * where n > 0
 *
 * Original Moser function has image <0,1)
 */
data class Moser constructor(
        override val varIndex: Int,
        val theta: Double,
        val n: Double
) : Evaluable {

    override fun eval(value: Double): Double {
        return (1 / (1 + theta/Math.pow(value, n)))
    }

    override fun toString(): String
        = "Moser($varIndex, $theta, $n)"

}