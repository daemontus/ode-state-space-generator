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

    private val positiveVertexCache = HashMap<Int, List<Boolean?>>()
    private val negativeVertexCache = HashMap<Int, List<Boolean?>>()

    override fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): Boolean? {
        return (if (positive) positiveVertexCache else negativeVertexCache).computeIfAbsent(vertex) {
            val dim = dimension
            val p: List<Boolean?> = (0 until dimensions).map {
                var derivationValue = 0.0

                //evaluate equations
                for (summand in model.variables[dim].equation) {
                    var partialSum = summand.constant
                    for (v in summand.variableIndices) {
                        partialSum *= model.variables[v].thresholds[encoder.vertexCoordinate(vertex, v)]
                    }
                    if (partialSum != 0.0) {
                        for (function in summand.evaluable) {
                            val index = function.varIndex
                            partialSum *= function(model.variables[index].thresholds[encoder.vertexCoordinate(vertex, index)])
                        }
                    }
                    derivationValue += partialSum
                }

                if (derivationValue == 0.0) { null } else {
                    derivationValue > 0 == positive
                }
            }
            //save also dual values (null means derivation zero - when inverted, it's also zero
            (if (positive) negativeVertexCache else positiveVertexCache)[vertex] = p.map { it?.not() }
            p
        }[dimension]
    }

}