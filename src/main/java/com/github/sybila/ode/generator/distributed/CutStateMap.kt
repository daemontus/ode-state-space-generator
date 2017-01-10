package com.github.sybila.ode.generator.distributed

import com.github.sybila.checker.distributed.StateMap
import com.github.sybila.ode.generator.NodeEncoder

class CutStateMap<out Params : Any>(
        private val encoder: NodeEncoder,
        private val dimension: Int,
        private val threshold: Int,
        private val gt: Boolean,
        private val stateCount: Int,
        override val sizeHint: Int,
        private val value: Params,
        private val default: Params
) : StateMap<Params> {

    override fun contains(state: Int): Boolean {
        val dimensionIndex = encoder.coordinate(state, dimension)
        val lowThreshold = dimensionIndex
        val highThreshold = dimensionIndex + 1
        return if (gt) {
            lowThreshold >= threshold
        } else {
            highThreshold <= threshold
        }
    }

    override fun get(state: Int): Params {
        return if (state in this) value else default
    }

    override fun entries(): Iterator<Pair<Int, Params>> = (0 until stateCount).asSequence()
            .filter { it in this }.map { it to get(it) }.iterator()

    override fun states(): Iterator<Int> = (0 until stateCount).asSequence()
            .filter { it in this }.iterator()

}