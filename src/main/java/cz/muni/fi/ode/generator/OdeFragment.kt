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

    private val emptyColors = RectangleColors()
    private val fullColors = RectangleColors(
            Rectangle(model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray())
    )

    private val successorCache = HashMap<IDNode, Nodes<IDNode, RectangleColors>>()
    private val predecessorCache = HashMap<IDNode, Nodes<IDNode, RectangleColors>>()

    override val predecessors: IDNode.() -> Nodes<IDNode, RectangleColors> = {
        val cached = predecessorCache[this]
        if (cached == null) {
            val computed = computeDirectedEdges(this, false)
            predecessorCache[this] = computed
            computed
        } else cached
    }

    override val successors: IDNode.() -> Nodes<IDNode, RectangleColors> = {
        val cached = successorCache[this]
        if (cached == null) {
            val computed = computeDirectedEdges(this, true)
            successorCache[this] = computed
            computed
        } else cached
    }

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

    private var derivationValue = 0.0
    private var denominator = 0.0
    private var parameterIndex = -1

    private fun computeDirectedEdges(target: IDNode, successors: Boolean): Nodes<IDNode, RectangleColors> {

        val results = HashMap<IDNode, RectangleColors>()

        //go through all dimension of model and compute results for each of them separately
        for (dim in model.variables.indices) {

            var lowerPositiveDirection = false
            var lowerNegativeDirection = false
            var upperPositiveDirection = false
            var upperNegativeDirection = false

            var upperParameterSplit = Double.POSITIVE_INFINITY
            var lowerParameterSplit = Double.NEGATIVE_INFINITY

            for (vertex in target.lowerFacet(dim)) {

                evaluate(vertex, dim)

                if (parameterIndex != -1) {

                    // lowest and highest values of parameter space for chosen variable
                    val paramBounds = model.parameters[parameterIndex].range

                    if (Math.abs(denominator) != 0.0) {

                        val parameterSplitValue = if (-derivationValue / denominator == -0.0) 0.0 else -derivationValue / denominator

                        if (successors) {
                            if (denominator < 0 && paramBounds.second >= parameterSplitValue) {
                                lowerNegativeDirection = true

                                if (lowerParameterSplit == java.lang.Double.NEGATIVE_INFINITY || lowerParameterSplit > parameterSplitValue)
                                    lowerParameterSplit = parameterSplitValue
                            }
                            if (derivationValue < 0 && denominator > 0 && paramBounds.first <= parameterSplitValue) {
                                lowerNegativeDirection = true

                                if (upperParameterSplit == java.lang.Double.POSITIVE_INFINITY || upperParameterSplit < parameterSplitValue) {
                                    upperParameterSplit = parameterSplitValue
                                }
                            }
                        } else {
                            // !successors
                            if (denominator > 0 && paramBounds.second >= parameterSplitValue) {
                                lowerPositiveDirection = true

                                if (lowerParameterSplit == java.lang.Double.NEGATIVE_INFINITY || lowerParameterSplit > parameterSplitValue) {
                                    lowerParameterSplit = parameterSplitValue
                                }
                            }
                            if (derivationValue > 0 && denominator < 0 && paramBounds.first <= parameterSplitValue) {
                                lowerPositiveDirection = true

                                if (upperParameterSplit == java.lang.Double.POSITIVE_INFINITY || upperParameterSplit < parameterSplitValue) {
                                    upperParameterSplit = parameterSplitValue
                                }
                            }
                        }
                    } else {
                        // abs(denominator) == 0 (ERGO: it might be at border of state space)
                        if (successors) {
                            if (derivationValue < 0) {
                                lowerNegativeDirection = true
                                lowerParameterSplit = paramBounds.first //Double.NEGATIVE_INFINITY;
                                upperParameterSplit = paramBounds.second //Double.POSITIVE_INFINITY;
                            }
                        } else {
                            // !successors
                            if (derivationValue > 0) {
                                lowerPositiveDirection = true
                                lowerParameterSplit = paramBounds.first //Double.NEGATIVE_INFINITY;
                                upperParameterSplit = paramBounds.second //Double.POSITIVE_INFINITY;
                            }
                        }
                    }
                } else {
                    // paramIndex == -1 (ERGO: no unknown parameter in equation)
                    if (derivationValue < 0) {
                        lowerNegativeDirection = true
                    } else {
                        lowerPositiveDirection = true
                    }
                }

            }

            val lowerNode = encoder.lowerNode(target, dim)
            if (lowerNode != null) {
                if (successors && lowerNegativeDirection || !successors && lowerPositiveDirection) {

                    val colors = if (parameterIndex != -1) {
                        val rectangle = DoubleArray(model.parameters.size) { i ->
                            if (i / 2 == parameterIndex) {
                                if (i % 2 == 0) Math.max(model.parameters[i/2].range.first, lowerParameterSplit)
                                else Math.min(model.parameters[i/2].range.second, upperParameterSplit)
                            } else {
                                if (i % 2 == 0) model.parameters[i/2].range.first
                                else model.parameters[i/2].range.second
                            }
                        }
                        RectangleColors(Rectangle(rectangle))
                    } else {
                        fullColors
                    }

                    results[lowerNode] = colors
                }
            }

            upperParameterSplit = Double.POSITIVE_INFINITY
            lowerParameterSplit = Double.NEGATIVE_INFINITY

            for (vertex in target.upperFacet(dim)) {
                evaluate(vertex, dim)

                if (parameterIndex != -1) {

                    // lowest and highest values of parameter space for chosen variable
                    val paramBounds = model.parameters[parameterIndex].range

                    if (Math.abs(denominator) != 0.0) {

                        val parameterSplit = if (-derivationValue / denominator == -0.0) 0.0 else -derivationValue / denominator

                        if (!successors) {
                            if (denominator < 0 && paramBounds.second >= parameterSplit) {
                                upperNegativeDirection = true

                                if (lowerParameterSplit == java.lang.Double.NEGATIVE_INFINITY || lowerParameterSplit > parameterSplit) {
                                    lowerParameterSplit = parameterSplit
                                }
                            }
                            if (derivationValue < 0 && denominator > 0 && paramBounds.first <= parameterSplit) {
                                upperNegativeDirection = true

                                if (upperParameterSplit == java.lang.Double.POSITIVE_INFINITY || upperParameterSplit < parameterSplit) {
                                    upperParameterSplit = parameterSplit
                                }
                            }
                        } else {
                            // successors
                            if (denominator > 0 && paramBounds.second >= parameterSplit) {
                                upperPositiveDirection = true

                                if (lowerParameterSplit == java.lang.Double.NEGATIVE_INFINITY || lowerParameterSplit > parameterSplit) {
                                    lowerParameterSplit = parameterSplit
                                }
                            }
                            if (derivationValue > 0 && denominator < 0 && paramBounds.first <= parameterSplit) {
                                upperPositiveDirection = true

                                if (upperParameterSplit == java.lang.Double.POSITIVE_INFINITY || upperParameterSplit < parameterSplit) {
                                    upperParameterSplit = parameterSplit
                                }
                            }
                        }
                    } else {
                        // abs(denominator) == 0 (ERGO: it might be at border of state space)

                        if (!successors) {
                            if (derivationValue < 0) {
                                upperNegativeDirection = true
                                lowerParameterSplit = paramBounds.first //Double.NEGATIVE_INFINITY;
                                upperParameterSplit = paramBounds.second //Double.POSITIVE_INFINITY;
                            }
                        } else {
                            // successors
                            if (derivationValue > 0) {
                                upperPositiveDirection = true
                                lowerParameterSplit = paramBounds.first //Double.NEGATIVE_INFINITY;
                                upperParameterSplit = paramBounds.second //Double.POSITIVE_INFINITY;
                            }
                        }
                    }

                } else {
                    // paramIndex == -1 (ERGO: no unknown parameter in equation)
                    if (derivationValue < 0) {
                        upperNegativeDirection = true
                    } else {
                        upperPositiveDirection = true
                    }
                }
            }

            val upperNode = encoder.higherNode(target, dim)
            if (upperNode != null) {
                if (successors && upperPositiveDirection || !successors && upperNegativeDirection) {

                    val colors = if (parameterIndex != -1) {
                        val rectangle = DoubleArray(model.parameters.size) { i ->
                            if (i / 2 == parameterIndex) {
                                if (i % 2 == 0) Math.max(model.parameters[i/2].range.first, lowerParameterSplit)
                                else Math.min(model.parameters[i/2].range.second, upperParameterSplit)
                            } else {
                                if (i % 2 == 0) model.parameters[i/2].range.first
                                else model.parameters[i/2].range.second
                            }
                        }
                        RectangleColors(Rectangle(rectangle))
                    } else {
                        fullColors
                    }

                    results.put(upperNode, colors)
                }
            }

        }

        //TODO: Self loops

        return results.toNodes(emptyColors)
    }

    private fun evaluate(vertex: IntArray, dim: Int) {
        val thresholds = model.variables[dim].thresholds
        derivationValue = 0.0
        denominator = 0.0
        parameterIndex = -1
        for (summand in model.variables[dim].equation) {
            var partialSum = summand.constant
            for (function in summand.evaluable) {
                partialSum *= function(thresholds[vertex[function.varIndex]])
            }
            if (summand.hasParam()) {
                parameterIndex = summand.paramIndex
                denominator += partialSum
            } else {
                derivationValue += partialSum
            }
        }
    }

    private fun IDNode.lowerFacet(dimension: Int): Iterable<IntArray> = facet(this, dimension, false)

    private fun IDNode.upperFacet(dimension: Int): Iterable<IntArray> = facet(this, dimension, true)

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
        val coordinateBuffer = IntArray(model.variables.size)

        //helper array that specifies whether the dimension is fully computed
        //A non negative number is a index that still needs to be processed.
        //Negative number means everything is done.
        val remainingWork = IntArray(model.variables.size)
        Arrays.fill(remainingWork, -1)

        var activeIndex = 0    //dimension of model being currently explored
        while (activeIndex >= 0) {
            if (activeIndex == model.variables.size) {
                //if all dimensions are processed, put resulting node in the collection
                val node = encoder.encode(coordinateBuffer)
                if (node.ownerId() == myId) {
                    results.put(encoder.encode(coordinateBuffer), fullColors)
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
                    remainingWork[activeIndex] = model.variables[activeIndex].thresholds.size - 1   //number of states in a dimension
                }

                coordinateBuffer[activeIndex] = remainingWork[activeIndex]
                remainingWork[activeIndex] -= 1

                activeIndex++ //move to higher dimension
            }
        }

        return results.toNodes(emptyColors)
    }
}