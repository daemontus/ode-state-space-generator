package com.github.sybila.ode.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WriterTest {

    @Test
    fun writerTest() {
        val m = Model(
                listOf(Model.Variable(
                        name = "V1", range = 0.0 to 3.0,
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(constant = 3.14, paramIndex = 0, variableIndices = listOf(1),
                                evaluable = listOf(RampApproximation(0,
                                doubleArrayOf(0.0, 2.0, 3.0), doubleArrayOf(1.2, 2.2, -0.3))
                        ))
                )
                ), Model.Variable(
                        name = "V2", range = 0.0 to 3.0,
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(constant = -2.03, paramIndex = -1, variableIndices = listOf(0),
                                evaluable = listOf(RampApproximation(0,
                                        doubleArrayOf(0.0, 2.0, 3.0), doubleArrayOf(-1.2, -2.2, 0.3))
                                ))
                )
                )), listOf(Model.Parameter("foo", 0.0 to 10.0))
        )

        println(m.toBio())

        assertEquals(m, Parser().parse(m.toBio()))
    }

    @Test
    fun invalidModel() {
        assertFailsWith(IllegalArgumentException::class) {
            Model(Model.Variable(
                    name = "foo",
                    thresholds = listOf(1.0, 2.0),
                    range = 1.0 to 2.0,
                    varPoints = null,
                    equation = listOf(Summand(evaluable = listOf(Hill(0, 0.0, 0.0, 0.0, 0.0, true))))
            )).toBio()
        }
    }
}