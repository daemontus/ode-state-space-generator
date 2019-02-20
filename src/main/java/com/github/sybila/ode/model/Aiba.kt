package com.github.sybila.ode.model

/**
 * Aiba-Edward function for bacterial growth looks like: k_max * ( e^(-S/K_i) / (1 + (K_s / S)) )
 *
 */
data class Aiba constructor(
        override val varIndex: Int,
        val theta: Double,
        val kappa: Double
) : Evaluable {

    override fun eval(value: Double): Double {
        return (Math.exp(-1*value/kappa) / (1 + (theta/value)))
    }

    override fun toString(): String
        = "Aiba($varIndex, $theta, $kappa)"

}