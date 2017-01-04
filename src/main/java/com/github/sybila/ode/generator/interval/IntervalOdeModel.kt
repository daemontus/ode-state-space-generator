package com.github.sybila.ode.generator.interval

import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.model.OdeModel
import java.util.*

class IntervalOdeModel(
        model: OdeModel,
        createSelfLoops: Boolean = true
) : AbstractOdeFragment<DoubleArray>(model, createSelfLoops, model.parameters.first().range.run {
    IntervalSolver(first, second)
}) {

    init {
        if (model.parameters.size > 1)
            throw IllegalStateException("Can't use interval solver. Model has ${model.parameters.size} parameters.")
    }

    private val positiveVertexCache = HashMap<Int, List<DoubleArray?>>()
    private val negativeVertexCache = HashMap<Int, List<DoubleArray?>>()

    override fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): DoubleArray? {
        return (if (positive) positiveVertexCache else negativeVertexCache).computeIfAbsent(vertex) {
            val p: List<DoubleArray?> = (0 until dimensions).map { dim ->
                var derivationValue = 0.0
                var denominator = 0.0

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
                    if (summand.hasParam()) {
                        denominator += partialSum
                    } else {
                        derivationValue += partialSum
                    }
                }

                val bounds: DoubleArray? = if (denominator == 0.0) {
                    //there is no parameter in this equation
                    if (derivationValue > 0 == positive) tt else ff
                } else {
                    //if you divide by negative number, you have to flip the condition
                    val newPositive = if (denominator > 0) positive else !positive
                    val split = -derivationValue / denominator
                    if (newPositive) {
                        if (split <= tt[0]) tt
                        else if (split >= tt[1]) ff
                        else tt.clone().apply { this[0] = split }
                    } else {
                        if (split <= tt[0]) ff
                        else if (split >= tt[1]) tt
                        else tt.clone().apply { this[1] = split }
                    }
                }
                bounds
            }
            //save also dual values
            (if (positive) negativeVertexCache else positiveVertexCache)[vertex] = p.map { it?.not() ?: tt }
            p
        }[dimension]
    }

}