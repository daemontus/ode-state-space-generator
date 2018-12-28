package com.github.sybila.ode.model

/**
 * Andrews function for bacterial growth looks like: k_max / (( 1 + K_s/S) * (1 + S/K_i))
 *
 */
data class Andrews constructor(
        override val varIndex: Int,
        val theta: Double,
        val kappa: Double
) : Evaluable {

    override fun eval(value: Double): Double {
        return (1 / ((1 + (theta / value)) * (1 + (value / kappa))))
    }

    override fun toString(): String
        = "Andrews($varIndex, $theta, $kappa)"

}
