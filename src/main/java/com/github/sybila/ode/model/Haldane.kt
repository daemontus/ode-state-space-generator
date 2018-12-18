package com.github.sybila.ode.model

/**
 * Haldane-Andrews function for bacterial growth looks like: k_max * ( S / (S + K_s + (S^2 / K_i)) )
 *
 */
data class Haldane private constructor(
        override val varIndex: Int,
        val theta: Double,
        val kappa: Double
) : Evaluable {

    constructor(varIndex: Int, theta: Double, kappa: Double):
            this(varIndex, theta, kappa)

    override fun eval(value: Double): Double {
        return (value / (theta + value + (Math.pow(value,2) / kappa) ))
    }

    override fun toString(): String
        = "Haldane($varIndex, $theta, $kappa)"

}
