package com.github.sybila.ode.generator.shared

import com.github.sybila.checker.shared.Params
import com.github.sybila.checker.shared.StateMap


class LazyStateMap(
        val stateCount: Int,
        val test: (Int) -> Params?
) : StateMap {

    override val entries: Sequence<Pair<Int, Params>> = (0 until stateCount).asSequence()
            .map { state -> test(state)?.let { state to it } }.filterNotNull()
    override val states: Sequence<Int> = (0 until stateCount).asSequence()
            .filter { it in this }

    override fun contains(state: Int): Boolean = test(state) != null

    override fun get(state: Int): Params? = test(state)

}