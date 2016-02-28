package cz.muni.fi.ode.generator

import cz.muni.fi.checker.*
import cz.muni.fi.ctl.*
import cz.muni.fi.ode.model.Model
import java.util.*

class OdeFragment(
        private val model: Model,
        partitioning: PartitionFunction<IDNode>
) : KripkeFragment<IDNode, RectangleColors>, PartitionFunction<IDNode> by partitioning {

    private val encoder = NodeEncoder(model)
    private val dimensions = model.variables.size

    private val emptyColors = RectangleColors()
    private val fullColors = if (model.parameters.isEmpty()) RectangleColors(Rectangle(doubleArrayOf())) else RectangleColors(
            Rectangle(model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray())
    )

    /*** PROPOSITION RESOLVING ***/

    override fun allNodes(): Nodes<IDNode, RectangleColors>
            = enumerateStates(0, 0, model.variables[0].thresholds.size - 2)

    override fun validNodes(a: Atom): Nodes<IDNode, RectangleColors> {
        return when(a) {
            True -> allNodes()
            False -> nodesOf(emptyColors)
            is FloatProposition -> {
                val p = if (a.left is Variable && a.right is Constant) {
                    a   //take as is
                } else if (a.left is Constant && a.right is Variable) {
                    //flip
                    FloatProposition(a.right, when(a.compareOp) {
                        CompareOp.EQ, CompareOp.NEQ -> a.compareOp
                        CompareOp.GT -> CompareOp.LT
                        CompareOp.GT_EQ -> CompareOp.LT_EQ
                        CompareOp.LT -> CompareOp.GT
                        CompareOp.LT_EQ -> CompareOp.GT_EQ
                    }, a.left)
                } else throw IllegalArgumentException("Unsupported float proposition: $a")

                val dimension = model.dimensionFromName((p.left as Variable).name)
                val tIndex = model.variables[dimension].thresholds.indexOf(
                        (p.right as Constant).value
                )
                if (tIndex < 0) {
                    throw IllegalArgumentException(
                            "You have to use an exact threshold in propositions! Proposition: $a, Thresholds: ${model.variables[dimension]}"
                    )
                }
                when (p.compareOp) {
                    CompareOp.EQ, CompareOp.NEQ ->
                        throw IllegalArgumentException("Equality does not make sense! $a")
                    CompareOp.GT, CompareOp.GT_EQ ->
                        enumerateStates(dimension, tIndex, model.variables[dimension].thresholds.size - 2)
                    CompareOp.LT, CompareOp.LT_EQ ->
                        enumerateStates(dimension, 0, tIndex)
                }
            }
            else -> throw IllegalArgumentException("Unsupported proposition: $a")
        }
    }


    /**
     * Return all states of the model restricted on given dimension by threshold index interval.
     * On all other dimensions, every possible threshold is combined in result.
     * (With respect to global coordinate bounds)
     * @param dimension Index of variable where state space is split.
     * @param lowerBound Index of lowest threshold that should be returned.
     * @param upperBound Index of highest threshold that should be returned.
     * @return Nodes matching requested criteria.
     */
    private fun enumerateStates(dimension: Int, lowerBound: Int, upperBound: Int): Nodes<IDNode, RectangleColors> {

        //compared to computeBorderVerticesForState() which is called for every state multiple times,
        //this method is called only once when proposition is evaluated.
        //So any performance aspect of buffer allocation is not really a concern here.

        val results = HashMap<IDNode, RectangleColors>()

        if (upperBound < lowerBound) {
            return results.toNodes(emptyColors)
        }

        //helper array that holds incomplete coordinates during computation
        val coordinateBuffer = IntArray(dimensions)

        //helper array that specifies whether the dimension is fully computed
        //A non negative number is a index that still needs to be processed.
        //Negative number means everything is done.
        val remainingWork = IntArray(dimensions)
        Arrays.fill(remainingWork, -1)

        var activeIndex = 0    //dimension of model being currently explored
        while (activeIndex >= 0) {
            if (activeIndex == dimensions) {
                //if all dimensions are processed, put resulting node in the collection
                val node = encoder.encodeNode(coordinateBuffer)
                if (node.ownerId() == myId) {
                    results.put(encoder.encodeNode(coordinateBuffer), fullColors)
                }
                activeIndex--
                //skip dimensions that are already completed
                while (activeIndex >= 0 && remainingWork[activeIndex] < 0) {
                    activeIndex--
                }
            } else if (activeIndex == dimension) {
                //if we are working on restricted interval, we do not want to process all thresholds, just
                //the ones within restricted bounds, so we add extra conditions to account for that.

                if (remainingWork[activeIndex] < 0) {
                    remainingWork[activeIndex] = upperBound
                }

                coordinateBuffer[activeIndex] = remainingWork[activeIndex]
                remainingWork[activeIndex] -= 1
                if (remainingWork[activeIndex] < lowerBound) {
                    remainingWork[activeIndex] = -1
                }

                activeIndex++  //move to higher dimension
            } else {
                //if we are working on any general dimension, we start from highest threshold index and work
                //toward zero or global bound. After that, dimension is done and is marked accordingly (-1)

                if (remainingWork[activeIndex] < 0) {
                    //if this is true, we are coming from lower dimension and we need to init new search
                    remainingWork[activeIndex] = model.variables[activeIndex].thresholds.size - 2   //index of highest state in dimension
                }

                coordinateBuffer[activeIndex] = remainingWork[activeIndex]
                remainingWork[activeIndex] -= 1

                activeIndex++ //move to higher dimension
            }
        }

        return results.toNodes(emptyColors)
    }

    /*** Successor/Predecessor resolving ***/

    /**
     * These arrays will cache all equation evaluations so that we can reuse them.
     * (Each value is used up to 2*|dim| times)
     * Whole operation is controlled by parameters array:
     * -2 - this value hasn't been computed yet
     * -1 - this value is computed and has no parameter
     * >=0 - this value is compute and has parameter
     */

    private val derivations = Array(model.variables.size) {
        DoubleArray(encoder.vertexCount)
    }

    private val denominators = Array(model.variables.size) {
        DoubleArray(encoder.vertexCount)
    }

    private val parameters = Array(model.variables.size) {
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
    private fun getFacetColors(from: IDNode, dim: Int, upper: Boolean, incoming: Boolean): RectangleColors {
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

    private val successorCache = Array<Nodes<IDNode, RectangleColors>?>(encoder.stateCount) { null }
    private val predecessorCache = Array<Nodes<IDNode, RectangleColors>?>(encoder.stateCount) { null }

    override val predecessors: IDNode.() -> Nodes<IDNode, RectangleColors> = {
        if (predecessorCache[this.id] == null) {
            predecessorCache[this.id] = getDirectedEdges(this, false)
        }
        predecessorCache[this.id]!!
    }

    override val successors: IDNode.() -> Nodes<IDNode, RectangleColors> = {
        if (successorCache[this.id] == null) {
            successorCache[this.id] = getDirectedEdges(this, true)
        }
        successorCache[this.id]!!
    }

    private fun getDirectedEdges(target: IDNode, successors: Boolean): Nodes<IDNode, RectangleColors> {
        val results = HashMap<IDNode, RectangleColors>()
        var selfLoop = fullColors
        for (dim in model.variables.indices) {
            //we need to compute all of them, because we need them to determine the self loops
            val upperIncoming = getFacetColors(from = target, dim = dim, incoming = true, upper = true)
            val upperOutgoing = getFacetColors(from = target, dim = dim, incoming = false, upper = true)
            val lowerIncoming = getFacetColors(from = target, dim = dim, incoming = true, upper = false)
            val lowerOutgoing = getFacetColors(from = target, dim = dim, incoming = false, upper = false)


            encoder.higherNode(target, dim)?.apply {
                val colors = if (successors) upperOutgoing else upperIncoming
                if (colors.isNotEmpty()) results.put(this, colors)

                //subtract flow
                val positiveFlow = (lowerIncoming intersect upperOutgoing) - (lowerOutgoing + upperIncoming)
                selfLoop -= positiveFlow
            }
            encoder.lowerNode(target, dim)?.apply {
                val colors = if (successors) lowerOutgoing else lowerIncoming
                if (colors.isNotEmpty()) results.put(this, colors)

                //subtract flow
                val negativeFlow = (lowerOutgoing intersect upperIncoming) - (lowerIncoming + upperOutgoing)
                selfLoop -= negativeFlow
            }
        }
        if (selfLoop.isNotEmpty()) {
            results.put(target, selfLoop)
        }
        return results.toNodes(emptyColors)
    }

    private fun facet(node: IDNode, dimension: Int, upper: Boolean): Iterable<IntArray> = object : Iterable<IntArray> {

        override fun iterator(): Iterator<IntArray> = object : Iterator<IntArray> {

            //this is basically simulating recursion using the needsMoreWork array.

            private val results = IntArray(model.variables.size) { 0 }
            private val needsMoreWork = BooleanArray(model.variables.size) { false }
            private var activeIndex = 0

            override fun hasNext(): Boolean = activeIndex >= 0

            override fun next(): IntArray {
                while (activeIndex != model.variables.size) {
                    if (activeIndex == dimension) {
                        //if we are working on fixed dimension, we do not process both lower and higher thresholds,
                        //we decide based on parameter whether we want strictly lower or higher
                        results[activeIndex] = encoder.threshold(node, activeIndex, upper)
                        needsMoreWork[activeIndex] = false
                        activeIndex++  //move to higher dimension
                    } else {
                        //if we are working on any general dimension, we first process all lower thresholds and
                        //mark dimension as not completed. When all lower thresholds are prepared, we process all higher
                        //thresholds and only after that mark the dimension as done.
                        if (!needsMoreWork[activeIndex]) {
                            results[activeIndex] = encoder.threshold(node, activeIndex, false)
                            needsMoreWork[activeIndex] = true
                        } else {
                            results[activeIndex] = encoder.threshold(node, activeIndex, true)
                            needsMoreWork[activeIndex] = false
                        }
                        activeIndex++ //move to higher dimension
                    }
                }
                //if all dimensions are processed, we have a valid vertex
                activeIndex--
                //skip dimensions that are already completed
                while (activeIndex >= 0 && !needsMoreWork[activeIndex]) {
                    activeIndex--
                }
                return results
            }

        }

    }
}