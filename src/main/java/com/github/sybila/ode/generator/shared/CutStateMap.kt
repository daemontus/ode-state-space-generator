package com.github.sybila.ode.generator.shared

import com.github.sybila.checker.shared.Params
import com.github.sybila.checker.shared.StateMap
import com.github.sybila.ode.generator.NodeEncoder


class CutStateMap(
        private val encoder: NodeEncoder,
        private val dimension: Int,
        private val threshold: Int,
        private val gt: Boolean,
        private val stateCount: Int,
        private val value: Params
        ) : StateMap {

    override val states: Sequence<Int> = (0 until stateCount).asSequence().filter { it in this }
    override val entries: Sequence<Pair<Int, Params>> = states.map { it to value }

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

    override fun get(state: Int): Params? {
        return if (state in this) value else null
    }

}