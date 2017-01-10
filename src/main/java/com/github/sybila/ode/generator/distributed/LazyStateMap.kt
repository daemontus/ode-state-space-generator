package com.github.sybila.ode.generator.distributed

import com.github.sybila.checker.distributed.StateMap

class LazyStateMap<out Params : Any>(
        val stateCount: Int,
        val default: Params,
        val test: (Int) -> Params?
) : StateMap<Params> {

    override val sizeHint: Int = stateCount

    override fun contains(state: Int): Boolean = test(state) != null

    override fun entries(): Iterator<Pair<Int, Params>> = (0 until stateCount).asSequence()
            .filter { it in this }.map { it to test(it)!! }.iterator()

    override fun get(state: Int): Params = test(state) ?: default

    override fun states(): Iterator<Int> = (0 until stateCount).asSequence().filter { it in this }.iterator()

}