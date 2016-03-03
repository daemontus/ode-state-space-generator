package com.github.sybila.ode.model

import org.junit.Test
import kotlin.test.assertTrue

/*
    This test file contains basic tests that should verify that basic functions are being evaluated correctly.
 */


private fun Hill.alternativeEval(x: Double): Double {
    return a + (b - a) * Math.pow(x, n) / (Math.pow(theta, n) + Math.pow(x, n))
}

private fun Sigmoid.alternativeEval(x: Double): Double {
    return a + (b - a) * 0.5 * (1 + Math.tanh(k*(x - theta)))
}

private fun Step.alternativeEval(x: Double): Double {
    return if (x < theta) a else b  //really no other way to write this
}

private fun Ramp.alternativeEval(x: Double): Double {
    return a + (b - a) * Math.max(Math.min((x - lowThreshold) / (highThreshold - lowThreshold), 1.0), 0.0)
}

private val error = Math.pow(10.0, -9.0)

class SigmoidTest {

    @Test
    fun simpleTest() = test(1.0, 1.0, 0.1, 2.0)

    @Test
    fun complexTest() = test(1.24, 2.12, 0.75, 17.3)

    @Test
    fun extendedTest() {

        //can't test zero because of inverse
        val values = listOf(0.005, 0.33, 0.744, 1.0, 1.45, 1.8, 3.14, 5.0)

        for (theta in values) {
            for (n in values) {
                for (a in values) {
                    for (b in values) {
                        if (a > b) continue //skip decreasing functions, test method creates them automatically
                        test(theta, n, a, b)
                    }
                }
            }
        }
    }

    private fun test(theta: Double, n: Double, a: Double, b: Double) {
        val positiveSigmoid = Sigmoid.positive(0, theta, n, a, b)
        val negativeSigmoid = Sigmoid.negative(0, theta, n, a, b)
        val positiveSigmoidInverse = Sigmoid.positiveInverse(0, theta, n, a, b)
        val negativeSigmoidInverse = Sigmoid.negativeInverse(0, theta, n, a, b)
        var e = 0.05
        while (e < 10) {
            val expectedP = positiveSigmoid.alternativeEval(e)
            val realP = positiveSigmoid.eval(e)
            assertTrue(
                    Math.abs(expectedP - realP) < error || (expectedP.isNaN() && realP.isNaN()),
                    "Problem in $positiveSigmoid: expected $expectedP, got $realP for value $e")
            val expectedPI = positiveSigmoidInverse.alternativeEval(e)
            val realPI = positiveSigmoidInverse.eval(e)
            assertTrue(
                    Math.abs(expectedPI - realPI) < error || (expectedPI.isNaN() && realPI.isNaN()),
                    "Problem in $positiveSigmoidInverse: expected $expectedPI, got $realPI for value $e")
            assertTrue(
                    Math.abs(realPI - 1 / realP) < error || (realPI.isNaN() && realP.isNaN()),
                    "Problem in $positiveSigmoidInverse vs. $positiveSigmoid: expected 1/$realP, got $realPI for value $e")

            val expectedN = negativeSigmoid.alternativeEval(e)
            val realN = negativeSigmoid.eval(e)
            assertTrue(
                    Math.abs(expectedN - realN) < error || (expectedN.isNaN() && realN.isNaN()),
                    "Problem in $negativeSigmoid: expected $expectedN, got $realN for value $e")
            val expectedNI = negativeSigmoidInverse.alternativeEval(e)
            val realNI = negativeSigmoidInverse.eval(e)
            assertTrue(
                    Math.abs(expectedNI - realNI) < error || (expectedNI.isNaN() && realNI.isNaN()),
                    "Problem in $negativeSigmoidInverse: expected $expectedNI, got $realNI for value $e")
            assertTrue(
                    Math.abs(realNI - 1 / realN) < error || (realNI.isNaN() && realN.isNaN()),
                    "Problem in $negativeSigmoidInverse vs. $negativeSigmoid: expected 1/$realN, got $realNI for value $e")

            assertTrue(
                    Math.abs(a + b - realP - realN) < error || realP.isNaN() || realN.isNaN(),
                    "Problem in comparison $positiveSigmoid, $negativeSigmoid: positive $realP, negative $realN for value $e")
            assertTrue(
                    Math.abs(a + b - 1 / realPI - 1 / realNI) < error || realPI.isNaN() || realNI.isNaN(),
                    "Problem in comparison $positiveSigmoidInverse, $negativeSigmoidInverse: positive $realPI, negative $realNI for value $e")

            e += 0.05
            e *= 1.2
        }
    }

}

class HillTest {

    @Test
    fun simpleTest() = test(1.0, 1.0, 0.0, 2.0)

    @Test
    fun complexTest() = test(1.24, 2.12, 0.75, 17.3)

