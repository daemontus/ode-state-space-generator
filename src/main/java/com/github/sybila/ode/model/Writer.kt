package com.github.sybila.ode.model

import java.util.*

fun OdeModel.toBio(): String {
    return (if (this.parameters.isNotEmpty()) {
                "PARAMS: ${this.parameters.map {
                    "${it.name}, ${it.range.first.safeString()}, ${it.range.second.safeString()}"
                }.joinToString(separator = ";")}"
            } else "") + "\n" +
            ("VARS: " + this.variables.map { it.name }.joinToString()) + "\n" +
            (this.variables.map {
                "THRES: ${it.name}: ${it.thresholds.map(Double::safeString).joinToString()}"
            }.joinToString(separator = "\n")) + "\n" +
            (this.variables.map {
                "EQ: ${it.name} = ${it.equation.map {
                    it.constant.safeString() +
                    (if (it.hasParam()) { " * " + this.parameters[it.paramIndex].name } else "") +
                    it.variableIndices.map { " * " + this.variables[it].name }.joinToString(separator = "") +
                    it.evaluable.map {
                        if (it is RampApproximation) {
                            " * Approx(${this.variables[it.varIndex].name})(${
                                it.thresholds.zip(it.values).map {
                                    "[${it.first.safeString()}, ${it.second.safeString()}]"
                                }.joinToString()
                            })"
                        } else {
                            throw IllegalArgumentException("Cannot serialize models with non-ramp functions.")
                        }
                    }.joinToString(separator = "")
                }.joinToString(separator = " + ")}"
            }.joinToString(separator = "\n"))
}

fun Double.safeString(): String {
    return String.format(Locale.ROOT, "%f", this)
}