package com.github.sybila.ode.generator

import com.github.sybila.checker.*
import com.github.sybila.huctl.*
import com.github.sybila.ode.model.OdeModel
import java.util.*


abstract class AbstractOdeFragment<Params : Any>(
        protected val model: OdeModel,
        private val createSelfLoops: Boolean,
        solver: Solver<Params>
) : Model<Params>, Solver<Params> by solver {

    protected val encoder = NodeEncoder(model)
    protected val dimensions = model.variables.size

    init {
        if (dimensions >= 30) throw IllegalStateException("Too many dimensions! Max. supported: 30")
    }

    override val stateCount: Int = model.variables.fold(1) { a, v ->
        a * (v.thresholds.size - 1)
    }

    /**
     * Return params for which dimensions derivation at vertex is positive/negative.
     *
     * Return null if there are no such parameters.
     */
    abstract fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): Params?

    //Facet param cache.
    //(a,b) -> P <=> p \in P: a -p-> b
    private val facetColors = HashMap<FacetId, Params>()

    private enum class Orientation {
        PositiveIn, PositiveOut, NegativeIn, NegativeOut
    }
    private data class FacetId(val state: Int, val dimension: Int, val orientation: Orientation)

    private fun getFacetColors(from: Int, dimension: Int, orientation: Orientation): Params
            = facetColors.computeIfAbsent(FacetId(from, dimension, orientation)) {
        //iterate over vertices
        val positiveFacet = if (orientation == Orientation.PositiveIn || orientation == Orientation.PositiveOut) 1 else 0
        val positiveDerivation = orientation == Orientation.PositiveOut || orientation == Orientation.NegativeIn
        val colors = vertexMasks.asSequence()
                .filter { it.shr(dimension).and(1) == positiveFacet }
                .map { encoder.nodeVertex(from, it) }
                .fold(ff) { a, vertex ->
                    //println("V: $vertex ${getVertexColor(vertex, dimension, positiveDerivation)}")
                    getVertexColor(vertex, dimension, positiveDerivation)?.let { a or it } ?: a
                }

        //also update dual facet
        if (orientation == Orientation.PositiveIn || orientation == Orientation.PositiveOut) {
            encoder.higherNode(from, dimension)?.let { higher ->
                val dual = if (orientation == Orientation.PositiveIn) {
                    Orientation.NegativeOut
                } else { Orientation.NegativeIn }
                facetColors[FacetId(higher, dimension, dual)] = colors
            }
        } else {
            encoder.lowerNode(from, dimension)?.let { lower ->
                val dual = if (orientation == Orientation.NegativeIn) {
                    Orientation.PositiveOut
                } else {
                    Orientation.PositiveIn
                }
                facetColors[FacetId(lower, dimension, dual)] = colors
            }
        }

        colors
    }

    //enumerate all bit masks corresponding to vertices of a state
    private val vertexMasks: List<Int> = (0 until dimensions).fold(listOf(0)) { a, d ->
        a.map { it.shl(1) }.flatMap { listOf(it, it.or(1)) }
    }

    /*** PROPOSITION RESOLVING ***/


    override fun Formula.Atom.Float.eval(): StateMap<Params> {
        val left = this.left
        val right = this.right
        val dimension: Int
        val threshold: Int
        val gt: Boolean
        when {
            left is Expression.Variable && right is Expression.Constant -> {
                dimension = model.variables.indexOfFirst { it.name == left.name }
                if (dimension < 0) throw IllegalArgumentException("Unknown variable ${left.name}")
                threshold = model.variables[dimension].thresholds.indexOfFirst { it == right.value }
                if (threshold < 0) throw IllegalArgumentException("Unknown threshold ${right.value}")

                gt = when (this.cmp) {
                    CompareOp.EQ, CompareOp.NEQ -> throw IllegalArgumentException("${this.cmp} comparison not supported.")
                    CompareOp.GT, CompareOp.GE -> true
                    CompareOp.LT, CompareOp.LE -> false
                }
            }
            left is Expression.Constant && right is Expression.Variable -> {
                dimension = model.variables.indexOfFirst { it.name == right.name }
                if (dimension < 0) throw IllegalArgumentException("Unknown variable ${right.name}")
                threshold = model.variables[dimension].thresholds.indexOfFirst { it == left.value }
                if (threshold < 0) throw IllegalArgumentException("Unknown threshold ${left.value}")

                gt = when (this.cmp) {
                    CompareOp.EQ, CompareOp.NEQ -> throw IllegalArgumentException("${this.cmp} comparison not supported.")
                    CompareOp.GT, CompareOp.GE -> false
                    CompareOp.LT, CompareOp.LE -> true
                }
            }
            else -> throw IllegalAccessException("Proposition is too complex: ${this}")
        }
        val dimensionSize = model.variables[dimension].thresholds.size - 1
        return CutStateMap(
                encoder = encoder,
                dimension = dimension,
                threshold = threshold,
                gt = gt,
                stateCount = stateCount,
                value = tt,
                default = ff,
                sizeHint = if (gt) {
                    (stateCount / dimensionSize) * (dimensionSize - threshold + 1)
                } else {
                    (stateCount / dimensionSize) * (threshold - 1)
                }
        )
    }

    override fun Formula.Atom.Transition.eval(): StateMap<Params> {
        val dimension = model.variables.indexOfFirst { it.name == this.name }
        if (dimension < 0) throw IllegalStateException("Unknown variable name: ${this.name}")
        return LazyStateMap(stateCount, ff) {
            val c = getFacetColors(it, dimension, when {
                facet == Facet.POSITIVE && direction == Direction.IN -> Orientation.PositiveIn
                facet == Facet.POSITIVE && direction == Direction.OUT -> Orientation.PositiveOut
                facet == Facet.NEGATIVE && direction == Direction.IN -> Orientation.NegativeIn
                else -> Orientation.NegativeOut
            })
            val exists = (facet == Facet.POSITIVE && encoder.higherNode(it, dimension) != null)
            || (facet == Facet.NEGATIVE && encoder.lowerNode(it, dimension) != null)
            if (exists && c.canSat()) c else null
        }
    }

    /*** Successor/Predecessor resolving ***/

    private val successorCache = HashMap<Int, List<Transition<Params>>>()
    private val pastSuccessorCache = HashMap<Int, List<Transition<Params>>>()
    private val predecessorCache = HashMap<Int, List<Transition<Params>>>()
    private val pastPredecessorCache = HashMap<Int, List<Transition<Params>>>()

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<Params>>
        = getStep(this, timeFlow, false).iterator()

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<Params>>
        = getStep(this, timeFlow, true).iterator()

    private fun getStep(from: Int, timeFlow: Boolean, successors: Boolean): List<Transition<Params>> {
        return (when {
            timeFlow && successors -> successorCache
            timeFlow && !successors -> predecessorCache
            !timeFlow && successors -> pastSuccessorCache
            else -> pastPredecessorCache
        }).computeIfAbsent(from) {
            val result = ArrayList<Transition<Params>>()
            //selfLoop <=> !positiveFlow && !negativeFlow <=> !(positiveFlow || negativeFlow)
            //positiveFlow = (-in && +out) && !(-out || +In) <=> -in && +out && !-out && !+In
            var selfloop = ff
            for (dim in model.variables.indices) {
                val dimName = model.variables[dim].name
                val positiveOut = lazy {
                    getFacetColors(from, dim, if (timeFlow) Orientation.PositiveOut else Orientation.PositiveIn)
                }
                val positiveIn = lazy {
                    getFacetColors(from, dim, if (timeFlow) Orientation.PositiveIn else Orientation.PositiveOut)
                }
                val negativeOut = lazy {
                    getFacetColors(from, dim, if (timeFlow) Orientation.NegativeOut else Orientation.NegativeIn)
                }
                val negativeIn = lazy {
                    getFacetColors(from, dim, if (timeFlow) Orientation.NegativeIn else Orientation.NegativeOut)
                }

                /*println("$from")
                println("${positiveIn.value} <-")
                println("${positiveOut.value} ->")
                println("-> ${negativeIn.value}")
                println("<- ${negativeOut.value}")*/

                encoder.higherNode(from, dim)?.let { higher ->
                    val colors = (if (successors) positiveOut else positiveIn).value
                    colors.minimize()
                    if (colors.isSat()) {
                        result.add(Transition(
                                target = higher,
                                direction = if (successors) dimName.increaseProp() else dimName.decreaseProp(),
                                bound = colors
                        ))
                    }

                    if (createSelfLoops) {
                        //selfLoop -= positiveFlow
                        val positiveFlow = negativeIn.value and positiveOut.value and negativeOut.value.not() and positiveIn.value.not()
                        selfloop = selfloop or positiveFlow
                    }
                }

                encoder.lowerNode(from, dim)?.let { lower ->
                    val colors = (if (successors) negativeOut else negativeIn).value
                    colors.minimize()
                    if (colors.isSat()) {
                        result.add(Transition(
                                target = lower,
                                direction = if (successors) dimName.decreaseProp() else dimName.increaseProp(),
                                bound = colors
                        ))
                    }

                    if (createSelfLoops) {
                        val negativeFlow = negativeOut.value and positiveIn.value and negativeIn.value.not() and positiveOut.value.not()
                        selfloop = selfloop or negativeFlow
                    }
                }

            }

            selfloop = selfloop.not()
            if (selfloop.isSat()) {
                selfloop.minimize()
                result.add(Transition(from, DirectionFormula.Atom.Loop, selfloop))
            }
            result
        }
    }

}