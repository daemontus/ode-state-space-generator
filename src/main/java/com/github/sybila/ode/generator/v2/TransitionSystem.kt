package com.github.sybila.ode.generator.v2

/**
 * Generic interface representing a transition system.
 */
interface TransitionSystem<State: Any, Param : Any> {

    /**
     * Returns a list of successors for the input state.
     */
    fun State.successors(): List<State>

    /**
     * Returns a list of predecessors for the input state.
     */
    fun State.predecessors(): List<State>


    /**
     * Returns parameters for which the transition between the source and target states is possible.
     */
    fun transitionParameters(source: State, target: State): Param

}