    @Test
    fun extendedTest() {

        val values = listOf(0.0, 0.005, 0.33, 0.744, 1.0, 1.45, 1.8, 3.14, 5.0)

        for (theta in values) {
            for (n in values) {
                for (a in values) {
                    for (b in values) {
                        if (a > b) continue //skip decreasing functions, test method creates them automatically
                        test(theta, n, a, b)
                    }
                }
            }
        }
    }

    private fun test(theta: Double, n: Double, a: Double, b: Double) {
        val positiveHill = Hill(0, theta, n, a, b, true)
        val negativeHill = Hill(0, theta, n, a, b, false)
        var e = 0.0
        while (e < 10) {
            val expectedP = positiveHill.alternativeEval(e)
            val realP = positiveHill.eval(e)
            assertTrue(
                    Math.abs(expectedP - realP) < error || (expectedP.isNaN() && realP.isNaN()),
                    "Problem in $positiveHill: expected $expectedP, got $realP for value $e")
            val expectedN = negativeHill.alternativeEval(e)
            val realN = negativeHill.eval(e)
            assertTrue(
                    Math.abs(expectedN - realN) < error || (expectedN.isNaN() && realN.isNaN()),
                    "Problem in $negativeHill: expected $expectedN, got $realN for value $e")
            assertTrue(
                    Math.abs(a + b - realP - realN) < error || realP.isNaN() || realN.isNaN(),
                    "Problem in comparison $positiveHill, $negativeHill: positive $realP, negative $realN for value $e")
            e += 0.05
            e *= 1.2
        }
    }

}

class RampTest() {

    @Test
    fun simpleTest() = test(1.0, 2.0, 0.0, 1.0)

    @Test
    fun complexTest() = test(1.24, 2.12, 0.75, 2.0)

    @Test
    fun extendedTest() {

        val values = listOf(0.0, 0.005, 0.33, 0.744, 1.0, 1.45, 1.8, 3.14, 5.0)

        for (low in values) {
            for (high in values) {
                for (a in values) {
                    for (b in values) {
                        if (a > b || low >= high) continue //skip decreasing functions, test method creates them automatically
                        test(low, high, a, b)
                    }
                }
            }
        }
    }

    private fun test(low: Double, high: Double, a: Double, b: Double) {
        val positiveRamp = Ramp.positiveCoordinate(0, low, high, a, b)
        val negativeRamp = Ramp.negativeCoordinate(0, low, high, a, b)
        var e = 0.0
        while (e < 10) {
            val expectedP = positiveRamp.alternativeEval(e)
            val realP = positiveRamp.eval(e)
            assertTrue(
                    Math.abs(expectedP - realP) < error || (expectedP.isNaN() && realP.isNaN()),
                    "Problem in $positiveRamp: expected $expectedP, got $realP for value $e")
            val expectedN = negativeRamp.alternativeEval(e)
            val realN = negativeRamp.eval(e)
            assertTrue(
                    Math.abs(expectedN - realN) < error || (expectedN.isNaN() && realN.isNaN()),
                    "Problem in $negativeRamp: expected $expectedN, got $realN for value $e")
            assertTrue(//can reuse directly a/b because they are changed in construction
                    Math.abs(positiveRamp.a + positiveRamp.b - realP - realN) < error || realP.isNaN() || realN.isNaN(),
                    "Problem in comparison $positiveRamp, $negativeRamp: positive $realP, negative $realN for value $e")
            e += 0.05
            e *= 1.2
        }
    }

}

class StepTest() {

    @Test
    fun simpleTest() = test(1.0, 1.0, 0.0)

    @Test
    fun complexTest() = test(1.24, 2.12, 0.75)

    @Test
    fun extendedTest() {

        val values = listOf(0.0, 0.005, 0.33, 0.744, 1.0, 1.45, 1.8, 3.14, 5.0)

        for (theta in values) {
            for (a in values) {
                for (b in values) {
                    if (a > b) continue //skip decreasing functions, test method creates them automatically
                    test(theta, a, b)
                }
            }
        }
    }

    private fun test(theta: Double, a: Double, b: Double) {
        val positiveStep = Step(0, theta, a, b, true)
        val negativeStep = Step(0, theta, a, b, false)
        var e = 0.0
        while (e < 10) {
            val expectedP = positiveStep.alternativeEval(e)
            val realP = positiveStep.eval(e)
            assertTrue(
                    Math.abs(expectedP - realP) < error || (expectedP.isNaN() && realP.isNaN()),
                    "Problem in $positiveStep: expected $expectedP, got $realP for value $e")
            val expectedN = negativeStep.alternativeEval(e)
            val realN = negativeStep.eval(e)
            assertTrue(
                    Math.abs(expectedN - realN) < error || (expectedN.isNaN() && realN.isNaN()),
                    "Problem in $negativeStep: expected $expectedN, got $realN for value $e")
            assertTrue(
                    Math.abs(a + b - realP - realN) < error || realP.isNaN() || realN.isNaN(),
                    "Problem in comparison $positiveStep, $negativeStep: positive $realP, negative $realN for value $e")
            e += 0.05
            e *= 1.2
        }
    }

}