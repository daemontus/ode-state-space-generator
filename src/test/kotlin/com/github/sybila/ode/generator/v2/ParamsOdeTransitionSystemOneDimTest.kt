package com.github.sybila.ode.generator.v2

import com.github.sybila.checker.Transition
import com.github.sybila.checker.decreaseProp
import com.github.sybila.checker.increaseProp
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.ode.assertTransitionEquals
import com.github.sybila.ode.generator.rect.RectangleOdeModel
import com.github.sybila.ode.generator.rect.rectangleOf
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test

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

    @Test
    fun parameterTestOne() {
        fragmentOne.run {
            assertTransitionEquals(0.successors(),
                    Transition(0, loop, rectangleOf(0.0, 1.0).asParams()),
                    Transition(1, up, rectangleOf(1.0 / 2.0, 2.0).asParams())
            )
            assertTransitionEquals(1.successors(),
                    Transition(0, down, rectangleOf(0.0, 1.0 / 2.0).asParams()),
                    Transition(1, loop, rectangleOf(1.0 / 3.0, 1.0 / 2.0).asParams()),
                    Transition(2, up, rectangleOf(1.0 / 3.0, 2.0).asParams())
            )
            assertTransitionEquals(2.successors(),
                    Transition(1, down, rectangleOf(0.0, 1.0 / 3.0).asParams()),
                    Transition(2, loop, rectangleOf(1.0 / 4.0, 2.0).asParams())
            )
            assertTransitionEquals(0.predecessors(),
                    Transition(0, loop, rectangleOf(0.0, 1.0).asParams()),
                    Transition(1, down, rectangleOf(0.0, 1.0 / 2.0).asParams())
            )
            assertTransitionEquals(1.predecessors(),
                    Transition(0, up, rectangleOf(1.0 / 2.0, 2.0).asParams()),
                    Transition(1, loop, rectangleOf(1.0 / 3.0, 1.0 / 2.0).asParams()),
                    Transition(2, down, rectangleOf(0.0, 1.0 / 3.0).asParams())
            )
            assertTransitionEquals(2.predecessors(),
                    Transition(1, up, rectangleOf(1.0 / 3.0, 2.0).asParams()),
                    Transition(2, loop, rectangleOf(1.0 / 4.0, 2.0).asParams())
            )
        }
    }

    @Test
    fun parameterTestThree() {
        fragmentOne.run {
            successors()
        }
    }

    @Test
    fun parameterTestTwo() {
        //dv2 = p(v1 - 2) - 1
        //(0) dv2 = p(-2) - 1 p>-1/2 => - // p < -1/2 => +
        //(1) dv2 = p(-1) - 1 p>-1 => - // p < -1 => +
        //(2) dv2 = p(0) - 1 // -1
        //(3) dv2 = p(1) - 1  p<1 => - // p > 1 => +
        fragmentTwo.run {
            assertTransitionEquals(0.successors(),
                    Transition(0, loop, rectangleOf(-1.0, 2.0).asParams()),
                    Transition(1, up, rectangleOf(-2.0, -1.0).asParams())
            )
            assertTransitionEquals(1.successors(),
                    Transition(0, down, rectangleOf(-1.0, 2.0).asParams()),
                    Transition(1, loop, rectangleOf(-2.0, -1.0).asParams())
            )
            assertTransitionEquals(2.successors(),
                    Transition(1, down, rectangleOf(-2.0, 2.0).asParams()),
                    Transition(2, loop, rectangleOf(1.0, 2.0).asParams())
            )
            assertTransitionEquals(0.predecessors(),
                    Transition(0, loop, rectangleOf(-1.0, 2.0).asParams()),
                    Transition(1, down, rectangleOf(-1.0, 2.0).asParams())
            )
            assertTransitionEquals(1.predecessors(),
                    Transition(0, up, rectangleOf(-2.0, -1.0).asParams()),
                    Transition(1, loop, rectangleOf(-2.0, -1.0).asParams()),
                    Transition(2, down, rectangleOf(-2.0, 2.0).asParams())
            )
            assertTransitionEquals(2.predecessors(),
                    Transition(2, loop, rectangleOf(1.0, 2.0).asParams())
            )
        }
    }
}