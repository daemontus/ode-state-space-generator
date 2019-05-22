package com.github.sybila.ode.generator

import com.github.sybila.ode.model.OdeModel

class NodeEncoder(
        private val model: OdeModel
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
            throw IllegalArgumentException("OdeModel is too big for integer encoding!")
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
            throw IllegalArgumentException("OdeModel is too big for integer encoding!")
        }
        vertexCount.toInt()
    }

    val dimensions = model.variables.size

    /**
     * Encode given coordinate array into a single number.
     */
    fun encodeNode(coordinates: IntArray): Int {
        return coordinates.foldIndexed(0) { i, acc, e ->
            acc + dimensionMultipliers[i] * e
        }
    }

    /**
     * Decode given node into array of it's coordinates.
     */
    fun decodeNode(node: Int): IntArray {
        return IntArray(dimensionMultipliers.size) { i ->
            (node / dimensionMultipliers[i]) % dimensionStateCounts[i]
        }
    }

    fun encodeVertex(coordinates: IntArray): Int {
        return coordinates.foldIndexed(0) { i, acc, e ->
            acc + thresholdMultipliers[i] * e
        }
    }

    fun decodeVertex(vertex: Int): IntArray {
        return IntArray(thresholdMultipliers.size) { i ->
            (vertex / thresholdMultipliers[i]) % thresholdMultipliers[i]
        }
    }

    fun vertexCoordinate(vertex: Int, dim: Int): Int {
        return (vertex / thresholdMultipliers[dim]) % thresholdCounts[dim]
    }

    fun nodeVertex(node: Int, vertexMask: Int): Int {
        var acc = 0
        var d = 0
        while (d < dimensions) {
            val newCoordinate = ((node / dimensionMultipliers[d]) % dimensionStateCounts[d]) + vertexMask.shr(d).and(1)
            acc += thresholdMultipliers[d] + newCoordinate
            d += 1
        }
        return acc
        /*return (0 until dimensions).asSequence().map { dim ->
            //compute vertex coordinates
            coordinate(node, dim) + vertexMask.shr(dim).and(1)
        }.foldIndexed(0) { i, acc, e ->
            //transform to ID
            acc + thresholdMultipliers[i] * e
        }*/
    }

    /**
     * Find an id node that is above given node in specified dimension.
     * Return null if such node is not in the model.
     */
    fun higherNode(from: Int, dim: Int): Int? {
        val coordinate = (from / dimensionMultipliers[dim]) % dimensionStateCounts[dim]
        return if (coordinate == dimensionStateCounts[dim] - 1) null else from + dimensionMultipliers[dim]
    }

    /**
     * Find an id node that is below given node in specified dimension.
     * Return null if such node is not in the model.
     */
    fun lowerNode(from: Int, dim: Int): Int? {
        val coordinate = (from / dimensionMultipliers[dim]) % dimensionStateCounts[dim]
        return if (coordinate == 0) null else from - dimensionMultipliers[dim]
    }

    /**
     * Return index of upper threshold in specified dimension
     */
    fun upperThreshold(of: Int, dim: Int): Int {
        return (of / dimensionMultipliers[dim]) % dimensionStateCounts[dim] + 1
    }

    /**
     * Return index of lower threshold in specified dimension
     */
    fun lowerThreshold(of: Int, dim: Int): Int {
        return (of / dimensionMultipliers[dim]) % dimensionStateCounts[dim]
    }

    fun threshold(of: Int, dim: Int, upper: Boolean): Int {
        return (of / dimensionMultipliers[dim]) % dimensionStateCounts[dim] + if (upper) 1 else 0
    }

    fun coordinate(of: Int, dim: Int): Int = (of / dimensionMultipliers[dim]) % dimensionStateCounts[dim]

}