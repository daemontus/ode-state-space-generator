package com.github.sybila.ode.model

import com.github.sybila.ode.safeString

fun OdeModel.toBio(): String {
    return (if (this.parameters.isNotEmpty()) {
                "PARAMS: ${this.parameters.joinToString(separator = ";") {
                    "${it.name}, ${it.range.first.safeString()}, ${it.range.second.safeString()}"
                }}"
            } else "") + "\n" +
            ("VARS: " + this.variables.joinToString { it.name }) + "\n" +
            (this.variables.joinToString(separator = "\n") {
                "THRES: ${it.name}: ${it.thresholds.joinToString(transform = Double::safeString)}"
            }) + "\n" +
            (this.variables.joinToString(separator = "\n") {
                "EQ: ${it.name} = ${it.equation.joinToString(separator = " + ") {
                    it.constant.safeString() +
                            (if (it.hasParam()) { " * " + this.parameters[it.paramIndex].name } else "") +
                            it.variableIndices.joinToString(separator = "") { " * " + this.variables[it].name } +
                            it.evaluable.joinToString(separator = "") {
                                when (it) {
                                    is RampApproximation -> {
                                        " * Approx(${this.variables[it.varIndex].name})(${
                                        it.thresholds.zip(it.values).joinToString {
                                            "[${it.first.safeString()}, ${it.second.safeString()}]"
                                        }
                                        })"
                                    }
                                    is Step -> {
                                        " * H${if (it.a < it.b) "p" else "m"}(${it.theta}, ${it.a}, ${it.b})"
                                    }
                                    else -> throw IllegalArgumentException("Cannot serialize models with non-ramp functions.")
                                }
                            }
                }}"
            })
}