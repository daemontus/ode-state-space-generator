package com.github.sybila.ode.generator.rect

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.checker.nodesOf
import com.github.sybila.ctl.Direction
import com.github.sybila.ctl.DirectionProposition
import com.github.sybila.ctl.Facet
import com.github.sybila.ode.model.Model
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class OneDimDirectionPropositionTest {

    //model from OneDimWithParamGeneratorTest
    //dv1 = p*(v1/2 + 1) - 1
    private val v1 = Model.Variable(
            name = "v1", range = Pair(0.0, 6.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 4.0, 6.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0), constant = 0.5),
                    Summand(paramIndex = 0),
                    Summand(constant = -1.0))
    )

    // 3p - 1 < 0

    private val fragmentOne = RectangleOdeFragment(Model(listOf(v1), listOf(
            Model.Parameter("p1", Pair(0.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    private val n0 = IDNode(0)
    private val n1 = IDNode(1)
    private val n2 = IDNode(2)

    private val c = RectangleColors()

    @Test
    fun positiveIn() {
        val res = fragmentOne.validNodes(DirectionProposition("v1", Direction.IN, Facet.POSITIVE))
        assertEquals(nodesOf(c,
                n0 to RectangleColors(rectangleOf(0.0,1.0/2.0)),
                n1 to RectangleColors(rectangleOf(0.0,1.0/3.0))
                ), res)
    }

    @Test
    fun positiveOut() {
        val res = fragmentOne.validNodes(DirectionProposition("v1", Direction.OUT, Facet.POSITIVE))
        assertEquals(nodesOf(c,
                n0 to RectangleColors(rectangleOf(1.0/2.0,2.0)),
                n1 to RectangleColors(rectangleOf(1.0/3.0,2.0))
        ), res)
    }

    @Test
    fun negativeIn() {
        val res = fragmentOne.validNodes(DirectionProposition("v1", Direction.IN, Facet.NEGATIVE))
        assertEquals(nodesOf(c,
                n1 to RectangleColors(rectangleOf(1.0/2.0,2.0)),
                n2 to RectangleColors(rectangleOf(1.0/3.0,2.0))
        ), res)
    }

    @Test
    fun negativeOut() {
        val res = fragmentOne.validNodes(DirectionProposition("v1", Direction.OUT, Facet.NEGATIVE))
        assertEquals(nodesOf(c,
                n1 to RectangleColors(rectangleOf(0.0,1.0/2.0)),
                n2 to RectangleColors(rectangleOf(0.0,1.0/3.0))
        ), res)
    }

    @Test
    fun unknownVariable() {
        assertFails {
            fragmentOne.validNodes(DirectionProposition("v2", Direction.IN, Facet.POSITIVE))
        }
    }

}