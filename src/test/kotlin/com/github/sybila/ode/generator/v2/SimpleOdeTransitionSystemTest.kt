package com.github.sybila.ode.generator.v2

import com.github.sybila.ode.generator.ExplicitEvaluable
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertEquals

class SimpleOdeTransitionSystemTest {
    private val variable = OdeModel.Variable(
            name = "v1", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            summands = Summand(evaluables = ExplicitEvaluable(
                    0, mapOf(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0, 3.0 to 0.0)
            ))
    )

    private fun createFragment(vararg values: Double): SimpleOdeTransitionSystem {
        return SimpleOdeTransitionSystem(OdeModel(variable.copy(equation = listOf(Summand(
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

    //No +/-

    //0..0..0..0
    @Test
    fun case0() {
        createFragment(0.0, 0.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }

//One +

    //+..0..0..0
    @Test
    fun case1() {
        createFragment(1.0, 0.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }

    //0..+..0..0
    @Test
    fun case2() {
        createFragment(0.0, 1.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0,1))
            checkPredecessors(2, listOf(2))
        }
    }

//One -

    //-..0..0..0
    @Test
    fun case3() {
        createFragment(-1.0, 0.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }

    //0..-..0..0
    @Test
    fun case4() {
        createFragment(0.0, -1.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }

//Two +

    //+..+..0..0
    @Test
    fun case5() {
        createFragment(1.0, 1.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf())
            checkPredecessors(1, listOf(0,1))
            checkPredecessors(2, listOf(2))
        }
    }
    //+..0..+..0
    @Test
    fun case6() {
        createFragment(1.0, 0.0, 1.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }
    //+..0..0..+
    @Test
    fun case7() {
        createFragment(1.0, 0.0, 0.0, 1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }
    //0..+..+..0
    @Test
    fun case8() {
        createFragment(0.0, 1.0, 1.0, 0.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0))
            checkPredecessors(2, listOf(1,2))
        }
    }

//Two -

    //-..-..0..0
    @Test
    fun case9() {
        createFragment(-1.0, -1.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }
    //-..0..-..0
    @Test
    fun case10() {
        createFragment(-1.0, 0.0, -1.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1,2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1,2))
            checkPredecessors(2, listOf(2))
        }
    }
    //-..0..0..-
    @Test
    fun case11() {
        createFragment(-1.0, 0.0, 0.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }
    //0..-..-..0
    @Test
    fun case12() {
        createFragment(0.0, -1.0, -1.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0))
            checkSuccessors(2, listOf(1,2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(2))
            checkPredecessors(2, listOf(2))
        }
    }

//Three +

    //0..+..+..+
    @Test
    fun case13() {
        createFragment(0.0, 1.0, 1.0, 1.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0))
            checkPredecessors(2, listOf(1,2))
        }
    }
    //+..0..+..+
    @Test
    fun case14() {
        createFragment(1.0, 0.0, 1.0, 1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }

//Three -

    //0..-..-..-
    @Test
    fun case15() {
        createFragment(0.0, -1.0, -1.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0))
            checkSuccessors(2, listOf(1))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(2))
            checkPredecessors(2, listOf())
        }
    }
    //-..0..-..-
    @Test
    fun case16() {
        createFragment(-1.0, 0.0, -1.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1,2))
            checkPredecessors(2, listOf())
        }
    }

//Four +

    //+..+..+..+
    @Test
    fun case17() {
        createFragment(1.0, 1.0, 1.0, 1.0).run {
            checkSuccessors(0, listOf(1))
            checkSuccessors(1, listOf(2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf())
            checkPredecessors(1, listOf(0))
            checkPredecessors(2, listOf(1,2))
        }
    }

//Four -

    //-..-..-..-
    @Test
    fun case18() {
        createFragment(-1.0, -1.0, -1.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0))
            checkSuccessors(2, listOf(1))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(2))
            checkPredecessors(2, listOf())
        }
    }

