package com.github.sybila.ode.generator

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.PartitionFunction


/**
 * This should partition the state space into equally sized "slices" across the highest dimension.
 *
 * It provides one of the smallest cross edge counts, however it doesn't provide a very good
 * load balancing if the query isn't "spanning" across the whole state space.
 */
class SlicePartitioning(
        override val myId: Int,
        private val workerCount: Int,
        private val encoder: NodeEncoder
) : PartitionFunction<IDNode> {

    private val statesPerMachine = Math.ceil(encoder.stateCount.toDouble() / workerCount.toDouble()).toInt();

    override val ownerId: IDNode.() -> Int = {
        this.id / statesPerMachine
    }

}
