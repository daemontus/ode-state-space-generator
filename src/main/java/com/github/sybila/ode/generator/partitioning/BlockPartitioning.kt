package com.github.sybila.ode.generator.partitioning

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.PartitionFunction
import com.github.sybila.ode.generator.NodeEncoder


/**
 * This type of partitioning should create a checkerboard pattern, so that the state space is
 * partitioned into equally sized blocks (size is a constructor parameter) and the blocks are
 * assigned to workers in regular manner.
 * Imagine 3 workers - the block assignment will then be following:
 * 1 2 3 1 2 3 ...
 * 2 3 1 2 3 1
 * 3 1 2 3 1 2
 * ...
 *
 * The efficiency of such partitioning depends a lot on on the block size. Too big and you basically end up with
 * slice partitioning, too small and you have a not very good inefficient hash.
 */
class BlockPartitioning(
        override val myId: Int,
        private val workerCount: Int,
        private val blockSize: Int,
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
                sum += encoder.coordinate(this, d) / blockSize
            }
            val newP = sum % workerCount
            partitionCache[this.id] = newP
            newP
        }
    }
}