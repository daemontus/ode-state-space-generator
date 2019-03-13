package com.github.sybila.ode.generator.v2

import com.github.sybila.ode.generator.ExplicitEvaluable
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertEquals

class SimpleOdeTransitionSystemTwoDimNoParamTest {
    private val variable1 = OdeModel.Variable(
            name = "v1", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(-1.0, 0.0, 1.0, 2.0),
            summand = Summand(evaluable = ExplicitEvaluable(
                    0, mapOf(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0, 3.0 to 0.0)
            ))
    )

    private val variable2 = OdeModel.Variable(
            name = "v2", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(1.0, 2.0, 3.0, 4.0),
            summand = Summand(evaluable = ExplicitEvaluable(
                    0, mapOf(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0, 3.0 to 0.0)
            ))
    )

    private fun createFragment(valuesV1: List<Double>, valuesV2: List<Double>): SimpleOdeTransitionSystem {
        return SimpleOdeTransitionSystem(OdeModel(
                variable1.copy(equation = listOf(Summand(
                    evaluable = ExplicitEvaluable(1,
                        listOf(1.0, 2.0, 3.0, 4.0).zip(valuesV1).toMap()
                    )
                ))),
                variable2.copy(equation = listOf(Summand(
                    evaluable = ExplicitEvaluable(0,
                        listOf(-1.0, 0.0, 1.0, 2.0).zip(valuesV2).toMap()
                    )
                )))
        ))
    }

    private fun SimpleOdeTransitionSystem.checkSuccessors(from: Int, to: Set<Int>) {
        this.run {
            val s = from.successors().asSequence().toSet()
            assertEquals(to, s.toSet())
        }
    }

    private fun SimpleOdeTransitionSystem.checkPredecessors(from: Int, to: Set<Int>) {
        this.run {
            val s = from.predecessors().asSequence().toSet()
            assertEquals(to, s.toSet())
        }
    }

    /*
     * State space:
     *
     * ------------- 2.0
     * | 6 | 7 | 8 |
     * ------------- 1.0
     * | 3 | 4 | 5 |
     * ------------- 0.0
     * | 0 | 1 | 2 |
     * ------------- -1.0
     * 1.0 2.0 3.0 4.0
     */

    @Test
    fun case0() {
        createFragment(listOf(0.0, 0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0, 0.0)).run {
            for (state in 0..8) {
                checkSuccessors(state, setOf(state))
                checkPredecessors(state, setOf(state))
            }
        }
    }

    /*
         <-
        |  ^
        v  |
         ->
     */
    @Test
    fun case1() {
        createFragment(listOf(1.0, 1.0, -1.0, -1.0), listOf(-1.0, -1.0, 1.0, 1.0)).run {
            checkSuccessors(0, setOf(1))
            checkSuccessors(1, setOf(2, 4))
            checkSuccessors(2, setOf(5))
            checkSuccessors(3, setOf(4, 0))
            checkSuccessors(4, setOf(1, 3, 4, 5, 7))
            checkSuccessors(5, setOf(4, 8))
            checkSuccessors(6, setOf(3))
            checkSuccessors(7, setOf(4, 6))
            checkSuccessors(8, setOf(7))
        }
    }

    @Test
    fun case2() {
        createFragment(listOf(1.0, 1.0, 1.0, 1.0), listOf(-1.0, -1.0, -1.0, -1.0)).run {

        }
    }
}