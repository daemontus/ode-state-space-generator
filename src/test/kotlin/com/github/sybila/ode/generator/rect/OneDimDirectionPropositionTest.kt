package com.github.sybila.ode.generator.rect

import com.github.sybila.huctl.negativeIn
import com.github.sybila.huctl.negativeOut
import com.github.sybila.huctl.positiveIn
import com.github.sybila.huctl.positiveOut
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertFails

class OneDimDirectionPropositionTest {

    //model from OneDimWithParamGeneratorTest
    //dv1 = p*(v1/2 + 1) - 1
    private val v1 = OdeModel.Variable(
            name = "v1", range = Pair(0.0, 6.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 4.0, 6.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0), constant = 0.5),
                    Summand(paramIndex = 0),
                    Summand(constant = -1.0))
    )

    private val fragmentOne = RectangleOdeModel(OdeModel(listOf(v1), listOf(
            OdeModel.Parameter("p1", Pair(0.0, 2.0))
    )))

    private val n0 = 0
    private val n1 = 1
    private val n2 = 2

    @Test
    fun positiveIn() {
        fragmentOne.run {
            mapOf(
                    n0 to rectangleOf(0.0,1.0/2.0).asParams(),
                    n1 to rectangleOf(0.0,1.0/3.0).asParams()
            ).asStateMap().assertDeepEquals("v1".positiveIn().eval())
        }
    }

    @Test
    fun positiveOut() {
        fragmentOne.run {
            mapOf(
                    n0 to rectangleOf(1.0/2.0,2.0).asParams(),
                    n1 to rectangleOf(1.0/3.0,2.0).asParams()
            ).asStateMap().assertDeepEquals("v1".positiveOut().eval())
        }
    }

    @Test
    fun negativeIn() {
        fragmentOne.run {
            mapOf(
                    n1 to rectangleOf(1.0/2.0,2.0).asParams(),
                    n2 to rectangleOf(1.0/3.0,2.0).asParams()
            ).asStateMap().assertDeepEquals("v1".negativeIn().eval())
        }
    }

    @Test
    fun negativeOut() {
        fragmentOne.run {
            mapOf(
                    n1 to rectangleOf(0.0,1.0/2.0).asParams(),
                    n2 to rectangleOf(0.0,1.0/3.0).asParams()
            ).asStateMap().assertDeepEquals("v1".negativeOut().eval())
        }
    }

    @Test
    fun unknownVariable() {
        assertFails {
            fragmentOne.run {
                "v2".positiveIn().eval()
            }
        }
    }

}