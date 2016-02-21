package cz.muni.fi.ode.generator

import cz.muni.fi.checker.IDNode
import cz.muni.fi.ode.model.Model

class NodeEncoder(
        private val model: Model
) {

    private val dimensionMultipliers = IntArray(model.variables.size)
    private val dimensionStateCounts = IntArray(model.variables.size) {
        model.variables[it].thresholds.size - 1
    }

    init {
        var stateCount = 1L
        for (v in dimensionMultipliers.indices) {
            dimensionMultipliers[v] = stateCount.toInt()
            stateCount *= dimensionStateCounts[v]
        }
        if (stateCount > Int.MAX_VALUE) {
            throw IllegalArgumentException("Model is too big for integer encoding!")
        }
    }

    /**
     * Encode given coordinate array into a single number.
     */
    fun encode(coordinates: IntArray): IDNode {
        return IDNode(coordinates.foldIndexed(0) { i, acc, e ->
            acc + dimensionMultipliers[i] * e
        })
    }

    /**
     * Decode given node into array of it's coordinates.
     */
    fun decode(node: IDNode): IntArray {
        return IntArray(dimensionMultipliers.size) { i ->
            (node.id / dimensionMultipliers[i]) % dimensionStateCounts[i]
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

}