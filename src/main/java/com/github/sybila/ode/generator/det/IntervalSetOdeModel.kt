package com.github.sybila.ode.generator.det

import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.model.OdeModel
import java.util.*

/**
 * IntervalSetOdeModel uses a 1D adaptive grid to maintain the set of parameter values.
 * It is faster than general rectangle model, because it does not have explicit
 * rectangle objects, but it is applicable only to 1-parameter models.
 *
 * @see [RectangleSetOdeModel]
 */
class IntervalSetOdeModel(
        model: OdeModel,
        createSelfLoops: Boolean = true
) : AbstractOdeFragment<IntervalSet>(model, createSelfLoops, IntervalSetSolver(
        model.parameters[0].range.let { it.first to it.second }
)) {

    private val boundsRect = model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray()

    private val positiveVertexCache = HashMap<Int, List<IntervalSet?>>()
    private val negativeVertexCache = HashMap<Int, List<IntervalSet?>>()

    override fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): IntervalSet? {
        return (if (positive) positiveVertexCache else negativeVertexCache).computeIfAbsent(vertex) {
            val p: List<IntervalSet?> = (0 until dimensions).map { dim ->
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

                val bounds: IntervalSet? = if (parameterIndex == -1 || denominator == 0.0) {
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
                        Rectangle(r).toIntervalSet()
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