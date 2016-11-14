package com.github.sybila.ode.model

fun Model.writeBio(): String {
    return (if (this.parameters.isNotEmpty()) {
                "PARAMS: ${this.parameters.map {
                    "${it.name}, ${it.range.first}, ${it.range.second}"
                }.joinToString(separator = ";")}"
            } else "") +
            ("VARS: " + this.variables.map { it.name }.joinToString()) +
            (this.variables.map {
                "THRES: ${it.name}: ${it.thresholds.joinToString()}"
            }) +
            (this.variables.map {
                "EQ: ${it.name} = ${it.equation.map {
                    "${it.constant}" +
                    if (it.hasParam()) { " * " + this.parameters[it.paramIndex].name } else "" +
                    it.variableIndices.map { " * " + this.variables[it].name }.joinToString(separator = "") +
                    it.evaluable.map {
                        if (it is RampApproximation) {
                            " * Approx(${this.variables[it.varIndex].name})(${
                                it.thresholds.zip(it.values).map { "[${it.first}, ${it.second}]" }
                                .joinToString()
                            })"
                        } else {
                            throw IllegalArgumentException("Cannot serialize models with non-ramp functions.")
                        }
                    }
                }.joinToString(separator = " + ")}"
            })
}