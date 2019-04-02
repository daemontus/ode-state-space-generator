package com.github.sybila.ode.generator.v2

import com.github.sybila.checker.Transition
import com.github.sybila.checker.decreaseProp
import com.github.sybila.checker.increaseProp
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.ode.assertTransitionEquals
import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.generator.rect.RectangleOdeModel
import com.github.sybila.ode.generator.rect.rectangleOf
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test

class ParamsOdeTransitionSystemTwoDimTest {
    //We don't test without parameters on two dimensions, because
    //there are already too many options.
    //With parameters, we can encode these options into only few models.

    //OdeModel 1:
    //A simple model based on one dimensional cases.
    //dv1 = p1(v1 + 1) - 1
    //dv2 = p2(v2 + 1) - 1

    //OdeModel 2:
    //More complicated model where variable actually depend on each other.
    //It also contains a nice spiral :)
    //dv1 = p1(v1 + v2 + 1) - 2.5
    //dv2 = p2(v2 - v1 + 1) - 2.5

    private val v1 = OdeModel.Variable(name = "v1", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0)),
                    Summand(paramIndex = 0), Summand(constant = -1.0)
            ))

    private val v2 = OdeModel.Variable(name = "v2", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            equation = listOf(
                    Summand(paramIndex = 1, variableIndices = listOf(1)),
                    Summand(paramIndex = 1), Summand(constant = -1.0)
            ))

    private val v3 = v1.copy(name = "v1", equation = listOf(
            Summand(paramIndex = 0, variableIndices = listOf(0)),
            Summand(paramIndex = 0, variableIndices = listOf(1)),
            Summand(paramIndex = 0), Summand(constant = -2.5)
    ))

    private val v4 = v1.copy(name = "v2", equation = listOf(
            Summand(paramIndex = 1, variableIndices = listOf(0), constant = -1.0),
            Summand(paramIndex = 1, variableIndices = listOf(1)),
            Summand(paramIndex = 1), Summand(constant = -2.5)
    ))

    private val fragmentOne =ParamsOdeTransitionSystem(OdeModel(listOf(v1, v2), listOf(
            OdeModel.Parameter(name = "p1", range = Pair(0.0, 2.0)),
            OdeModel.Parameter(name = "p2", range = Pair(0.0, 2.0))
    )))

    private val fragmentTwo = ParamsOdeTransitionSystem(OdeModel(listOf(v3, v4), listOf(
            OdeModel.Parameter(name = "p1", range = Pair(0.0, 2.0)),
            OdeModel.Parameter(name = "p2", range = Pair(0.0, 2.0))
    )))

    val n00 = 0
    val n01 = 1
    val n02 = 2
    val n10 = 3
    val n11 = 4
    val n12 = 5
    val n20 = 6
    val n21 = 7
    val n22 = 8

    val loop = DirectionFormula.Atom.Loop
    val v1Up = "v1".increaseProp()
    val v1Down = "v1".decreaseProp()
    val v2Up = "v2".increaseProp()
    val v2Down = "v2".decreaseProp()

    private fun singleRectangle(vararg values: Double): MutableSet<Rectangle>
            = mutableSetOf(rectangleOf(*values))

    private fun checkPredecessors(transitions: Map<Pair<Int, Int>, MutableSet<Rectangle>>) {
        TODO("not implemented")
    }

    private fun checkSuccessors(transitions: Map<Pair<Int, Int>, MutableSet<Rectangle>>) {
        TODO("not implemented")
    }


    @Test
    fun fragmentOneTest() {
        val transitions: Map<Pair<Int, Int>, MutableSet<Rectangle>> = mapOf(
                n00 to n00 to singleRectangle(0.0, 1.0, 0.0, 1.0),
                n01 to n01 to singleRectangle(1.0 / 3.0, 1.0 / 2.0, 0.0, 1.0),
                n02 to n02 to singleRectangle(1.0 / 4.0, 2.0, 0.0, 1.0),
                n10 to n10 to singleRectangle(0.0, 1.0, 1.0 / 3.0, 1.0 / 2.0),
                n11 to n11 to singleRectangle(1.0 / 3.0, 1.0 / 2.0, 1.0 / 3.0, 1.0 / 2.0),
                n12 to n12 to singleRectangle(1.0 / 4.0, 2.0, 1.0 / 3.0, 1.0 / 2.0),
                n20 to n20 to singleRectangle(0.0, 1.0, 1.0 / 4.0, 2.0),
                n21 to n21 to singleRectangle(1.0 / 3.0, 1.0 / 2.0, 1.0 / 4.0, 2.0),
                n22 to n22 to singleRectangle(1.0 / 4.0, 2.0, 1.0 / 4.0, 2.0),
                n00 to n01 to singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0),
                n00 to n10 to singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0),
                n10 to n11 to singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0),
                n20 to n21 to singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0),
                n01 to n11 to singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0),
                n02 to n12 to singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0),
                n10 to n00 to singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0),
                n11 to n01 to singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0),
                n12 to n02 to singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0),
                n01 to n00 to singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0),
                n11 to n10 to singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0),
                n21 to n20 to singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0),
                n01 to n02 to singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0),
                n11 to n12 to singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0),
                n21 to n22 to singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0),
                n10 to n20 to singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0),
                n11 to n21 to singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0),
                n12 to n22 to singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0),
                n02 to n01 to singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0),
                n12 to n11 to singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0),
                n22 to n21 to singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0),
                n20 to n10 to singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0),
                n21 to n11 to singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0),
                n22 to n12 to singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0)
        )
        checkSuccessors(transitions)
        checkPredecessors(transitions)
    }


    /*@Test
    fun fragmentOneTest() {
        //Note that these edges can be quite easily "derived" from one dimensional model.
        //It's basically a cartesian product or two transition relations of one dimensional model.
        val transitions = listOf(
                n00 to Transition(n00, loop, singleRectangle(0.0, 1.0, 0.0, 1.0)),
                n01 to Transition(n01, loop, singleRectangle(1.0 / 3.0, 1.0 / 2.0, 0.0, 1.0)),
                n02 to Transition(n02, loop, singleRectangle(1.0 / 4.0, 2.0, 0.0, 1.0)),
                n10 to Transition(n10, loop, singleRectangle(0.0, 1.0, 1.0 / 3.0, 1.0 / 2.0)),
                n11 to Transition(n11, loop, singleRectangle(1.0 / 3.0, 1.0 / 2.0, 1.0 / 3.0, 1.0 / 2.0)),
                n12 to Transition(n12, loop, singleRectangle(1.0 / 4.0, 2.0, 1.0 / 3.0, 1.0 / 2.0)),
                n20 to Transition(n20, loop, singleRectangle(0.0, 1.0, 1.0 / 4.0, 2.0)),
                n21 to Transition(n21, loop, singleRectangle(1.0 / 3.0, 1.0 / 2.0, 1.0 / 4.0, 2.0)),
                n22 to Transition(n22, loop, singleRectangle(1.0 / 4.0, 2.0, 1.0 / 4.0, 2.0)),
                n00 to Transition(n01, v1Up, singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0)),
                n00 to Transition(n10, v2Up, singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0)),
                n10 to Transition(n11, v1Up, singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0)),
                n20 to Transition(n21, v1Up, singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0)),
                n01 to Transition(n11, v2Up, singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0)),
                n02 to Transition(n12, v2Up, singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0)),
                n10 to Transition(n00, v2Down, singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0)),
                n11 to Transition(n01, v2Down, singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0)),
                n12 to Transition(n02, v2Down, singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0)),
                n01 to Transition(n00, v1Down, singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0)),
                n11 to Transition(n10, v1Down, singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0)),
                n21 to Transition(n20, v1Down, singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0)),
                n01 to Transition(n02, v1Up, singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0)),
                n11 to Transition(n12, v1Up, singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0)),
                n21 to Transition(n22, v1Up, singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0)),
                n10 to Transition(n20, v2Up, singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0)),
                n11 to Transition(n21, v2Up, singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0)),
                n12 to Transition(n22, v2Up, singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0)),
                n02 to Transition(n01, v1Down, singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0)),
                n12 to Transition(n11, v1Down, singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0)),
                n22 to Transition(n21, v1Down, singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0)),
                n20 to Transition(n10, v2Down, singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0)),
                n21 to Transition(n11, v2Down, singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0)),
                n22 to Transition(n12, v2Down, singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0))
        )
        verify(fragmentOne, transitions)
    }
    */

    fun fragmentTwoTest() {
        checkSuccessors()
        checkPredecessors()
    }

    /*
    @Test
    fun fragmentTwoTest() {
        //Unfortunately, here you really have to compute it by hand or check it with visualization.
        val transitions = listOf(
                n00 to Transition(n00, loop, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                n01 to Transition(n01, loop, singleRectangle(2.5 / 4.0, 1.25, 0.0, 2.0)),
                n02 to Transition(n02, loop, singleRectangle(0.5, 2.0, 0.0, 2.0)),
                n10 to Transition(n10, loop, singleRectangle(0.0, 1.25, 2.5 / 3.0, 2.0)),
                n11 to Transition(n11, loop, singleRectangle(0.5, 2.5 / 3.0, 1.25, 2.0)),
                //n12 to Transition(n12, singleRectangle(1.0/4.0,2.0, 1.0/3.0,1.0/2.0)), disabled
                n20 to Transition(n20, loop, singleRectangle(0.0, 2.5 / 3.0, 0.625, 2.0)),
                n21 to Transition(n21, loop, singleRectangle(2.5 / 6.0, 0.625, 2.5 / 3.0, 2.0)),
                n22 to Transition(n22, loop, singleRectangle(2.5 / 7.0, 2.0, 1.25, 2.0)),
                n00 to Transition(n01, v1Up, singleRectangle(2.5 / 3.0, 2.0, 0.0, 2.0)),
                n00 to Transition(n10, v2Up, singleRectangle(0.0, 2.0, 5.0 / 4.0, 2.0)),
                n10 to Transition(n11, v1Up, singleRectangle(0.625, 2.0, 0.0, 2.0)),
                n20 to Transition(n21, v1Up, singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0)),
                //n01 to Transition(n11, singleRectangle(0.0,2.0, 1.0/2.0, 2.0)), disabled
                //n02 to Transition(n12, singleRectangle(0.0,2.0, 1.0/2.0, 2.0)), disabled
                n10 to Transition(n00, v2Down, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                n11 to Transition(n01, v2Down, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                n12 to Transition(n02, v2Down, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                n01 to Transition(n00, v1Down, singleRectangle(0.0, 5.0 / 4.0, 0.0, 2.0)),
                n11 to Transition(n10, v1Down, singleRectangle(0.0, 2.5 / 3.0, 0.0, 2.0)),
                n21 to Transition(n20, v1Down, singleRectangle(0.0, 0.625, 0.0, 2.0)),
                n01 to Transition(n02, v1Up, singleRectangle(2.5 / 4.0, 2.0, 0.0, 2.0)),
                n11 to Transition(n12, v1Up, singleRectangle(0.5, 2.0, 0.0, 2.0)),
                n21 to Transition(n22, v1Up, singleRectangle(2.5 / 6.0, 2.0, 0.0, 2.0)),
                n10 to Transition(n20, v2Up, singleRectangle(0.0, 2.0, 2.5 / 3.0, 2.0)),
                n11 to Transition(n21, v2Up, singleRectangle(0.0, 2.0, 1.25, 2.0)),
                // n12 to Transition(n22, singleRectangle(0.0,2.0, 1.0/3.0,2.0)), disabled
                n02 to Transition(n01, v1Down, singleRectangle(0.0, 2.5 / 3.0, 0.0, 2.0)),
                n12 to Transition(n11, v1Down, singleRectangle(0.0, 0.625, 0.0, 2.0)),
                n22 to Transition(n21, v1Down, singleRectangle(0.0, 0.5, 0.0, 2.0)),
                n20 to Transition(n10, v2Down, singleRectangle(0.0, 2.0, 0.0, 1.25)),
                n21 to Transition(n11, v2Down, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                n22 to Transition(n12, v2Down, singleRectangle(0.0, 2.0, 0.0, 2.0))
        )
        verify(fragmentTwo, transitions)
    }

    private fun verify(model: ParamsOdeTransitionSystem, transitions: List<Pair<Int, Transition<MutableSet<Rectangle>>>>) {
        model.run {
            for (node in (0 until stateCount)) {
                solver.assertTransitionEquals(node.successors().iterator(), *transitions
                        .filter { it.first == node }.map { it.second }.toTypedArray()
                )
                solver.assertTransitionEquals(node.predecessors().iterator(), *transitions
                        .filter { it.second.target == node }.map { it.second.copy(target = it.first) }.toTypedArray()
                )
            }
        }
    }*/
}