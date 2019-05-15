package com.github.sybila.ode.generator.v2

import com.github.sybila.checker.Solver
import com.github.sybila.checker.Transition
import com.github.sybila.checker.decreaseProp
import com.github.sybila.checker.increaseProp
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.ode.generator.NodeEncoder
import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.generator.rect.RectangleSolver
import com.github.sybila.ode.model.OdeModel
import java.util.ArrayList
import java.util.HashMap
import kotlin.math.pow


class Test(
        protected val model: OdeModel,
        private val createSelfLoops: Boolean
) : TransitionSystem<Int, MutableSet<Rectangle>>,
        Solver<MutableSet<Rectangle>> by RectangleSolver(Rectangle(model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray())) {

    protected val encoder = NodeEncoder(model)
    protected val dimensions = model.variables.size
    private val boundsRect = model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray()

    private val positiveVertexCache = HashMap<Int, List<MutableSet<Rectangle>?>>()
    private val negativeVertexCache = HashMap<Int, List<MutableSet<Rectangle>?>>()

    val stateCount: Int = model.variables.fold(1) { a, v ->
        a * (v.thresholds.size - 1)
    }

    private val facetColors = arrayOfNulls<Any>(stateCount * dimensions * 4)

    private val PositiveIn = 0
    private val PositiveOut = 1
    private val NegativeIn = 2
    private val NegativeOut = 3


    private val vertexMasks = IntArray(2.toDouble().pow(dimensions).toInt()) { i -> i }

    private fun facetIndex(from: Int, dimension: Int, orientation: Int)
            = from + (stateCount * dimension) + (stateCount * dimensions * orientation)

    private fun getFacetColors(from: Int, dimension: Int, orientation: Int): MutableSet<Rectangle> {
        val index = facetIndex(from, dimension, orientation)
        val value = facetColors[index] ?: run {
            //iterate over vertices
            val positiveFacet = if (orientation == PositiveIn || orientation == PositiveOut) 1 else 0
            val positiveDerivation = orientation == PositiveOut || orientation == NegativeIn


            val colors = vertexMasks
                    .filter { it.shr(dimension).and(1) == positiveFacet }
                    .fold(ff) { a, mask ->
                        val vertex = encoder.nodeVertex(from, mask)
                        getVertexColor(vertex, dimension, positiveDerivation)?.let { a or it } ?: a
                    }
            //val colors = tt

            colors.minimize()

            facetColors[index] = colors

            //also update dual facet
            if (orientation == PositiveIn || orientation == PositiveOut) {
                encoder.higherNode(from, dimension)?.let { higher ->
                    val dual = if (orientation == PositiveIn) {
                        NegativeOut
                    } else { NegativeIn }
                    facetColors[facetIndex(higher, dimension, dual)] = colors
                }
            } else {
                encoder.lowerNode(from, dimension)?.let { lower ->
                    val dual = if (orientation == NegativeIn) {
                        PositiveOut
                    } else {
                        PositiveIn
                    }
                    facetColors[facetIndex(lower, dimension, dual)] = colors
                }
            }

            colors
        }

        return value as MutableSet<Rectangle>
    }


    fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): MutableSet<Rectangle>? {
        return (if (positive) positiveVertexCache else negativeVertexCache).computeIfAbsent(vertex) {
            val p: List<MutableSet<Rectangle>?> = (0 until dimensions).map { dim ->
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

                val bounds: MutableSet<Rectangle>? = if (parameterIndex == -1 || denominator == 0.0) {
                    //there is no parameter in this equation
                    if ((positive && derivationValue > 0) || (!positive && derivationValue < 0)) tt else ff
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
                        mutableSetOf(Rectangle(r))
                    }
                }
                bounds
            }
            //save also dual values. THIS DOES NOT WORK WHEN DERIVATION IS ZERO!
            //(if (positive) negativeVertexCache else positiveVertexCache)[vertex] = p.map { it?.not() ?: tt }
            p
        }[dimension]
    }

    private val successorCache = HashMap<Int, List<Int>>(stateCount)
    private val predecessorCache = HashMap<Int, List<Int>>(stateCount)

    override fun Int.predecessors(): List<Int>
            = getStep(this, false)

    override fun Int.successors(): List<Int>
            = getStep(this, true)

    private fun getStep(from: Int, successors: Boolean): List<Int> {
        return (when {
            successors -> successorCache
            else -> predecessorCache
        }).computeIfAbsent(from) {
            val result = ArrayList<Int>()
            //selfLoop <=> !positiveFlow && !negativeFlow <=> !(positiveFlow || negativeFlow)
            //positiveFlow = (-in && +out) && !(-out || +In) <=> -in && +out && !-out && !+In
            var selfloop = tt
            for (dim in model.variables.indices) {

                val positiveOut = getFacetColors(from, dim, PositiveOut)
                val positiveIn = getFacetColors(from, dim, PositiveIn)
                val negativeOut = getFacetColors(from, dim, NegativeOut)
                val negativeIn = getFacetColors(from, dim, NegativeIn)

                encoder.higherNode(from, dim)?.let { higher ->
                    val colors = (if (successors) positiveOut else positiveIn)
                    if (colors.isSat()) {
                        result.add(higher)
                    }

                    if (createSelfLoops) {
                        val positiveFlow = negativeIn and positiveOut and (negativeOut or positiveIn).not()
                        selfloop = selfloop and positiveFlow.not()
                    }
                }

                encoder.lowerNode(from, dim)?.let { lower ->
                    val colors = (if (successors) negativeOut else negativeIn)
                    if (colors.isSat()) {
                        result.add(lower)
                    }

                    if (createSelfLoops) {
                        val negativeFlow = negativeOut and positiveIn and (negativeIn or positiveOut).not()
                        selfloop = selfloop and negativeFlow.not()
                    }
                }

            }

            if (selfloop.isSat()) {
                selfloop.minimize()
                result.add(from)
            }
            result
        }
    }

    override fun transitionParameters(source: Int, target: Int): MutableSet<Rectangle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun main(args: Array<String>) {
        val vertexMasks = IntArray(2.toDouble().pow(3).toInt()) { i -> i}
        println(vertexMasks)
    }
}