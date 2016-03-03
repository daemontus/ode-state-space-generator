package com.github.sybila.ode.generator

import com.github.sybila.ode.model.Evaluable

/**
 * Utility class that allows us to define any arbitrary equation by simply listing values
 */
class ExplicitEvaluable(
        override val varIndex: Int,
        private val values: Map<Double, Double> = mapOf()
) : Evaluable {
    override fun eval(value: Double)
            = values[value] ?: throw IllegalArgumentException("Function not defined for $value")
}