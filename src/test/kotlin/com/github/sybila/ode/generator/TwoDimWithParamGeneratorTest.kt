package com.github.sybila.ode.generator

import com.github.sybila.checker.*
import com.github.sybila.ode.model.Model
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertEquals

class TwoDimWithParamGeneratorTest {

    //We don't test without parameters on two dimensions, because
    //there are already too many options.
    //With parameters, we can encode these options into only few models.

    //Model 1:
    //A simple model based on one dimensional cases.
    //dv1 = p1(v1 + 1) - 1
    //dv2 = p2(v2 + 1) - 1

    //Model 2:
    //More complicated model where variable actually depend on each other.
    //It also contains a nice spiral :)
    //dv1 = p1(v1 + v2 + 1) - 2.5
    //dv2 = p2(v2 - v1 + 1) - 2.5

    private val v1 = Model.Variable(name = "v1", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0)),
                    Summand(paramIndex = 0), Summand(constant = -1.0)
            ))

    private val v2 = Model.Variable(name = "v2", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            equation = listOf(
                    Summand(paramIndex = 1, variableIndices = listOf(1)),
                    Summand(paramIndex = 1), Summand(constant = -1.0)
            ))

    private val v3 = v1.copy(name = "v3", equation = listOf(
            Summand(paramIndex = 0, variableIndices = listOf(0)),
            Summand(paramIndex = 0, variableIndices = listOf(1)),
            Summand(paramIndex = 0), Summand(constant = -2.5)
    ))

    private val v4 = v1.copy(name = "v4", equation = listOf(
            Summand(paramIndex = 1, variableIndices = listOf(0), constant = -1.0),
            Summand(paramIndex = 1, variableIndices = listOf(1)),
            Summand(paramIndex = 1), Summand(constant = -2.5)
    ))

    private val fragmentOne = RectangleOdeFragment(Model(listOf(v1, v2), listOf(
            Model.Parameter(name = "p1", range = Pair(0.0, 2.0)),
            Model.Parameter(name = "p2", range = Pair(0.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    private val fragmentTwo = RectangleOdeFragment(Model(listOf(v3, v4), listOf(
            Model.Parameter(name = "p1", range = Pair(0.0, 2.0)),
            Model.Parameter(name = "p2", range = Pair(0.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    val n00 = IDNode(0)
    val n01 = IDNode(1)
    val n02 = IDNode(2)
    val n10 = IDNode(3)
    val n11 = IDNode(4)
    val n12 = IDNode(5)
    val n20 = IDNode(6)
    val n21 = IDNode(7)
    val n22 = IDNode(8)

    val allNodes = listOf(n00, n01, n02, n10, n11, n12, n20, n21, n22)

    val fullColors = singleRectangle(0.0,2.0, 0.0,2.0)
    private val e = RectangleColors()

    private fun singleRectangle(vararg values: Double): RectangleColors
            = RectangleColors(Rectangle(values))

    @Test fun fragmentOneTest() {
        //Note that these edges can be quite easily "derived" from one dimensional model.
        //It's basically a cartesian product or two transition relations of one dimensional model.
        val transitions = listOf(
                Edge(n00, n00, singleRectangle(0.0, 1.0, 0.0, 1.0)),
                Edge(n01, n01, singleRectangle(1.0 / 3.0, 1.0 / 2.0, 0.0, 1.0)),
                Edge(n02, n02, singleRectangle(1.0 / 4.0, 2.0, 0.0, 1.0)),
                Edge(n10, n10, singleRectangle(0.0, 1.0, 1.0 / 3.0, 1.0 / 2.0)),
                Edge(n11, n11, singleRectangle(1.0 / 3.0, 1.0 / 2.0, 1.0 / 3.0, 1.0 / 2.0)),
                Edge(n12, n12, singleRectangle(1.0 / 4.0, 2.0, 1.0 / 3.0, 1.0 / 2.0)),
                Edge(n20, n20, singleRectangle(0.0, 1.0, 1.0 / 4.0, 2.0)),
                Edge(n21, n21, singleRectangle(1.0 / 3.0, 1.0 / 2.0, 1.0 / 4.0, 2.0)),
                Edge(n22, n22, singleRectangle(1.0 / 4.0, 2.0, 1.0 / 4.0, 2.0)),
                Edge(n00, n01, singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0)),
                Edge(n00, n10, singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0)),
                Edge(n10, n11, singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0)),
                Edge(n20, n21, singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0)),
                Edge(n01, n11, singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0)),
                Edge(n02, n12, singleRectangle(0.0, 2.0, 1.0 / 2.0, 2.0)),
                Edge(n10, n00, singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0)),
                Edge(n11, n01, singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0)),
                Edge(n12, n02, singleRectangle(0.0, 2.0, 0.0, 1.0 / 2.0)),
                Edge(n01, n00, singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0)),
                Edge(n11, n10, singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0)),
                Edge(n21, n20, singleRectangle(0.0, 1.0 / 2.0, 0.0, 2.0)),
                Edge(n01, n02, singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0)),
                Edge(n11, n12, singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0)),
                Edge(n21, n22, singleRectangle(1.0 / 3.0, 2.0, 0.0, 2.0)),
                Edge(n10, n20, singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0)),
                Edge(n11, n21, singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0)),
                Edge(n12, n22, singleRectangle(0.0, 2.0, 1.0 / 3.0, 2.0)),
                Edge(n02, n01, singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0)),
                Edge(n12, n11, singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0)),
                Edge(n22, n21, singleRectangle(0.0, 1.0 / 3.0, 0.0, 2.0)),
                Edge(n20, n10, singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0)),
                Edge(n21, n11, singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0)),
                Edge(n22, n12, singleRectangle(0.0, 2.0, 0.0, 1.0 / 3.0))
        )
        verify(fragmentOne, transitions)
    }

    @Test fun fragmentTwoTest() {
        //Unfortunately, here you really have to compute it by hand or check it with visualization.
        val transitions = listOf(
                Edge(n00, n00, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                Edge(n01, n01, singleRectangle(2.5 / 4.0, 1.25, 0.0, 2.0)),
                Edge(n02, n02, singleRectangle(0.5, 2.0, 0.0, 2.0)),
                Edge(n10, n10, singleRectangle(0.0, 1.25, 2.5 / 3.0, 2.0)),
                Edge(n11, n11, singleRectangle(0.5, 2.5 / 3.0, 1.25, 2.0)),
                //Edge(n12, n12, singleRectangle(1.0/4.0,2.0, 1.0/3.0,1.0/2.0)), disabled
                Edge(n20, n20, singleRectangle(0.0, 2.5 / 3.0, 0.625, 2.0)),
                Edge(n21, n21, singleRectangle(2.5 / 6.0, 0.625, 2.5 / 3.0, 2.0)),
                Edge(n22, n22, singleRectangle(2.5 / 7.0, 2.0, 1.25, 2.0)),
                Edge(n00, n01, singleRectangle(2.5 / 3.0, 2.0, 0.0, 2.0)),
                Edge(n00, n10, singleRectangle(0.0, 2.0, 5.0 / 4.0, 2.0)),
                Edge(n10, n11, singleRectangle(0.625, 2.0, 0.0, 2.0)),
                Edge(n20, n21, singleRectangle(1.0 / 2.0, 2.0, 0.0, 2.0)),
                //Edge(n01, n11, singleRectangle(0.0,2.0, 1.0/2.0, 2.0)), disabled
                //Edge(n02, n12, singleRectangle(0.0,2.0, 1.0/2.0, 2.0)), disabled
                Edge(n10, n00, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                Edge(n11, n01, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                Edge(n12, n02, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                Edge(n01, n00, singleRectangle(0.0, 5.0 / 4.0, 0.0, 2.0)),
                Edge(n11, n10, singleRectangle(0.0, 2.5 / 3.0, 0.0, 2.0)),
                Edge(n21, n20, singleRectangle(0.0, 0.625, 0.0, 2.0)),
                Edge(n01, n02, singleRectangle(2.5 / 4.0, 2.0, 0.0, 2.0)),
                Edge(n11, n12, singleRectangle(0.5, 2.0, 0.0, 2.0)),
                Edge(n21, n22, singleRectangle(2.5 / 6.0, 2.0, 0.0, 2.0)),
                Edge(n10, n20, singleRectangle(0.0, 2.0, 2.5 / 3.0, 2.0)),
                Edge(n11, n21, singleRectangle(0.0, 2.0, 1.25, 2.0)),
               // Edge(n12, n22, singleRectangle(0.0,2.0, 1.0/3.0,2.0)), disabled
                Edge(n02, n01, singleRectangle(0.0, 2.5 / 3.0, 0.0, 2.0)),
                Edge(n12, n11, singleRectangle(0.0, 0.625, 0.0, 2.0)),
                Edge(n22, n21, singleRectangle(0.0, 0.5, 0.0, 2.0)),
                Edge(n20, n10, singleRectangle(0.0, 2.0, 0.0, 1.25)),
                Edge(n21, n11, singleRectangle(0.0, 2.0, 0.0, 2.0)),
                Edge(n22, n12, singleRectangle(0.0, 2.0, 0.0, 2.0))
        )
        verify(fragmentTwo, transitions)
    }

    private fun verify(fragment: RectangleOdeFragment, transitions: List<Edge<IDNode, RectangleColors>>) {
        assertEquals(allNodes.map { Pair(it, fullColors) }.toMap().toNodes(e), fragment.allNodes())

        for (node in allNodes) {
            val successors = fragment.successors.invoke(node)
            assertEquals(transitions.filter {
                it.start == node
            }.map { Pair(it.end, it.colors) }.toMap().toNodes(e),
                    successors)
            val predecessors = fragment.predecessors.invoke(node)
            assertEquals(transitions.filter {
                it.end == node
            }.map { Pair(it.start, it.colors) }.toMap().toNodes(e),
                    predecessors)
        }
    }

}