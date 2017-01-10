package com.github.sybila.ode.generator.rect

import com.github.sybila.huctl.*
import com.github.sybila.ode.generator.distributed.rect.RectangleOdeModel
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertFails


class TwoDimPropositionTest {

    private val v1 = OdeModel.Variable(name = "v1", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0)),
                    Summand(paramIndex = 0), Summand(constant = -1.0)
            ))

    private val v2 = OdeModel.Variable(name = "v2", range = Pair(0.0, 5.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 3.0, 5.0),
            equation = listOf(
                    Summand(paramIndex = 1, variableIndices = listOf(1)),
                    Summand(paramIndex = 1), Summand(constant = -1.0)
            ))

    private val fragmentOne = RectangleOdeModel(OdeModel(listOf(v1, v2), listOf(
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

    @Test
    fun gtTest() {
        fragmentOne.run {
            val expected1 = listOf(n01, n02, n11, n12, n21, n22).map { it to tt }.toMap().asStateMap()
            val expected2 = listOf(n20, n21, n22).map { it to tt }.toMap().asStateMap()

            expected1.assertDeepEquals(("v1".asVariable() gt 1.0.asConstant()).eval())
            expected1.assertDeepEquals(("v1".asVariable() ge 1.0.asConstant()).eval())
            expected2.assertDeepEquals(("v2".asVariable() gt 3.0.asConstant()).eval())
            expected2.assertDeepEquals(("v2".asVariable() ge 3.0.asConstant()).eval())
        }
    }

    @Test
    fun ltTest() {
        fragmentOne.run {
            val expected1 = listOf(n00, n10, n20).map { it to tt }.toMap().asStateMap()
            val expected2 = listOf(n00, n01, n02, n10, n11, n12).map { it to tt }.toMap().asStateMap()

            expected1.assertDeepEquals(("v1".asVariable() lt 1.0.asConstant()).eval())
            expected1.assertDeepEquals(("v1".asVariable() le 1.0.asConstant()).eval())
            expected2.assertDeepEquals(("v2".asVariable() lt 3.0.asConstant()).eval())
            expected2.assertDeepEquals(("v2".asVariable() le 3.0.asConstant()).eval())
        }
    }

    @Test
    fun eqTest() {
        assertFails {
            fragmentOne.run { ("v1".asVariable() eq 2.0.asConstant()).eval() }
        }
    }

    @Test
    fun invalidThreshold() {
        assertFails {
            fragmentOne.run { ("v1".asVariable() gt 1.5.asConstant()).eval() }
        }
    }

    @Test
    fun invalidFloatProposition() {
        assertFails {
            fragmentOne.run { (3.0.asConstant() lt 4.0.asConstant()).eval() }
        }
    }
}