//One + / One -

    //+..-..0..0
    @Test
    fun case19() {
        createFragment(1.0, -1.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }

    //+..0..-..0
    @Test
    fun case20() {
        createFragment(1.0, 0.0, -1.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1,2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1,2))
            checkPredecessors(2, listOf(2))
        }
    }
    //+..0..0..-
    @Test
    fun case21() {
        createFragment(1.0, 0.0, 0.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }
    //-..+..0..0
    @Test
    fun case22() {
        createFragment(-1.0, 1.0, 0.0, 0.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0,1))
            checkPredecessors(2, listOf(2))
        }
    }
    //-..0..+..0
    @Test
    fun case23() {
        createFragment(-1.0, 0.0, 1.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }
    //0..+..-..0
    @Test
    fun case24() {
        createFragment(0.0, 1.0, -1.0, 0.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1,2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0,1,2))
            checkPredecessors(2, listOf(2))
        }
    }
    //0..-..+..0
    @Test
    fun case25() {
        createFragment(0.0, -1.0, 1.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }

//One + / Two -

    //+..0..-..-
    @Test
    fun case26() {
        createFragment(1.0, 0.0, -1.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1,2))
            checkPredecessors(2, listOf())
        }
    }
    //+..-..0..-
    @Test
    fun case27() {
        createFragment(1.0, -1.0, 0.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }
    //+..-..-..0
    @Test
    fun case28() {
        createFragment(1.0, -1.0, -1.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0))
            checkSuccessors(2, listOf(1,2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(2))
            checkPredecessors(2, listOf(2))
        }
    }
    //0..+..-..-
    @Test
    fun case29() {
        createFragment(0.0, 1.0, -1.0, -1.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0,1,2))
            checkPredecessors(2, listOf())
        }
    }
    //-..+..0..-
    @Test
    fun case30() {
        createFragment(-1.0, 1.0, 0.0, -1.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0,1))
            checkPredecessors(2, listOf(2))
        }
    }
    //-..+..-..0
    @Test
    fun case31() {
        createFragment(-1.0, 1.0, -1.0, 0.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1,2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0,1,2))
            checkPredecessors(2, listOf(2))
        }
    }

//Two + / One -

    //-..0..+..+
    @Test
    fun case32() {
        createFragment(-1.0, 0.0, 1.0, 1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }
    //-..+..0..+
    @Test
    fun case33() {
        createFragment(-1.0, 1.0, 0.0, 1.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0,1))
            checkPredecessors(2, listOf(2))
        }
    }
    //-..+..+..0
    @Test
    fun case34() {
        createFragment(-1.0, 1.0, 1.0, 0.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0))
            checkPredecessors(2, listOf(1,2))
        }
    }
    //0..-..+..+
    @Test
    fun case35() {
        createFragment(0.0, -1.0, 1.0, 1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }
    //+..-..0..+
    @Test
    fun case36() {
        createFragment(1.0, -1.0, 0.0, 1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(2))
        }
    }
    //+..-..+..0
    @Test
    fun case37() {
        createFragment(1.0, -1.0, 1.0, 0.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }

//Two + / Two -

    //+..+..-..-
    @Test
    fun case38() {
        createFragment(1.0, 1.0, -1.0, -1.0).run {
            checkSuccessors(0, listOf(1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1))
            checkPredecessors(0, listOf())
            checkPredecessors(1, listOf(0,1,2))
            checkPredecessors(2, listOf())
        }
    }
    //+..-..+..-
    @Test
    fun case39() {
        createFragment(1.0, -1.0, 1.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }

//Three + / One -

    //-..+..+..+
    @Test
    fun case40() {
        createFragment(-1.0, 1.0, 1.0, 1.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0))
            checkPredecessors(2, listOf(1,2))
        }
    }
    //+..-..+..+
    @Test
    fun case41() {
        createFragment(1.0, -1.0, 1.0, 1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0,1,2))
            checkSuccessors(2, listOf(2))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(1))
            checkPredecessors(2, listOf(1,2))
        }
    }

//Three - / One +

    //+..-..-..-
    @Test
    fun case42() {
        createFragment(1.0, -1.0, -1.0, -1.0).run {
            checkSuccessors(0, listOf(0))
            checkSuccessors(1, listOf(0))
            checkSuccessors(2, listOf(1))
            checkPredecessors(0, listOf(0,1))
            checkPredecessors(1, listOf(2))
            checkPredecessors(2, listOf())
        }
    }
    //-..+..-..-
    @Test
    fun case43() {
        createFragment(-1.0, 1.0, -1.0, -1.0).run {
            checkSuccessors(0, listOf(0,1))
            checkSuccessors(1, listOf(1))
            checkSuccessors(2, listOf(1))
            checkPredecessors(0, listOf(0))
            checkPredecessors(1, listOf(0,1,2))
            checkPredecessors(2, listOf())
        }
    }
}