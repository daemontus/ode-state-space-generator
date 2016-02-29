package cz.muni.fi.ode.generator

import com.github.sybila.checker.IDNode
import cz.muni.fi.ode.model.Model

class NodeEncoder(
        private val model: Model
) {

    private val dimensionMultipliers = IntArray(model.variables.size)
    private val dimensionStateCounts = IntArray(model.variables.size) {
        model.variables[it].thresholds.size - 1
    }
    private val thresholdCounts = IntArray(model.variables.size) {
        model.variables[it].thresholds.size
    }
    private val thresholdMultipliers = IntArray(model.variables.size)

    val stateCount: Int = run {
        var stateCount = 1L
        for (v in dimensionMultipliers.indices) {
            dimensionMultipliers[v] = stateCount.toInt()
            stateCount *= dimensionStateCounts[v]
        }
        if (stateCount > Int.MAX_VALUE) {
            throw IllegalArgumentException("Model is too big for integer encoding!")
        }
        stateCount.toInt()
    }

    val vertexCount: Int = run {
        var vertexCount = 1L
        for (v in thresholdMultipliers.indices) {
            thresholdMultipliers[v] = vertexCount.toInt()
            vertexCount *= thresholdCounts[v]
        }
        if (vertexCount > Int.MAX_VALUE) {
            throw IllegalArgumentException("Model is too big for integer encoding!")
        }
        vertexCount.toInt()
    }

    /**
     * Encode given coordinate array into a single number.
     */
    fun encodeNode(coordinates: IntArray): IDNode {
        return IDNode(coordinates.foldIndexed(0) { i, acc, e ->
            acc + dimensionMultipliers[i] * e
        })
    }

    /**
     * Decode given node into array of it's coordinates.
     */
    fun decodeNode(node: IDNode): IntArray {
        return IntArray(dimensionMultipliers.size) { i ->
            (node.id / dimensionMultipliers[i]) % dimensionStateCounts[i]
        }
    }

    fun encodeVertex(coordinates: IntArray): Int {
        return coordinates.foldIndexed(0) { i, acc, e ->
            acc + thresholdMultipliers[i] * e
        }
    }

    fun decodeVertex(vertex: Int): IntArray {
        return IntArray(thresholdMultipliers.size) { i ->
            (vertex / thresholdMultipliers[i]) % thresholdCounts[i]
        }
    }

    /**
     * Find an id node that is above given node in specified dimension.
     * Return null if such node is not in the model.
     */
    fun higherNode(from: IDNode, dim: Int): IDNode? {
        val coordinate = (from.id / dimensionMultipliers[dim]) % dimensionStateCounts[dim]
        if (coordinate == dimensionStateCounts[dim] - 1) return null
        else return IDNode(from.id + dimensionMultipliers[dim])
    }

    /**
     * Find an id node that is below given node in specified dimension.
     * Return null if such node is not in the model.
     */
    fun lowerNode(from: IDNode, dim: Int): IDNode? {
        val coordinate = (from.id / dimensionMultipliers[dim]) % dimensionStateCounts[dim]
        if (coordinate == 0) return null
        else return IDNode(from.id - dimensionMultipliers[dim])
    }

    /**
     * Return index of upper threshold in specified dimension
     */
    fun upperThreshold(of: IDNode, dim: Int): Int {
        return (of.id / dimensionMultipliers[dim]) % dimensionStateCounts[dim] + 1
    }

    /**
     * Return index of lower threshold in specified dimension
     */
    fun lowerThreshold(of: IDNode, dim: Int): Int {
        return (of.id / dimensionMultipliers[dim]) % dimensionStateCounts[dim]
    }

    fun threshold(of: IDNode, dim: Int, upper: Boolean): Int {
        return (of.id / dimensionMultipliers[dim]) % dimensionStateCounts[dim] + if (upper) 1 else 0
    }

    fun coordinate(of: IDNode, dim: Int): Int = (of.id / dimensionMultipliers[dim]) % dimensionStateCounts[dim]

}