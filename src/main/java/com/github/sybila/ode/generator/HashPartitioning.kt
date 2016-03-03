package com.github.sybila.ode.generator

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.PartitionFunction


/**
 * This should provide a reasonably random partitioning of nodes between workers.
 * The amount of cross edges will be naturally high, on the other hand, the
 * workload should have the best possible distribution.
 */
class HashPartitioning(
        override val myId: Int,
        private val workerCount: Int,
        private val encoder: NodeEncoder
) : PartitionFunction<IDNode> {

    private val partitionCache = IntArray(encoder.stateCount) { -1 }

    override val ownerId: IDNode.() -> Int = {
        val p = partitionCache[this.id]
        if (p != -1) {
            p
        } else {
            var sum = 0
            for (d in 0 until encoder.dimensions) {
                sum += 31 * encoder.coordinate(this, d)
            }
            val newP = sum % workerCount
            partitionCache[this.id] = newP
            newP
        }
    }

}