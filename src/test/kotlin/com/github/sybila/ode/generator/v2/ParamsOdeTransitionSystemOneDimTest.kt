package com.github.sybila.ode.generator.v2

import com.github.sybila.checker.Transition
import com.github.sybila.checker.decreaseProp
import com.github.sybila.checker.increaseProp
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.ode.assertDeepEquals
import com.github.sybila.ode.assertTransitionEquals
import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.generator.rect.rectangleOf
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertEquals

class ParamsOdeTransitionSystemOneDimTest {
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

    private val fragmentOne = ParamsOdeTransitionSystem(OdeModel(listOf(v1), listOf(
            OdeModel.Parameter("p1", Pair(0.0, 2.0))
    )))

    private val fragmentTwo = ParamsOdeTransitionSystem(OdeModel(listOf(v2), listOf(
            OdeModel.Parameter("p2", Pair(-2.0, 2.0))
    )))

    val up = "v1".increaseProp()
    val down = "v1".decreaseProp()
    val loop = DirectionFormula.Atom.Loop

    private fun ParamsOdeTransitionSystem.checkSuccessors(from: Int, to: List<Int>) {
        this.run {
            val s = from.successors().asSequence().toSet()
            assertEquals(to.toSet(), s)
        }
    }

    private fun ParamsOdeTransitionSystem.checkPredecessors(from: Int, to: List<Int>) {
        this.run {
            val s = from.predecessors().asSequence().toSet()
            assertEquals(to.toSet(), s)
        }
    }

    private fun ParamsOdeTransitionSystem.checkTransitions(from: Int, bounds: Map<Int, MutableSet<Rectangle>>) {
        val states = from.successors()
        assert(states.size == bounds.size)

        for (state in states) {
            assert(bounds.containsKey(state))
            assert(bounds[state] == transitionParameters(from, state))

        }

    }

    @Test
    fun parameterTestOne() {
        fragmentOne.run {
            checkTransitions(0, mapOf(
                    0 to rectangleOf(0.0, 1.0).asParams(),
                    1 to rectangleOf(1.0 / 2.0, 2.0).asParams()
            ))

            checkTransitions(1, mapOf(
                    0 to rectangleOf(0.0, 1.0 / 2.0).asParams(),
                    1 to rectangleOf(1.0 / 3.0, 1.0 / 2.0).asParams(),
                    2 to rectangleOf(1.0 / 3.0, 2.0).asParams()
            ))

            checkTransitions(2, mapOf(
                    1 to rectangleOf(0.0, 1.0 / 3.0).asParams(),
                    2 to rectangleOf(1.0 / 4.0, 2.0).asParams()
            ))
        }
    }

    /*
    @Test
    fun parameterTestOne() {
        fragmentOne.run {
            solver.assertTransitionEquals(0.successors().iterator(),
                    Transition(0, loop, rectangleOf(0.0, 1.0).asParams()),
                    Transition(1, up, rectangleOf(1.0 / 2.0, 2.0).asParams())
            )
            solver.assertTransitionEquals(1.successors().iterator(),
                    Transition(0, down, rectangleOf(0.0, 1.0 / 2.0).asParams()),
                    Transition(1, loop, rectangleOf(1.0 / 3.0, 1.0 / 2.0).asParams()),
                    Transition(2, up, rectangleOf(1.0 / 3.0, 2.0).asParams())
            )
            solver.assertTransitionEquals(2.successors().iterator(),
                    Transition(1, down, rectangleOf(0.0, 1.0 / 3.0).asParams()),
                    Transition(2, loop, rectangleOf(1.0 / 4.0, 2.0).asParams())
            )
            solver.assertTransitionEquals(0.predecessors().iterator(),
                    Transition(0, loop, rectangleOf(0.0, 1.0).asParams()),
                    Transition(1, down, rectangleOf(0.0, 1.0 / 2.0).asParams())
            )
            solver.assertTransitionEquals(1.predecessors().iterator(),
                    Transition(0, up, rectangleOf(1.0 / 2.0, 2.0).asParams()),
                    Transition(1, loop, rectangleOf(1.0 / 3.0, 1.0 / 2.0).asParams()),
                    Transition(2, down, rectangleOf(0.0, 1.0 / 3.0).asParams())
            )
            solver.assertTransitionEquals(2.predecessors().iterator(),
                    Transition(1, up, rectangleOf(1.0 / 3.0, 2.0).asParams()),
                    Transition(2, loop, rectangleOf(1.0 / 4.0, 2.0).asParams())
            )
        }
    }
    */

    @Test
    fun parameterTestTwo() {
        fragmentTwo.run {
            checkTransitions(0, mapOf(
                    0 to rectangleOf(-1.0, 2.0).asParams(),
                    1 to rectangleOf(-2.0, -1.0).asParams()
            ))

            checkTransitions(1, mapOf(
                    0 to rectangleOf(-1.0, 2.0).asParams(),
                    1 to rectangleOf(-2.0, -1.0).asParams()
            ))

            checkTransitions(2, mapOf(
                    1 to rectangleOf(-2.0, 2.0).asParams(),
                    2 to rectangleOf(1.0, 2.0).asParams()
            ))
        }
    }

    /*
    @Test
    fun parameterTestTwo() {
        //dv2 = p(v1 - 2) - 1
        //(0) dv2 = p(-2) - 1 p>-1/2 => - // p < -1/2 => +
        //(1) dv2 = p(-1) - 1 p>-1 => - // p < -1 => +
        //(2) dv2 = p(0) - 1 // -1
        //(3) dv2 = p(1) - 1  p<1 => - // p > 1 => +
        fragmentTwo.run {
            solver.assertTransitionEquals(0.successors().iterator(),
                    Transition(0, loop, rectangleOf(-1.0, 2.0).asParams()),
                    Transition(1, up, rectangleOf(-2.0, -1.0).asParams())
            )
            solver.assertTransitionEquals(1.successors().iterator(),
                    Transition(0, down, rectangleOf(-1.0, 2.0).asParams()),
                    Transition(1, loop, rectangleOf(-2.0, -1.0).asParams())
            )
            solver.assertTransitionEquals(2.successors().iterator(),
                    Transition(1, down, rectangleOf(-2.0, 2.0).asParams()),
                    Transition(2, loop, rectangleOf(1.0, 2.0).asParams())
            )
            solver.assertTransitionEquals(0.predecessors().iterator(),
                    Transition(0, loop, rectangleOf(-1.0, 2.0).asParams()),
                    Transition(1, down, rectangleOf(-1.0, 2.0).asParams())
            )
            solver.assertTransitionEquals(1.predecessors().iterator(),
                    Transition(0, up, rectangleOf(-2.0, -1.0).asParams()),
                    Transition(1, loop, rectangleOf(-2.0, -1.0).asParams()),
                    Transition(2, down, rectangleOf(-2.0, 2.0).asParams())
            )
            solver.assertTransitionEquals(2.predecessors().iterator(),
                    Transition(2, loop, rectangleOf(1.0, 2.0).asParams())
            )
        }
    }
    */
}