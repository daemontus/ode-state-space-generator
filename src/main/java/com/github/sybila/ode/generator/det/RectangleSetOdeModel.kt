package com.github.sybila.ode.generator.det

import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.model.OdeModel
import java.util.*

/**
 * RectangleSetOdeModel uses a 2D adaptive grid to maintain the set of parameter values.
 * It is faster than general rectangle model, because it does not have explicit
 * rectangle objects, but it is applicable only to 2-parameter models.
 */
class RectangleSetOdeModel(
        model: OdeModel,
        createSelfLoops: Boolean = true
) : AbstractOdeFragment<RectangleSet>(model, createSelfLoops, RectangleSetSolver(
        model.parameters[0].range.let { it.first to it.second },
        model.parameters[1].range.let { it.first to it.second }
)) {

    private val boundsRect = model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray()

    private val positiveVertexCache = HashMap<Int, List<RectangleSet?>>()
    private val negativeVertexCache = HashMap<Int, List<RectangleSet?>>()

    override fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): RectangleSet? {
        return (if (positive) positiveVertexCache else negativeVertexCache).computeIfAbsent(vertex) {
            val p: List<RectangleSet?> = (0 until dimensions).map { dim ->
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

                val bounds: RectangleSet? = if (parameterIndex == -1 || denominator == 0.0) {
                    //there is no parameter in this equation
                    if (derivationValue > 0 == positive) tt else ff
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
                        Rectangle(r).toRectangleSet()
                    }
                }
                bounds
            }
            //save also dual values THIS DOES NOT WORK WHEN DERIVATION IS ZERO!
            //(if (positive) negativeVertexCache else positiveVertexCache)[vertex] = p.map { it?.not() ?: tt }
            p
        }[dimension]
    }

}