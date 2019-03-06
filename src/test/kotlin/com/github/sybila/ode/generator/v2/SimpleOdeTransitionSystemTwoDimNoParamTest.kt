package com.github.sybila.ode.generator.v2

import com.github.sybila.ode.generator.ExplicitEvaluable
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import kotlin.test.assertEquals

class SimpleOdeTransitionSystemTwoDimNoParamTest {
    private val variable1 = OdeModel.Variable(
            name = "v1", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            summands = Summand(evaluables = ExplicitEvaluable(
                    0, mapOf(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0, 3.0 to 0.0)
            ))
    )

    private val variable2 = OdeModel.Variable(
            name = "v2", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            summands = Summand(evaluables = ExplicitEvaluable(
                    0, mapOf(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0, 3.0 to 0.0)
            ))
    )



    private fun createFragment(vararg values: Double): SimpleOdeTransitionSystem {
        return SimpleOdeTransitionSystem(OdeModel(variable1.copy(equation = listOf(Summand(
                evaluables = ExplicitEvaluable(0,
                        listOf(0.0, 1.0, 2.0, 3.0).zip(values.toList()).toMap()
                )
        )))))
    }

    private fun SimpleOdeTransitionSystem.checkSuccessors(from: Int, to: List<Int>) {
        this.run {
            val s = from.successors().asSequence().toSet()
            assertEquals(to.toSet(), s)
        }
    }

    private fun SimpleOdeTransitionSystem.checkPredecessors(from: Int, to: List<Int>) {
        this.run {
            val s = from.predecessors().asSequence().toSet()
            assertEquals(to.toSet(), s)
        }
    }
}