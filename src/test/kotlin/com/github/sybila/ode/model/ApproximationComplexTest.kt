package com.github.sybila.ode.model

import org.junit.Test
import kotlin.test.assertTrue

private val error = Math.pow(10.0, -4.0)

class ApproximationComplexTest {

    @Test
    fun hillComplexTest() {

        val model = Model(listOf(
                Model.Variable("v1", Pair(0.0, 10.0), listOf(0.0, 10.0), Pair(8000, 10), listOf(
                        Summand(2.4, -1, listOf(0, 1), listOf(simpleHill.copy(varIndex = 1)))
                )),
                Model.Variable("v2", Pair(0.0, 10.0), listOf(0.0, 10.0), Pair(8000, 10), listOf(
                        Summand(2.4, -1, listOf(0, 1), listOf(complexHill))
                ))
        ), listOf()).computeApproximation(fast = true, cutToRange = true)

        val complexThresholds = listOf(0.0, 0.000802176, 1.38616, 1.73671, 2.03833, 3.56728, 4.11997, 4.66064, 5.21735, 5.80053, 6.4166, 10.0)
        val simpleThresholds = listOf(0.0, 1.02125, 1.57345, 2.13002, 2.71021, 3.32279, 3.973, 4.66346, 5.3968, 6.17477, 7.0, 10.0)

        val expected = Model(listOf(
                Model.Variable("v1", Pair(0.0, 10.0), complexThresholds, Pair(8000, 10), listOf(
                        Summand(2.4, -1, listOf(0, 1), listOf(
                                RampApproximation(1,
                                        simpleThresholds.toDoubleArray(),
                                        simpleThresholds.map { simpleHill.eval(it) }.toDoubleArray()
                                )
                        ))
                )),
                Model.Variable("v2", Pair(0.0, 10.0), simpleThresholds, Pair(8000, 10), listOf(
                        Summand(2.4, -1, listOf(0, 1), listOf(
                                RampApproximation(0,
                                        complexThresholds.toDoubleArray(),
                                        complexThresholds.map { complexHill.eval(it) }.toDoubleArray()
                                )
                        ))
                ))
        ), listOf())

        assertTrue(model.weakEq(expected), "Models don't match. Expected $expected, got $model")
    }

    fun Model.weakEq(other: Model): Boolean {
        return this.parameters == other.parameters && this.variables.zip(other.variables).all { it.first.weakEq(it.second) }
    }

    fun Model.Variable.weakEq(other: Model.Variable): Boolean {
        return this.thresholds.zip(other.thresholds).all { if (Math.abs(it.first - it.second) > error) throw IllegalStateException("$it") else true }
                && this.name == other.name
                //&& this.range == other.range
                && this.varPoints == other.varPoints
                && this.equation.zip(other.equation).all { it.first.weakEq(it.second) }
    }

    fun Summand.weakEq(other: Summand): Boolean {
        return Math.abs(this.constant - other.constant) < error
            && this.paramIndex == other.paramIndex
            && this.variableIndices == other.variableIndices
            && this.evaluable.zip(other.evaluable).all { it.first.weakEq(it.second) }
    }

    fun Evaluable.weakEq(other: Evaluable): Boolean {
        return this is RampApproximation && other is RampApproximation
            && this.thresholds.zip(other.thresholds).all { if (Math.abs(it.first - it.second) > error) throw IllegalStateException("$it") else true }
            && this.values.zip(other.values).all { if (Math.abs(it.first - it.second) > error) throw IllegalStateException("$it") else true }
    }

}