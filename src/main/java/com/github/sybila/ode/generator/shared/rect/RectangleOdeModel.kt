package com.github.sybila.ode.generator.shared.rect

import com.github.sybila.checker.shared.FF
import com.github.sybila.checker.shared.Params
import com.github.sybila.checker.shared.TT
import com.github.sybila.ode.generator.shared.AbstractOdeFragment
import com.github.sybila.ode.model.OdeModel
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class RectangleOdeModel(
        model: OdeModel,
        createSelfLoops: Boolean = true
) : AbstractOdeFragment(model, createSelfLoops, RectangleSolver(Rectangle(
        model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray()
))) {

    private val boundsRect = model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray()

    private val positiveVertexCache = ConcurrentHashMap<Int, List<Params?>>()
    private val negativeVertexCache = ConcurrentHashMap<Int, List<Params?>>()

    override fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): Params? {
        val primaryStorage = if (positive) positiveVertexCache else negativeVertexCache
        val secondaryStorage = if (!positive) positiveVertexCache else negativeVertexCache
        val current = primaryStorage[vertex]
        return if (current == null) {
            val p: List<Params?> = (0 until dimensions).map { dim ->
                var derivationValue = 0.0
                var denominator = 0.0
                var parameterIndex = -1

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
                        parameterIndex = summand.paramIndex
                        denominator += partialSum
                    } else {
                        derivationValue += partialSum
                    }
                }

                val bounds: Params? = if (parameterIndex == -1 || denominator == 0.0) {
                    //there is no parameter in this equation
                    if (derivationValue > 0 == positive) TT else FF
                } else {
                    //if you divide by negative number, you have to flip the condition
                    val newPositive = if (denominator > 0) positive else !positive
                    val range = model.parameters[parameterIndex].range
                    //min <= split <= max
                    val split = Math.min(range.second, Math.max(range.first, -derivationValue / denominator))
                    val newLow = if (newPositive) split else range.first
                    val newHigh = if (newPositive) range.second else split

                    if (newLow >= newHigh) null else {
                        val r = boundsRect.clone()
                        r[2*parameterIndex] = newLow
                        r[2*parameterIndex+1] = newHigh
                        listOf(Rectangle(r)).asParams()
                    }
                }
                bounds
            }
            //save also dual values
            secondaryStorage[vertex] = p.map { it?.not() ?: TT }
            primaryStorage[vertex] = p
            p[dimension]
        } else current[dimension]
    }

}