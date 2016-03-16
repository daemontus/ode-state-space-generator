package com.github.sybila.ode.generator

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.checker.nodesOf
import com.github.sybila.ode.model.Model
import com.github.sybila.ode.model.Summand
import org.junit.Test
import kotlin.test.assertEquals

/**
 * This test suit should provide a really basic way to test how
 * s.s.g behaves in trivial cases of one dimensional model.
 *
 * All test cases rely on a one dimensional model with three states and predefined result.
 **/
class OneDimNoParamGeneratorTest {

    private val variable = Model.Variable(
            name = "v1", range = Pair(0.0, 3.0), varPoints = null,
            thresholds = listOf(0.0, 1.0, 2.0, 3.0),
            summands = Summand(evaluables = ExplicitEvaluable(
                    0, mapOf(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0, 3.0 to 0.0)
            ))
    )

    private fun createFragment(vararg values: Double): RectangleOdeFragment {
        return RectangleOdeFragment(Model(variable.copy(equation = listOf(Summand(
                evaluables = ExplicitEvaluable(0,
                        listOf(0.0, 1.0, 2.0, 3.0).zip(values.toList()).toMap()
                )
        )))), UniformPartitionFunction<IDNode>())
    }

    private val n0 = IDNode(0)
    private val n1 = IDNode(1)
    private val n2 = IDNode(2)

    //A non empty color set contains an empty rectangle
    private val c = RectangleColors(Rectangle(doubleArrayOf()))
    private val e = RectangleColors()

    //ignore symmetric cases, otherwise try as many combinations as possible

//No +/-

