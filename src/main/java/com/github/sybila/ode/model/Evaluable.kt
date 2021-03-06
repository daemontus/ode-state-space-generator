package com.github.sybila.ode.model

/**
 * Class implementing this interface can be evaluated in certain point
 */
interface Evaluable : (Double) -> Double {

    val varIndex: Int

    fun eval(value: Double): Double
    override fun invoke(p1: Double): Double = eval(p1)
}