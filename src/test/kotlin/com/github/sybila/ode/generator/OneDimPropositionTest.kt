package com.github.sybila.ode.generator

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.checker.nodesOf
import com.github.sybila.ctl.*
import com.github.sybila.ode.model.Model
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class OneDimPropositionTest {

    private val v1 = Model.Variable(
            name = "v1", range = Pair(0.0,6.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 4.0, 6.0),
            equation = listOf() //equation values shouldn't influence propositions
    )

    private val fragmentOne = RectangleOdeFragment(Model(listOf(v1), listOf(
            Model.Parameter("p1", Pair(0.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    private val n0 = IDNode(0)
    private val n1 = IDNode(1)
    private val n2 = IDNode(2)

    val c = fragmentOne.fullColors
    val e = fragmentOne.emptyColors

    @Test
    fun gtTest() {
        val gtTwo = fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT, 2.0))
        val gtEqTwo = fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT_EQ, 2.0))
        val gtTwoFlipped = fragmentOne.validNodes(FloatProposition(Constant(2.0), CompareOp.LT, Variable("v1")))
        val gtEqTwoFlipped = fragmentOne.validNodes(FloatProposition(Constant(2.0), CompareOp.LT_EQ, Variable("v1")))
        val res = nodesOf(e, n1 to c, n2 to c)
        assertEquals(res, gtTwo)
        assertEquals(res, gtEqTwo)
        assertEquals(res, gtTwoFlipped)
        assertEquals(res, gtEqTwoFlipped)
        val gtZero = fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT, 0.0))
        val gtFour = fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT, 4.0))
        val gtSix = fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT, 6.0))
        assertEquals(nodesOf(e, n0 to c, n1 to c, n2 to c), gtZero)
        assertEquals(nodesOf(e, n2 to c), gtFour)
        assertEquals(nodesOf(e), gtSix)
    }

    @Test
    fun ltTest() {
        val ltFour = fragmentOne.validNodes(FloatProposition("v1", CompareOp.LT, 4.0))
        val ltEqFour = fragmentOne.validNodes(FloatProposition("v1", CompareOp.LT_EQ, 4.0))
        val ltFourFlipped = fragmentOne.validNodes(FloatProposition(Constant(4.0), CompareOp.GT, Variable("v1")))
        val ltEqFourFlipped = fragmentOne.validNodes(FloatProposition(Constant(4.0), CompareOp.GT_EQ, Variable("v1")))
        val res = nodesOf(e, n0 to c, n1 to c)
        assertEquals(res, ltFour)
        assertEquals(res, ltEqFour)
        assertEquals(res, ltFourFlipped)
        assertEquals(res, ltEqFourFlipped)
        val ltZero = fragmentOne.validNodes(FloatProposition("v1", CompareOp.LT, 0.0))
        val ltTwo = fragmentOne.validNodes(FloatProposition("v1", CompareOp.LT, 2.0))
        val ltSix = fragmentOne.validNodes(FloatProposition("v1", CompareOp.LT, 6.0))
        assertEquals(nodesOf(e), ltZero)
        assertEquals(nodesOf(e, n0 to c), ltTwo)
        assertEquals(nodesOf(e, n0 to c, n1 to c, n2 to c), ltSix)
    }

    @Test
    fun eqTest() {
        assertFails {
            fragmentOne.validNodes(FloatProposition("v1", CompareOp.EQ, 2.0))
        }
    }

    @Test
    fun invalidThreshold() {
        assertFails {
            fragmentOne.validNodes(FloatProposition("v1", CompareOp.GT, 1.0))
        }
    }

    @Test
    fun invalidProposition() {
        assertFails {
            fragmentOne.validNodes(DirectionProposition("v1", Direction.IN, Facet.POSITIVE))
        }
    }

    @Test
    fun invalidFloatProposition() {
        assertFails {
            fragmentOne.validNodes(FloatProposition(Constant(3.0), CompareOp.LT, Constant(4.0)))
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
}