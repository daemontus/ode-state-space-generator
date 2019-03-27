package com.github.sybila.ode.generator.v2

interface TransitionSystem<State: Any, Param : Any> {

    fun State.successors(): List<State>
    fun State.predecessors(): List<State>

    fun transitionParameters(source: State, target: State): Param

}