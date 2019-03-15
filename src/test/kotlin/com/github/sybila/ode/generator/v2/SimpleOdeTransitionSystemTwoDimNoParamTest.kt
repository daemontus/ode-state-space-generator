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

            checkPredecessors(0, setOf(3))
            checkPredecessors(1, setOf(0, 4))
            checkPredecessors(2, setOf(1))
            checkPredecessors(3, setOf(4, 6))
            checkPredecessors(4, setOf(1, 3, 4, 5, 7))
            checkPredecessors(5, setOf(2, 4))
            checkPredecessors(6, setOf(7))
            checkPredecessors(7, setOf(4, 8))
            checkPredecessors(8, setOf(5))
        }
    }

    /*
         ->
        ^  |
        |  v
         <-
    */
    @Test
    fun case2() {
        createFragment(listOf(-1.0, -1.0, 1.0, 1.0), listOf(1.0, 1.0, -1.0, -1.0)).run {
            checkSuccessors(0, setOf(3))
            checkSuccessors(1, setOf(0, 4))
            checkSuccessors(2, setOf(1))
            checkSuccessors(3, setOf(4, 6))
            checkSuccessors(4, setOf(1, 3, 4, 5, 7))
            checkSuccessors(5, setOf(4, 2))
            checkSuccessors(6, setOf(7))
            checkSuccessors(7, setOf(4, 8))
            checkSuccessors(8, setOf(5))

            checkPredecessors(0, setOf(1))
            checkPredecessors(1, setOf(2, 4))
            checkPredecessors(2, setOf(5))
            checkPredecessors(3, setOf(0, 4))
            checkPredecessors(4, setOf(1, 3, 4, 5, 7))
            checkPredecessors(5, setOf(4, 8))
            checkPredecessors(6, setOf(3))
            checkPredecessors(7, setOf(4, 6))
            checkPredecessors(8, setOf(7))
        }
    }

    @Test
    fun case3() {
        createFragment(listOf(1.0, 1.0, 1.0, 1.0), listOf(-1.0, -1.0, -1.0, -1.0)).run {
            checkSuccessors(0, setOf(1))
            checkSuccessors(1, setOf(2))
            checkSuccessors(2, setOf(2))
            checkSuccessors(3, setOf(4, 0))
            checkSuccessors(4, setOf(1, 5))
            checkSuccessors(5, setOf(2))
            checkSuccessors(6, setOf(3, 7))
            checkSuccessors(7, setOf(4, 8))
            checkSuccessors(8, setOf(5))

            checkPredecessors(0, setOf(3))
            checkPredecessors(1, setOf(0, 4))
            checkPredecessors(2, setOf(1, 2, 5))
            checkPredecessors(3, setOf(6))
            checkPredecessors(4, setOf(3, 7))
            checkPredecessors(5, setOf(4, 8))
            checkPredecessors(6, setOf())
            checkPredecessors(7, setOf(6))
            checkPredecessors(8, setOf(7))
        }
    }

    @Test
    fun case4() {
        createFragment(listOf(-1.0, -1.0, -1.0, -1.0), listOf(1.0, 1.0, 1.0, 1.0)).run {
            checkSuccessors(0, setOf(3))
            checkSuccessors(1, setOf(0, 4))
            checkSuccessors(2, setOf(1, 5))
            checkSuccessors(3, setOf(6))
            checkSuccessors(4, setOf(3, 7))
            checkSuccessors(5, setOf(4, 8))
            checkSuccessors(6, setOf(6))
            checkSuccessors(7, setOf(6))
            checkSuccessors(8, setOf(7))

            checkPredecessors(0, setOf(1))
            checkPredecessors(1, setOf(2))
            checkPredecessors(2, setOf())
            checkPredecessors(3, setOf(0, 4))
            checkPredecessors(4, setOf(1, 5))
            checkPredecessors(5, setOf(2))
            checkPredecessors(6, setOf(3, 6, 7))
            checkPredecessors(7, setOf(4, 8))
            checkPredecessors(8, setOf(5))
        }
    }

    @Test
    fun case5() {
        createFragment(listOf(1.0, 1.0, 1.0, 1.0), listOf(1.0, 1.0, 1.0, 1.0)).run {
            checkSuccessors(0, setOf(1, 3))
            checkSuccessors(1, setOf(2, 4))
            checkSuccessors(2, setOf(5))
            checkSuccessors(3, setOf(4, 6))
            checkSuccessors(4, setOf(5, 7))
            checkSuccessors(5, setOf(8))
            checkSuccessors(6, setOf(7))
            checkSuccessors(7, setOf(8))
            checkSuccessors(8, setOf(8))

            checkPredecessors(0, setOf())
            checkPredecessors(1, setOf(0))
            checkPredecessors(2, setOf(1))
            checkPredecessors(3, setOf(0))
            checkPredecessors(4, setOf(1, 3))
            checkPredecessors(5, setOf(2, 4))
            checkPredecessors(6, setOf(3))
            checkPredecessors(7, setOf(4, 6))
            checkPredecessors(8, setOf(5, 7, 8))
        }
    }

    @Test
    fun case6() {
        createFragment(listOf(-1.0, -1.0, -1.0, -1.0), listOf(-1.0, -1.0, -1.0, -1.0)).run {
            checkSuccessors(0, setOf(0))
            checkSuccessors(1, setOf(0))
            checkSuccessors(2, setOf(1))
            checkSuccessors(3, setOf(0))
            checkSuccessors(4, setOf(1, 3))
            checkSuccessors(5, setOf(2, 4))
            checkSuccessors(6, setOf(3))
            checkSuccessors(7, setOf(4, 6))
            checkSuccessors(8, setOf(5, 7))

            checkPredecessors(0, setOf(0, 1, 3))
            checkPredecessors(1, setOf(2, 4))
            checkPredecessors(2, setOf(5))
            checkPredecessors(3, setOf(4, 6))
            checkPredecessors(4, setOf(5, 7))
            checkPredecessors(5, setOf(8))
            checkPredecessors(6, setOf(7))
            checkPredecessors(7, setOf(8))
            checkPredecessors(8, setOf())
        }
    }

    @Test
    fun case7() {
        createFragment(listOf(1.0, -1.0, 1.0, -1.0), listOf(1.0, -1.0, 1.0, -1.0)).run {
            checkSuccessors(0, setOf(0, 1, 3))
            checkSuccessors(1, setOf(0, 1, 2, 4))
            checkSuccessors(2, setOf(1, 5, 2))
            checkSuccessors(3, setOf(0, 3, 4, 6))
            checkSuccessors(4, setOf(1, 3, 4, 5, 7))
            checkSuccessors(5, setOf(2, 4, 5, 8))
            checkSuccessors(6, setOf(3, 6, 7))
            checkSuccessors(7, setOf(4, 6, 7, 8))
            checkSuccessors(8, setOf(5, 7, 8))

            checkPredecessors(0, setOf(0, 1, 3))
            checkPredecessors(1, setOf(0, 1, 2, 4))
            checkPredecessors(2, setOf(1, 2, 5))
            checkPredecessors(3, setOf(0, 3, 4, 6))
            checkPredecessors(4, setOf(1, 3, 4, 5, 7))
            checkPredecessors(5, setOf(2, 4, 5, 8))
            checkPredecessors(6, setOf(3, 6, 7))
            checkPredecessors(7, setOf(4, 6, 7, 8))
            checkPredecessors(8, setOf(5, 7, 8))
        }
    }

    @Test
    fun case8() {
        createFragment(listOf(-1.0, 1.0, -1.0, 1.0), listOf(-1.0, 1.0, -1.0, 1.0)).run {
            checkSuccessors(0, setOf(0, 1, 3))
            checkSuccessors(1, setOf(0, 1, 2, 4))
            checkSuccessors(2, setOf(1, 5, 2))
            checkSuccessors(3, setOf(0, 3, 4, 6))
            checkSuccessors(4, setOf(1, 3, 4, 5, 7))
            checkSuccessors(5, setOf(2, 4, 5, 8))
            checkSuccessors(6, setOf(3, 6, 7))
            checkSuccessors(7, setOf(4, 6, 7, 8))
            checkSuccessors(8, setOf(5, 7, 8))

            checkPredecessors(0, setOf(0, 1, 3))
            checkPredecessors(1, setOf(0, 1, 2, 4))
            checkPredecessors(2, setOf(1, 2, 5))
            checkPredecessors(3, setOf(0, 3, 4, 6))
            checkPredecessors(4, setOf(1, 3, 4, 5, 7))
            checkPredecessors(5, setOf(2, 4, 5, 8))
            checkPredecessors(6, setOf(3, 6, 7))
            checkPredecessors(7, setOf(4, 6, 7, 8))
            checkPredecessors(8, setOf(5, 7, 8))
        }
    }

    @Test
    fun case9() {
        createFragment(listOf(1.0, 1.0, 1.0, 1.0), listOf(-1.0, 1.0, 1.0, 1.0)).run {
            checkSuccessors(0, setOf(1, 3))
            checkSuccessors(1, setOf(2, 4))
            checkSuccessors(2, setOf(5))
            checkSuccessors(3, setOf(0, 4, 6)) //why 0?
            checkSuccessors(4, setOf(5, 7))
            checkSuccessors(5, setOf(8))
            checkSuccessors(6, setOf(3, 7)) //why 3?
            checkSuccessors(7, setOf(8))
            checkSuccessors(8, setOf(8))

            checkPredecessors(0, setOf(3))
            checkPredecessors(1, setOf(0))
            checkPredecessors(2, setOf(1))
            checkPredecessors(3, setOf(0, 6))
            checkPredecessors(4, setOf(1, 3))
            checkPredecessors(5, setOf(2, 4))
            checkPredecessors(6, setOf(3))
            checkPredecessors(7, setOf(4, 6))
            checkPredecessors(8, setOf(5, 7, 8))
        }
    }
}