package com.github.sybila.ode.model

import org.junit.Test
import kotlin.test.assertTrue

private val error = Math.pow(10.0, -9.0)

class RampApproximationTest {

    @Test
    fun singleRampTest() {

        val approximation = RampApproximation(0, doubleArrayOf(1.0, 2.0), doubleArrayOf(3.5, 4.2))
        val ramp = Ramp.positive(0, 1.0, 2.0, 3.5, 4.2)

        var t = 0.2
        while (t < 4.0) {
            assertTrue(
                    Math.abs(approximation(t) - ramp(t)) < error,
                    "Problem in $approximation: expected ${ramp(t)}, got ${approximation(t)} for value $t")
            t += 0.2
        }

    }

    @Test
    fun twoRampTest() {

        val approximation = RampApproximation(0, doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(3.5, 4.2, 2.2))
        val ramp1 = Ramp.positive(0, 1.0, 2.0, 3.5, 4.2)
        val ramp2 = Ramp.negative(0, 2.0, 3.0, 4.2, 2.2)

        var t = 0.2
        while (t < 4.0) {
            if (t > 2.0) {
                assertTrue(
                        Math.abs(approximation(t) - ramp2(t)) < error,
                        "Problem in $approximation: expected ${ramp2(t)}, got ${approximation(t)} for value $t")
            } else {
                assertTrue(
                        Math.abs(approximation(t) - ramp1(t)) < error,
                        "Problem in $approximation: expected ${ramp1(t)}, got ${approximation(t)} for value $t")
            }
            t += 0.2
        }

        assertTrue(
                Math.abs(approximation(2.0) - 4.2) < error,
                "Problem in $approximation: expected ${ramp2(2.0)}, got ${approximation(2.0)} for value 4.2")

    }

}