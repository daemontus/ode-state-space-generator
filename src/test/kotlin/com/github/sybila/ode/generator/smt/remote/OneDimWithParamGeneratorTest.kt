package com.github.sybila.ode.generator.smt.remote

import com.github.sybila.checker.Transition
import com.github.sybila.checker.decreaseProp
import com.github.sybila.checker.increaseProp
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.ode.assertTransitionEquals
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test


class OneDimWithParamGeneratorTest {

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

    private val loop = DirectionFormula.Atom.Loop
    private val up = "v1".increaseProp()
    private val down = "v1".decreaseProp()

    /**
     *  WARNING - Because we don't know how to compare <= and < properly, some answers have
     *  non-strict operators that don't make that much sense, but are necessary
     */

    @Test
    fun parameterTestOne() {
        val fragmentOne = Z3OdeFragment(OdeModel(listOf(v1), listOf(
                OdeModel.Parameter("p", Pair(0.0, 2.0))
        )))
        fragmentOne.run {
            val p = "p"
            val one = "1"
            val two = "2"
            val three = "3"
            val four = "4"
            val half = one div two
            val third = one div three
            val fourth = one div four
            val thirdApprox = "0.3333333333"
            //bounds are already in the solver
            assertTransitionEquals(0.successors(true),
                    Transition(0, loop, (p le one).toParams()),
                    Transition(1, up, (p gt half).toParams())
            )
            assertTransitionEquals(1.successors(true),
                    Transition(0, down, ((p lt half).toParams())),
                    Transition(1, loop, ((p ge thirdApprox) and (p le half)).toParams()),
                    Transition(2, up, (p gt thirdApprox).toParams())
            )
            assertTransitionEquals(2.successors(true),
                    Transition(1, down, (p lt thirdApprox).toParams()),
                    Transition(2, loop, (p ge fourth).toParams())
            )
            assertTransitionEquals(0.predecessors(true),
                    Transition(0, loop, (p le one).toParams()),
                    Transition(1, down, (p lt half).toParams())
            )
            assertTransitionEquals(1.predecessors(true),
                    Transition(0, up, ((p gt half).toParams())),
                    Transition(1, loop, ((p ge thirdApprox) and (p le half)).toParams()),
                    Transition(2, down, (p lt thirdApprox).toParams())
            )
            assertTransitionEquals(2.predecessors(true),
                    Transition(1, up, (p gt thirdApprox).toParams()),
                    Transition(2, loop, (p ge fourth).toParams())
            )
        }
    }

    @Test
    fun parameterTestTwo() {
        val fragmentTwo = Z3OdeFragment(OdeModel(listOf(v2), listOf(
                OdeModel.Parameter("p", Pair(-2.0, 2.0))
        )))
        //dv2 = p(v1 - 2) - 1
        //(0) dv2 = p(-2) - 1 p>-1/2 => - // p < -1/2 => +
        //(1) dv2 = p(-1) - 1 p>-1 => - // p < -1 => +
        //(2) dv2 = p(0) - 1 // -1
        //(3) dv2 = p(1) - 1  p<1 => - // p > 1 => +
        fragmentTwo.run {
            val q = "p"
            val one = "1"
            val mOne = "-1"
            assertTransitionEquals(0.successors(true),
                    Transition(0, loop, (q ge mOne).toParams()),
                    Transition(1, up, (q lt mOne).toParams())
            )
            assertTransitionEquals(1.successors(true),
                    Transition(0, down, (q gt mOne).toParams()),
                    Transition(1, loop, (q le mOne).toParams())
            )
            assertTransitionEquals(2.successors(true),
                    Transition(1, down, tt),
                    Transition(2, loop, (q ge one).toParams())
            )
            assertTransitionEquals(0.predecessors(true),
                    Transition(0, loop, (q ge mOne).toParams()),
                    Transition(1, down, (q gt mOne).toParams())
            )
            assertTransitionEquals(1.predecessors(true),
                    Transition(0, up, (q lt mOne).toParams()),
                    Transition(1, loop, (q le mOne).toParams()),
                    Transition(2, down, tt)
            )
            assertTransitionEquals(2.predecessors(true),
                    Transition(2, loop, (q ge one).toParams())
            )
        }
    }

}
