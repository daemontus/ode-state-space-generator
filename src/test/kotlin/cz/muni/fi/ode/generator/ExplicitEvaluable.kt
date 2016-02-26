package cz.muni.fi.ode.generator

import cz.muni.fi.ode.model.Evaluable

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