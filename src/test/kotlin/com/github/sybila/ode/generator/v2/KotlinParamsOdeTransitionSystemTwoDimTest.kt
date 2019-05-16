package com.github.sybila.ode.generator.v2

import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.generator.rect.rectangleOf
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test

class KotlinParamsOdeTransitionSystemTwoDimTest {
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

    private val fragmentOne = KotlinParamsOdeTransitionSystem(OdeModel(listOf(v1, v2), listOf(
            OdeModel.Parameter(name = "p1", range = Pair(0.0, 2.0)),
            OdeModel.Parameter(name = "p2", range = Pair(0.0, 2.0))
    )), true)

    private val fragmentTwo = KotlinParamsOdeTransitionSystem(OdeModel(listOf(v3, v4), listOf(
            OdeModel.Parameter(name = "p1", range = Pair(0.0, 2.0)),
            OdeModel.Parameter(name = "p2", range = Pair(0.0, 2.0))
    )), true)

    val n00 = 0
    val n01 = 1
    val n02 = 2
    val n10 = 3
    val n11 = 4
    val n12 = 5
    val n20 = 6
    val n21 = 7
    val n22 = 8

    private fun singleRectangle(vararg values: Double): MutableSet<Rectangle>
            = mutableSetOf(rectangleOf(*values))

    private fun checkTransitions(model: KotlinParamsOdeTransitionSystem, expectedTransitions: Map<Pair<Int, Int>, MutableSet<Rectangle>>) {
        model.run {
            for (node in 0 until model.stateCount) {
                for (successor in node.successors()) {
                    assert(expectedTransitions[Pair(node, successor)] == transitionParameters(node, successor))
                }

                for (predecessor in node.predecessors()) {
                    assert(expectedTransitions[Pair(predecessor, node)] == transitionParameters(predecessor, node))
                }
            }
        }
    }

    @Test
    fun fragmentOneTest() {
        val expectedTransitions: Map<Pair<Int, Int>, MutableSet<Rectangle>> = mapOf(
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
        checkTransitions(fragmentOne, expectedTransitions)
    }

    @Test
    fun fragmentTwoTest() {
        val expectedTransitions: Map<Pair<Int, Int>, MutableSet<Rectangle>> = mapOf(
                n00 to n00 to singleRectangle(0.0, 2.0, 0.0, 2.0),
                n01 to n01 to singleRectangle(2.5 / 4.0, 1.25, 0.0, 2.0),
                n02 to n02 to singleRectangle(0.5, 2.0, 0.0, 2.0),
                n10 to n10 to singleRectangle(0.0, 1.25, 2.5 / 3.0, 2.0),
                n11 to n11 to singleRectangle(0.5, 2.5 / 3.0, 1.25, 2.0),
                //n12 to n12 to singleRectangle(1.0/4.0,2.0, 1.0/3.0,1.0/2.0)), disabled
                n20 to n20 to singleRectangle(0.0, 2.5 / 3.0, 0.625, 2.0),
                n21 to n21 to singleRectangle(2.5 / 6.0, 0.625, 2.5 / 3.0, 2.0),
                n22 to n22 to singleRectangle(2.5 / 7.0, 2.0, 1.25, 2.0),
                n00 to n01 to singleRectangle(2.5 / 3.0, 2.0, 0.0, 2.0),
                n00 to n10 to singleRectangle(0.0, 2.0, 5.0 / 4.0, 2.0),
                n10 to n11 to singleRectangle(0.625, 2.0, 0.0, 2.0),
                n20 to n21 to singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0),
                //n01 to n11 to singleRectangle(0.0,2.0, 1.0/2.0, 2.0)), disabled
                //n02 to n12 to singleRectangle(0.0,2.0, 1.0/2.0, 2.0)), disabled
                n10 to n00 to singleRectangle(0.0, 2.0, 0.0, 2.0),
                n11 to n01 to singleRectangle(0.0, 2.0, 0.0, 2.0),
                n12 to n02 to singleRectangle(0.0, 2.0, 0.0, 2.0),
                n01 to n00 to singleRectangle(0.0, 5.0 / 4.0, 0.0, 2.0),
                n11 to n10 to singleRectangle(0.0, 2.5 / 3.0, 0.0, 2.0),
                n21 to n20 to singleRectangle(0.0, 0.625, 0.0, 2.0),
                n01 to n02 to singleRectangle(2.5 / 4.0, 2.0, 0.0, 2.0),
                n11 to n12 to singleRectangle(0.5, 2.0, 0.0, 2.0),
                n21 to n22 to singleRectangle(2.5 / 6.0, 2.0, 0.0, 2.0),
                n10 to n20 to singleRectangle(0.0, 2.0, 2.5 / 3.0, 2.0),
                n11 to n21 to singleRectangle(0.0, 2.0, 1.25, 2.0),
                // n12 to n22 to singleRectangle(0.0,2.0, 1.0/3.0,2.0)), disabled
                n02 to n01 to singleRectangle(0.0, 2.5 / 3.0, 0.0, 2.0),
                n12 to n11 to singleRectangle(0.0, 0.625, 0.0, 2.0),
                n22 to n21 to singleRectangle(0.0, 0.5, 0.0, 2.0),
                n20 to n10 to singleRectangle(0.0, 2.0, 0.0, 1.25),
                n21 to n11 to singleRectangle(0.0, 2.0, 0.0, 2.0),
                n22 to n12 to singleRectangle(0.0, 2.0, 0.0, 2.0)
        )
        checkTransitions(fragmentTwo, expectedTransitions)
    }
}