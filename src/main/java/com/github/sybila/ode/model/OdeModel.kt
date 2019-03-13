package com.github.sybila.ode.model

data class OdeModel(
        val variables: List<Variable>,
        val parameters: List<Parameter> = listOf()
) {

    constructor(vararg variable: Variable) : this(variable.toList())

    init {
        if (variables.isEmpty()) throw IllegalArgumentException("OdeModel has no variables!")
    }

    data class Variable(
            val name: String,
            val range: Pair<Double, Double>,
            val thresholds: List<Double>,
            val varPoints: Pair<Int, Int>?,
            val equation: List<Summand>
    ) {
        constructor(name: String, range: Pair<Double, Double>, thresholds: List<Double>, varPoints: Pair<Int, Int>?,
                    vararg summands: Summand) : this(name, range, thresholds, varPoints, summands.toList())

        constructor(name: String, range: Pair<Double, Double>, thresholds: List<Double>, varPoints: Pair<Int, Int>?,
                    summand: Summand) : this(name, range, thresholds, varPoints, listOf(summand))
    }

    data class Parameter(
            val name: String,
            val range: Pair<Double, Double>
    )

    fun dimensionFromName(name: String): Int {
        for (v in variables.indices) {
            if (variables[v].name == name) return v
        }
        throw IllegalArgumentException("Unknown variable: $name")
    }

}