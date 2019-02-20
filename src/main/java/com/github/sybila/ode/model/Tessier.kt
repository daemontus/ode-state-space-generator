package com.github.sybila.ode.model

/**
 * Tessier function for bacterial growth looks like: k_max * ( 1 - e^(-S/K_s) )
 *
 */
data class Tessier constructor(
        override val varIndex: Int,
        val theta: Double
) : Evaluable {

    override fun eval(value: Double): Double {
        return (1 - Math.exp(-1*value/theta))
    }

    override fun toString(): String
        = "Tessier($varIndex, $theta)"

}