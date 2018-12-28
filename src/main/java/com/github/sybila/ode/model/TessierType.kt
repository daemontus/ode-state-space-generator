package com.github.sybila.ode.model

/**
 * Tessier-type function for bacterial growth looks like: k_max * ( e^(-S/K_i) - e^(-S/K_s) )
 *
 */
data class TessierType constructor(
        override val varIndex: Int,
        val theta: Double,
        val kappa: Double
) : Evaluable {

    override fun eval(value: Double): Double {
        return (Math.exp(-1*value/kappa) - Math.exp(-1*value/theta))
    }

    override fun toString(): String
        = "Tessier_type($varIndex, $theta, $kappa)"

}