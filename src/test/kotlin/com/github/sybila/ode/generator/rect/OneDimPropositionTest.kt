package com.github.sybila.ode.generator.rect

import com.github.sybila.huctl.*
import com.github.sybila.ode.generator.distributed.rect.RectangleOdeModel
import com.github.sybila.ode.model.OdeModel
import org.junit.Test
import kotlin.test.assertFails


class OneDimPropositionTest {

    private val v1 = OdeModel.Variable(
            name = "v1", range = Pair(0.0, 6.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 4.0, 6.0),
            equation = listOf() //equation values shouldn't influence propositions
    )

    private val fragmentOne = RectangleOdeModel(OdeModel(listOf(v1), listOf(
            OdeModel.Parameter("p1", Pair(0.0, 2.0))
    )))

    @Test
    fun gtTest() {
        fragmentOne.run {
            val r = (1..2).asStateMap(tt)
            r.assertDeepEquals(("v1".asVariable() gt 2.0.asConstant()).eval())
            r.assertDeepEquals(("v1".asVariable() ge 2.0.asConstant()).eval())
            r.assertDeepEquals((2.0.asConstant() lt "v1".asVariable()).eval())
            r.assertDeepEquals((2.0.asConstant() le "v1".asVariable()).eval())
            (0..2).asStateMap(tt).assertDeepEquals(("v1".asVariable() gt 0.0.asConstant()).eval())
            2.asStateMap(tt).assertDeepEquals(("v1".asVariable() gt 4.0.asConstant()).eval())
            emptyStateMap().assertDeepEquals(("v1".asVariable() gt 6.0.asConstant()).eval())
        }
    }

    @Test
    fun ltTest() {
        fragmentOne.run {
            val r = (0..1).asStateMap(tt)
            r.assertDeepEquals(("v1".asVariable() lt 4.0.asConstant()).eval())
            r.assertDeepEquals(("v1".asVariable() le 4.0.asConstant()).eval())
            r.assertDeepEquals((4.0.asConstant() gt "v1".asVariable()).eval())
            r.assertDeepEquals((4.0.asConstant() ge "v1".asVariable()).eval())
            emptyStateMap().assertDeepEquals(("v1".asVariable() lt 0.0.asConstant()).eval())
            0.asStateMap(tt).assertDeepEquals(("v1".asVariable() lt 2.0.asConstant()).eval())
            (0..2).asStateMap(tt).assertDeepEquals(("v1".asVariable() lt 6.0.asConstant()).eval())
        }
    }

    @Test
    fun eqTest() {
        assertFails {
            fragmentOne.run {
                ("v1".asVariable() eq 2.0.asConstant()).eval()
            }
        }
    }

    @Test
    fun invalidThreshold() {
        assertFails {
            fragmentOne.run {
                ("v1".asVariable() gt 1.0.asConstant()).eval()
            }
        }
    }

    @Test
    fun invalidFloatProposition() {
        assertFails {
            fragmentOne.run {
                (3.0.asConstant() lt 4.0.asConstant()).eval()
            }
        }
    }

}