    //0..0..0..0
    @Test fun case0() {
        val fragment = createFragment(0.0, 0.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }

//One +

    //+..0..0..0
    @Test fun case1() {
        val fragment = createFragment(1.0, 0.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }

    //0..+..0..0
    @Test fun case2() {
        val fragment = createFragment(0.0, 1.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }

//One -

    //-..0..0..0
    @Test fun case3() {
        val fragment = createFragment(-1.0, 0.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }

    //0..-..0..0
    @Test fun case4() {
        val fragment = createFragment(0.0, -1.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }

//Two +

    //+..+..0..0
    @Test fun case5() {
        val fragment = createFragment(1.0, 1.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //+..0..+..0
    @Test fun case6() {
        val fragment = createFragment(1.0, 0.0, 1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }
    //+..0..0..+
    @Test fun case7() {
        val fragment = createFragment(1.0, 0.0, 0.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //0..+..+..0
    @Test fun case8() {
        val fragment = createFragment(0.0, 1.0, 1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }

//Two -

    //-..-..0..0
    @Test fun case9() {
        val fragment = createFragment(-1.0, -1.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //-..0..-..0
    @Test fun case10() {
        val fragment = createFragment(-1.0, 0.0, -1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //-..0..0..-
    @Test fun case11() {
        val fragment = createFragment(-1.0, 0.0, 0.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //0..-..-..0
    @Test fun case12() {
        val fragment = createFragment(0.0, -1.0, -1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }

//Three +

    //0..+..+..+
    @Test fun case13() {
        val fragment = createFragment(0.0, 1.0, 1.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }
    //+..0..+..+
    @Test fun case14() {
        val fragment = createFragment(1.0, 0.0, 1.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }

//Three -

    //0..-..-..-
    @Test fun case15() {
        val fragment = createFragment(0.0, -1.0, -1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e), p2)
    }
    //-..0..-..-
    @Test fun case16() {
        val fragment = createFragment(-1.0, 0.0, -1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e), p2)
    }

//Four +

    //+..+..+..+
    @Test fun case17() {
        val fragment = createFragment(1.0, 1.0, 1.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c, n2 to c), p2)
    }

//Four -

    //-..-..-..-
    @Test fun case18() {
        val fragment = createFragment(-1.0, -1.0, -1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e), p2)
    }

//One + / One -

    //+..-..0..0
    @Test fun case19() {
        val fragment = createFragment(1.0, -1.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }

    //+..0..-..0
    @Test fun case20() {
        val fragment = createFragment(1.0, 0.0, -1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //+..0..0..-
    @Test fun case21() {
        val fragment = createFragment(1.0, 0.0, 0.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //-..+..0..0
    @Test fun case22() {
        val fragment = createFragment(-1.0, 1.0, 0.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //-..0..+..0
    @Test fun case23() {
        val fragment = createFragment(-1.0, 0.0, 1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }
    //0..+..-..0
    @Test fun case24() {
        val fragment = createFragment(0.0, 1.0, -1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //0..-..+..0
    @Test fun case25() {
        val fragment = createFragment(0.0, -1.0, 1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }

//One + / Two -

    //+..0..-..-
    @Test fun case26() {
        val fragment = createFragment(1.0, 0.0, -1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e), p2)
    }
    //+..-..0..-
    @Test fun case27() {
        val fragment = createFragment(1.0, -1.0, 0.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //+..-..-..0
    @Test fun case28() {
        val fragment = createFragment(1.0, -1.0, -1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //0..+..-..-
    @Test fun case29() {
        val fragment = createFragment(0.0, 1.0, -1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e), p2)
    }
    //-..+..0..-
    @Test fun case30() {
        val fragment = createFragment(-1.0, 1.0, 0.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //-..+..-..0
    @Test fun case31() {
        val fragment = createFragment(-1.0, 1.0, -1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }

//Two + / One -

    //-..0..+..+
    @Test fun case32() {
        val fragment = createFragment(-1.0, 0.0, 1.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }
    //-..+..0..+
    @Test fun case33() {
        val fragment = createFragment(-1.0, 1.0, 0.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //-..+..+..0
    @Test fun case34() {
        val fragment = createFragment(-1.0, 1.0, 1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }
    //0..-..+..+
    @Test fun case35() {
        val fragment = createFragment(0.0, -1.0, 1.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }
    //+..-..0..+
    @Test fun case36() {
        val fragment = createFragment(1.0, -1.0, 0.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), p2)
    }
    //+..-..+..0
    @Test fun case37() {
        val fragment = createFragment(1.0, -1.0, 1.0, 0.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }

//Two + / Two -

    //+..+..-..-
    @Test fun case38() {
        val fragment = createFragment(1.0, 1.0, -1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e), p2)
    }
    //+..-..+..-
    @Test fun case39() {
        val fragment = createFragment(1.0, -1.0, 1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }

//Three + / One -

    //-..+..+..+
    @Test fun case40() {
        val fragment = createFragment(-1.0, 1.0, 1.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }
    //+..-..+..+
    @Test fun case41() {
        val fragment = createFragment(1.0, -1.0, 1.0, 1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n0 to c, n2 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n2 to c, n1 to c), p2)
    }

//Three - / One +

    //+..-..-..-
    @Test fun case42() {
        val fragment = createFragment(1.0, -1.0, -1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n0 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n2 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e), p2)
    }
    //-..+..-..-
    @Test fun case43() {
        val fragment = createFragment(-1.0, 1.0, -1.0, -1.0)
        val s0 = fragment.successors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c, n1 to c), s0)
        val s1 = fragment.successors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c), s1)
        val s2 = fragment.successors.invoke(IDNode(2))
        assertEquals(nodesOf(e, n1 to c), s2)
        val p0 = fragment.predecessors.invoke(IDNode(0))
        assertEquals(nodesOf(e, n0 to c), p0)
        val p1 = fragment.predecessors.invoke(IDNode(1))
        assertEquals(nodesOf(e, n1 to c, n2 to c, n0 to c), p1)
        val p2 = fragment.predecessors.invoke(IDNode(2))
        assertEquals(nodesOf(e), p2)
    }

}