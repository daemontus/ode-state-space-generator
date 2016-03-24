package com.github.sybila.ode.generator

import com.github.sybila.checker.*
import com.github.sybila.ctl.*
import com.github.sybila.ode.model.Model
import java.util.*


abstract class AbstractOdeFragment<C: Colors<C>>(
        protected val model: Model,
        partitioning: PartitionFunction<IDNode>,
        private val createSelfLoops: Boolean
) : KripkeFragment<IDNode, C>, PartitionFunction<IDNode> by partitioning {

    protected  val encoder = NodeEncoder(model)
    protected  val dimensions = model.variables.size

    abstract val emptyColors: C
    abstract val fullColors: C

    /*** PROPOSITION RESOLVING ***/

    override fun allNodes(): Nodes<IDNode, C>
            = enumerateStates(0, 0, model.variables[0].thresholds.size - 2)

    override fun validNodes(a: Atom): Nodes<IDNode, C> {
        return when(a) {
            True -> allNodes()
            False -> nodesOf(emptyColors)
            is FloatProposition -> {
                val p = if (a.left is Variable && a.right is Constant) {
                    a   //take as is
                } else if (a.left is Constant && a.right is Variable) {
                    //flip
                    FloatProposition(a.right, when (a.compareOp) {
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
                        enumerateStates(dimension, 0, tIndex - 1)
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
    private fun enumerateStates(dimension: Int, lowerBound: Int, upperBound: Int): Nodes<IDNode, C> {

        //compared to computeBorderVerticesForState() which is called for every state multiple times,
        //this method is called only once when proposition is evaluated.
        //So any performance aspect of buffer allocation is not really a concern here.

        val results = HashMap<IDNode, C>()

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


    /**
     * Iterate over all vertices of specified facet
     */
    protected fun facet(node: IDNode, dimension: Int, upper: Boolean): Iterable<IntArray> = object : Iterable<IntArray> {

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

    /*** Successor/Predecessor resolving ***/

    /**
     * Compute a color set that is valid for specified facet and direction
     */
    protected abstract fun getFacetColors(from: IDNode, dim: Int, upper: Boolean, incoming: Boolean): C

    private val successorCache = Array<Nodes<IDNode, C>?>(encoder.stateCount) { null }
    private val predecessorCache = Array<Nodes<IDNode, C>?>(encoder.stateCount) { null }

    override val predecessors: IDNode.() -> Nodes<IDNode, C> = {
        if (predecessorCache[this.id] == null) {
            predecessorCache[this.id] = getDirectedEdges(this, false)
        }
        predecessorCache[this.id]!!
    }

    override val successors: IDNode.() -> Nodes<IDNode, C> = {
        if (successorCache[this.id] == null) {
            successorCache[this.id] = getDirectedEdges(this, true)
        }
        successorCache[this.id]!!
    }

    private fun getDirectedEdges(target: IDNode, successors: Boolean): Nodes<IDNode, C> {
        val results = HashMap<IDNode, C>()
        var selfLoop = fullColors
        for (dim in model.variables.indices) {
            //we need to compute all of them, because we need them to determine the self loops
            val upperIncoming = lazy { getFacetColors(from = target, dim = dim, incoming = true, upper = true) }
            val upperOutgoing = lazy { getFacetColors(from = target, dim = dim, incoming = false, upper = true) }
            val lowerIncoming = lazy { getFacetColors(from = target, dim = dim, incoming = true, upper = false) }
            val lowerOutgoing = lazy { getFacetColors(from = target, dim = dim, incoming = false, upper = false) }


            encoder.higherNode(target, dim)?.apply {
                val colors = (if (successors) upperOutgoing else upperIncoming).value
                if (colors.isNotEmpty()) results.put(this, colors)

                //subtract flow
                if (createSelfLoops) {
                    val positiveFlow = (lowerIncoming.value intersect upperOutgoing.value) - (lowerOutgoing.value + upperIncoming.value)
                    selfLoop -= positiveFlow
                }
            }
            encoder.lowerNode(target, dim)?.apply {
                val colors = (if (successors) lowerOutgoing else lowerIncoming).value
                if (colors.isNotEmpty()) results.put(this, colors)

                //subtract flow
                if (createSelfLoops) {
                    val negativeFlow = (lowerOutgoing.value intersect upperIncoming.value) - (lowerIncoming.value + upperOutgoing.value)
                    selfLoop -= negativeFlow
                }
            }
        }
        if (createSelfLoops && selfLoop.isNotEmpty()) {
            results.put(target, selfLoop)
        }
        return results.toNodes(emptyColors)
    }

}