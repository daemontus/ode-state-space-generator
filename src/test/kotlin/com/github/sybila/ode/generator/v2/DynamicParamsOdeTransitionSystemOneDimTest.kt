package com.github.sybila.ode.generator.v2

import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.generator.rect.rectangleOf
import com.github.sybila.ode.generator.v2.dynamic.DynamicParamsOdeTransitionSystem
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test

class DynamicParamsOdeTransitionSystemOneDimTest {
    //dv1 = p(v1/2 + 1) - 1
    //This one dimensional model should actually cover most of the behaviour
    //It only fails to cover a steady state in the middle of the model and
    //cases when parameter is multiplied by zero

    //dv2 = p(v1/2 - 2) - 1
    //This model covers the two remaining cases. A stable state and a zero on threshold.

    private val v1 = OdeModel.Variable(
            name = "v1", range = Pair(0.0, 6.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 4.0, 6.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0), constant = 0.5),
                    Summand(paramIndex = 0),
                    Summand(constant = -1.0))
    )

    private val v2 = v1.copy(name = "v1",
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0), constant = 0.5),
                    Summand(paramIndex = 0, constant = -2.0),
                    Summand(constant = -1.0)))

    private val fragmentOne = DynamicParamsOdeTransitionSystem(OdeModel(listOf(v1), listOf(
            OdeModel.Parameter("p1", Pair(0.0, 2.0))
    )), "C:\\Users\\Jakub\\Desktop\\ode-generator\\build\\libs\\ode-generator-1.3.3-2-all.jar")

    private val fragmentTwo = DynamicParamsOdeTransitionSystem(OdeModel(listOf(v2), listOf(
            OdeModel.Parameter("p2", Pair(-2.0, 2.0))
    )), "C:\\Users\\Jakub\\Desktop\\ode-generator\\build\\libs\\ode-generator-1.3.3-2-all.jar")

    private fun DynamicParamsOdeTransitionSystem.checkSuccessors(from: Int, bounds: Map<Int, MutableSet<Rectangle>>) {
        val states = from.successors()
        assert(states.size == bounds.size)

        for (state in states) {
            assert(bounds.containsKey(state))
            assert(bounds[state] == transitionParameters(from, state))

        }

    }

    private fun DynamicParamsOdeTransitionSystem.checkPredecessors(from: Int, bounds: Map<Int, MutableSet<Rectangle>>) {
        val states = from.predecessors()
        assert(states.size == bounds.size)

        for (state in states) {
            assert(bounds.containsKey(state))
            assert(bounds[state] == transitionParameters(state, from))

        }

    }


    @Test
    fun parameterTestOne() {
        fragmentOne.run {
            checkSuccessors(0, mapOf(
                    0 to rectangleOf(0.0, 1.0).asParams(),
                    1 to rectangleOf(1.0 / 2.0, 2.0).asParams()
            ))

            checkSuccessors(1, mapOf(
                    0 to rectangleOf(0.0, 1.0 / 2.0).asParams(),
                    1 to rectangleOf(1.0 / 3.0, 1.0 / 2.0).asParams(),
                    2 to rectangleOf(1.0 / 3.0, 2.0).asParams()
            ))

            checkSuccessors(2, mapOf(
                    1 to rectangleOf(0.0, 1.0 / 3.0).asParams(),
                    2 to rectangleOf(1.0 / 4.0, 2.0).asParams()
            ))

            checkPredecessors(0, mapOf(
                    0 to rectangleOf(0.0, 1.0).asParams(),
                    1 to rectangleOf(0.0, 1.0 / 2.0).asParams()
            ))

            checkPredecessors(1, mapOf(
                    0 to rectangleOf(1.0 / 2.0, 2.0).asParams(),
                    1 to rectangleOf(1.0 / 3.0, 1.0 / 2.0).asParams(),
                    2 to rectangleOf(0.0, 1.0 / 3.0).asParams()
            ))

            checkPredecessors(2, mapOf(
                    1 to rectangleOf(1.0 / 3.0, 2.0).asParams(),
                    2 to rectangleOf(1.0 / 4.0, 2.0).asParams()
            ))


        }
    }

    @Test
    fun parameterTestTwo() {
        fragmentTwo.run {
            checkSuccessors(0, mapOf(
                    0 to rectangleOf(-1.0, 2.0).asParams(),
                    1 to rectangleOf(-2.0, -1.0).asParams()
            ))

            checkSuccessors(1, mapOf(
                    0 to rectangleOf(-1.0, 2.0).asParams(),
                    1 to rectangleOf(-2.0, -1.0).asParams()
            ))

            checkSuccessors(2, mapOf(
                    1 to rectangleOf(-2.0, 2.0).asParams(),
                    2 to rectangleOf(1.0, 2.0).asParams()
            ))

            checkPredecessors(0, mapOf(
                    0 to rectangleOf(-1.0, 2.0).asParams(),
                    1 to rectangleOf(-1.0, 2.0).asParams()
            ))

            checkPredecessors(1, mapOf(
                    0 to rectangleOf(-2.0, -1.0).asParams(),
                    1 to rectangleOf(-2.0, -1.0).asParams(),
                    2 to rectangleOf(-2.0, 2.0).asParams()
            ))

            checkPredecessors(2, mapOf(
                    2 to rectangleOf(1.0, 2.0).asParams()
            ))
        }
    }
}