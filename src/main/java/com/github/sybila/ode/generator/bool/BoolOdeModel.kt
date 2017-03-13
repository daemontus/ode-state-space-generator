package com.github.sybila.ode.generator.bool

import com.github.sybila.checker.solver.BoolSolver
import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.model.OdeModel
import java.util.*

class BoolOdeModel(
        model: OdeModel,
        createSelfLoops: Boolean = true
) : AbstractOdeFragment<Boolean>(model, createSelfLoops, BoolSolver()) {

    init {
        if (model.parameters.isNotEmpty()) throw IllegalArgumentException("Can't use bool model for model with parameters")
    }

    private val equations = model.variables.map { it.equation }.toTypedArray()
    private val thresholds = model.variables.map { it.thresholds }.toTypedArray()

    override fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): Boolean? {
        val dim = dimension

        var derivationValue = 0.0

        //evaluate equations
        for (summand in equations[dim]) {
            var partialSum = summand.constant
            for (v in summand.variableIndices) {
                partialSum *= thresholds[v][encoder.vertexCoordinate(vertex, v)]
            }
            if (partialSum != 0.0) {
                for (function in summand.evaluable) {
                    val index = function.varIndex
                    partialSum *= function(thresholds[index][encoder.vertexCoordinate(vertex, index)])
                }
            }
            derivationValue += partialSum
        }

        return if (derivationValue == 0.0) { null } else {
            derivationValue > 0 == positive
        }
    }

}