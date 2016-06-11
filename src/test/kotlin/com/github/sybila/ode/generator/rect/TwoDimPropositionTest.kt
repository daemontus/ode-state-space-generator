package com.github.sybila.ode.generator.rect

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.checker.nodesOf
import com.github.sybila.ctl.*
import com.github.sybila.ode.generator.rect.RectangleOdeFragment
import com.github.sybila.ode.model.Model
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TwoDimPropositionTest {

    private val v1 = Model.Variable(name = "v1", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0)),
                    Summand(paramIndex = 0), Summand(constant = -1.0)
            ))

    private val v2 = Model.Variable(name = "v2", range = Pair(0.0, 5.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 3.0, 5.0),
            equation = listOf(
                    Summand(paramIndex = 1, variableIndices = listOf(1)),
                    Summand(paramIndex = 1), Summand(constant = -1.0)
            ))

    private val fragmentOne = RectangleOdeFragment(Model(listOf(v1, v2), listOf(
            Model.Parameter(name = "p1", range = Pair(0.0, 2.0)),
            Model.Parameter(name = "p2", range = Pair(0.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    val e = fragmentOne.emptyColors
    val c = fragmentOne.fullColors

    val n00 = IDNode(0)
    val n01 = IDNode(1)
    val n02 = IDNode(2)
    val n10 = IDNode(3)
    val n11 = IDNode(4)
    val n12 = IDNode(5)
    val n20 = IDNode(6)
    val n21 = IDNode(7)
    val n22 = IDNode(8)

    @Test
    fun gtTest() {
        val expected1 = nodesOf(e, n01 to c, n02 to c, n11 to c, n12 to c, n21 to c, n22 to c)
        val expected2 = nodesOf(e, n20 to c, n21 to c, n22 to c)
        assertEquals(expected1, fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT, 1.0)))
        assertEquals(expected1, fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT_EQ, 1.0)))
        assertEquals(expected2, fragmentOne.validNodes(FloatProposition("v2", CompareOp.GT, 3.0)))
        assertEquals(expected2, fragmentOne.validNodes(FloatProposition("v2", CompareOp.GT_EQ, 3.0)))
    }

    @Test
    fun ltTest() {
        val expected1 = nodesOf(e, n00 to c, n10 to c, n20 to c)
        val expected2 = nodesOf(e, n00 to c, n01 to c, n02 to c, n10 to c, n11 to c, n12 to c)
        assertEquals(expected1, fragmentOne.validNodes(FloatProposition("v1", CompareOp.LT, 1.0)))
        assertEquals(expected1, fragmentOne.validNodes(FloatProposition("v1", CompareOp.LT_EQ, 1.0)))
        assertEquals(expected2, fragmentOne.validNodes(FloatProposition("v2", CompareOp.LT, 3.0)))
        assertEquals(expected2, fragmentOne.validNodes(FloatProposition("v2", CompareOp.LT_EQ, 3.0)))
    }

    @Test
    fun eqTest() {
        assertFails {
            fragmentOne.validNodes(FloatProposition("v1", CompareOp.EQ, 2.0))
        }
    }

    @Test
    fun trueTest() {
        assertEquals(fragmentOne.allNodes(), fragmentOne.validNodes(True))
    }

    @Test
    fun falseTest() {
        assertEquals(nodesOf(e), fragmentOne.validNodes(False))
    }

    @Test
    fun invalidThreshold() {
        assertFails {
            fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT, 1.5))
        }
    }

    @Test
    fun invalidProposition() {
        class BadAtom(override val operator: Op, override val subFormulas: List<Formula>) : Atom
        assertFails {
            fragmentOne.validNodes(BadAtom(Op.ATOM, listOf()))
        }
    }

    @Test
    fun invalidFloatProposition() {
        assertFails {
            fragmentOne.validNodes(FloatProposition(Constant(3.0), CompareOp.LT, Constant(4.0)))
        }
    }
}