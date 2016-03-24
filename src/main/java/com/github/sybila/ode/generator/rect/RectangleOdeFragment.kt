package com.github.sybila.ode.generator.rect

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.PartitionFunction
import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.model.Model
import java.util.*

class RectangleOdeFragment(
        model: Model,
        partitioning: PartitionFunction<IDNode>,
        createSelfLoops: Boolean = true
) : AbstractOdeFragment<RectangleColors>(model, partitioning, createSelfLoops) {

    override val emptyColors = RectangleColors()
    override val fullColors = if (model.parameters.isEmpty()) RectangleColors(Rectangle(doubleArrayOf())) else RectangleColors(
            Rectangle(model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray())
    )

    /*** Successor/Predecessor resolving ***/

    /**
     * These arrays will cache all equation evaluations so that we can reuse them.
     * (Each value is used up to 2*|dim| times)
     * Whole operation is controlled by parameters array:
     * -2 - this value hasn't been computed yet
     * -1 - this value is computed and has no parameter
     * >=0 - this value is compute and has parameter
     */

    private val derivations = Array(dimensions) {
        DoubleArray(encoder.vertexCount)
    }

    private val denominators = Array(dimensions) {
        DoubleArray(encoder.vertexCount)
    }

    private val parameters = Array(dimensions) {
        IntArray(encoder.vertexCount) { -2 }
    }

    private fun evaluate(dim: Int, coordinates: IntArray, vertex: Int) {
        if (parameters[dim][vertex] == -2) {    //evaluate!
            var derivationValue = 0.0
            var denominator = 0.0
            var parameterIndex = -1
            for (summand in model.variables[dim].equation) {
                var partialSum = summand.constant
                for (v in summand.variableIndices) {
                    partialSum *= model.variables[v].thresholds[coordinates[v]]
                }
                if (partialSum != 0.0) {
                    for (function in summand.evaluable) {
                        val index = function.varIndex
                        partialSum *= function(model.variables[index].thresholds[coordinates[index]])
                    }
                }
                if (summand.hasParam()) {
                    parameterIndex = summand.paramIndex
                    denominator += partialSum
                } else {
                    derivationValue += partialSum
                }
            }
            derivations[dim][vertex] = derivationValue
            denominators[dim][vertex] = denominator
            parameters[dim][vertex] = parameterIndex
        }
    }

    /**
     * This map will cache all edge colors, so that we don't have to recompute them.
     */

    //First dimension: Nodes
    //second: dimensions
    //third: upper/lower facet
    //fourth: Incoming/outgoing
    private val facets = HashMap<IDNode, Array<Array<Array<RectangleColors?>>>>()

    private fun getFacets(from: IDNode) = facets.getOrPut(from) { Array(dimensions) {
        Array(2) { Array<RectangleColors?>(2) { null } }
    } }

    /**
     * Compute colors for which a incoming/outgoing transition is valid on the specified facet.
     * TODO: This should actually compute all four facets at the same time - we need them to compute self loops anyway...
     */
    override fun getFacetColors(from: IDNode, dim: Int, upper: Boolean, incoming: Boolean): RectangleColors {
        val myFacets = getFacets(from)
        val u = if (upper) 1 else 0
        val i = if (incoming) 1 else 0
        if (myFacets[dim][u][i] != null) {
            return myFacets[dim][u][i]!!;
        } else {
            //compute facet

            var upperParameterBound = Double.NEGATIVE_INFINITY;
            var lowerParameterBound = Double.POSITIVE_INFINITY;

            var parameterIndex = -1
            var edgeValid = false

            //If there is a vertex and parameter interval for equation is positive/negative
            //(depends where we want to go), edge should be valid.
            //upperParameterBound should be maximal parametric value for which this condition holds.
            //lowerParameterBound should be minimal value for which this holds.

            for (coordinates in facet(from, dim, upper)) {
                val vertex = encoder.encodeVertex(coordinates)
                evaluate(dim, coordinates, vertex)
                val value = derivations[dim][vertex]
                val denominator = denominators[dim][vertex]
                val parameter = parameters[dim][vertex]

                if (parameter == -1) {
                    //there is no parameter in this equation
                    if ((value > 0 && ((upper && !incoming) || (!upper && incoming))) ||
                        (value < 0 && ((upper && incoming) || (!upper && !incoming)))) {
                        edgeValid = true
                    }
                } else if (denominator == 0.0) {
                    //denominator is zero, decide only based on value
                    parameterIndex = parameter
                    if ((value > 0 && ((upper && !incoming) || (!upper && incoming))) ||
                        (value < 0 && ((upper && incoming) || (!upper && !incoming)))) {
                        edgeValid = true
                        //parameter bounds will be updated after for loop
                    }
                } else {
                    parameterIndex = parameter
                    //if you divide by negative number, you have to flip the condition
                    val newIncoming = if (denominator > 0) incoming else !incoming
                    val split = (-value) / denominator

                    if (split <= lowerParameterBound && ((upper && !newIncoming) || (!upper && newIncoming))) {
                        edgeValid = true
                        lowerParameterBound = split
                    }
                    if (split >= upperParameterBound && ((upper && newIncoming) || (!upper && !newIncoming))) {
                        edgeValid = true
                        upperParameterBound = split
                    }
                }

            }

            val bounds = if (parameterIndex != -1) {
                model.parameters[parameterIndex].range
            } else {
                Pair(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
            }

            //If we haven't found any max/min, we substitute bounds from model
            if (lowerParameterBound == Double.POSITIVE_INFINITY) {
                lowerParameterBound = bounds.first
            }
            if (upperParameterBound == Double.NEGATIVE_INFINITY) {
                upperParameterBound = bounds.second
            }

            val colors = when {
                !edgeValid -> emptyColors
                parameterIndex == -1 -> fullColors
                lowerParameterBound >= bounds.second -> emptyColors
                upperParameterBound <= bounds.first -> emptyColors
                else -> {
                    //constructs a valid rectangle with specified constrains
                    val rectangle = DoubleArray(2*model.parameters.size) { i ->
                        if (i / 2 == parameterIndex) {
                            if (i % 2 == 0) Math.max(bounds.first, lowerParameterBound)
                            else Math.min(bounds.second, upperParameterBound)
                        } else {
                            if (i % 2 == 0) model.parameters[i/2].range.first
                            else model.parameters[i/2].range.second
                        }
                    }
                    RectangleColors(Rectangle(rectangle))
                }
            }

            myFacets[dim][u][i] = colors

            //also update facets for related nodes
            //(every facet is shared by two nodes, so if you compute upper incoming facet, you
            //have also computed lower outgoing facet for your upper neighbor
            if (upper) {
                encoder.higherNode(from, dim)?.apply {
                    val alternativeFacets = getFacets(this)
                    //lower facet, negation of incoming
                    alternativeFacets[dim][0][(i+1) % 2] = colors
                }
            } else {
                encoder.lowerNode(from, dim)?.apply {
                    val alternativeFacets = getFacets(this)
                    //upper facet, negation of incoming
                    alternativeFacets[dim][1][(i+1) % 2] = colors
                }
            }
            return colors
        }
    }


}