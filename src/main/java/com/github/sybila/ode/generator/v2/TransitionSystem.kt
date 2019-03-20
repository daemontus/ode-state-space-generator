package com.github.sybila.ode.generator.v2

interface TransitionSystem<State: Any, Param : Any> {

    fun Int.successors(): List<State>
    fun Int.predecessors(): List<State>

    fun transitionParameters(source: State, target: State): Param

}