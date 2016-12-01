package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.checker.nodesOf
import com.github.sybila.ode.model.Model
import com.github.sybila.ode.model.Summand
import org.junit.Test


class OneDimWithParamGeneratorTest {

    //dv1 = p(v1/2 + 1) - 1
    //This one dimensional model should actually cover most of the behaviour
    //It only fails to cover a steady state in the middle of the model and
    //cases when parameter is multiplied by zero

    //dv2 = p(v1/2 - 2) - 1
    //This model covers the two remaining cases. A stable state and a zero on threshold.

    private val v1 = Model.Variable(
            name = "v1", range = Pair(0.0, 6.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 4.0, 6.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0), constant = 0.5),
                    Summand(paramIndex = 0),
                    Summand(constant = -1.0))
    )

    private val v2 = v1.copy(name = "v2",
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0), constant = 0.5),
                    Summand(paramIndex = 0, constant = -2.0),
                    Summand(constant = -1.0)))

    private val fragmentOne = SMTOdeFragment(Model(listOf(v1), listOf(
            Model.Parameter("p", Pair(0.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    private val fragmentTwo = SMTOdeFragment(Model(listOf(v2), listOf(
            Model.Parameter("p2", Pair(-2.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    private val n0 = IDNode(0)
    private val n1 = IDNode(1)
    private val n2 = IDNode(2)

    private val p = "p".toZ3()
    private val q = "p2".toZ3()
    private val zero = 0.toZ3()
    private val one = 1.toZ3()
    private val two = 2.toZ3()
    private val three = 3.toZ3()
    private val mTwo = (-2).toZ3()
    private val mOne = (-1).toZ3()

    private val c = ff

    /**
     *  WARNING - Because we don't know how to compare <= and < properly, some answers have
     *  non-strict operators that don't make that much sense, but are necessary (self loops)
     */

    @Test
    fun parameterTestOne() {
        val s0 = fragmentOne.successors.invoke(n0)
        assertEquals(nodesOf(c,
                n0 to (p.gt(zero) and p.le(one)).asColors(),
                n1 to (p.gt(one.div(two)) and p.lt(two)).asColors()
        ).normalize(), s0.normalize())
        val s1 = fragmentOne.successors.invoke(n1)
        assertEquals(nodesOf(c,
                n0 to (p.gt(zero) and p.lt(one.div(two))).asColors(),
                n1 to (p.ge(one.div(three)) and p.le(one.div(two))).asColors(),
                n2 to (p.gt(one div three) and p.lt(two)).asColors()
        ).normalize(), s1.normalize())
        val s2 = fragmentOne.successors.invoke(n2)
        assertEquals(nodesOf(c,
                n1 to (p.gt(zero) and p.lt(one.div(three))).asColors(),
                n2 to (p.ge(one.div(4.toZ3())) and p.lt(two)).asColors()
        ).normalize(), s2.normalize())
        val p0 = fragmentOne.predecessors.invoke(n0)
        assertEquals(nodesOf(c,
                n0 to (p.gt(zero) and p.le(one)).asColors(),
                n1 to (p.gt(zero) and p.lt(one.div(two))).asColors()
        ).normalize(), p0.normalize())
        val p1 = fragmentOne.predecessors.invoke(n1)
        assertEquals(nodesOf(c,
                n0 to (p.gt(one.div(two)) and p.lt(two)).asColors(),
                n1 to (p.ge(one.div(three)) and p.le(one.div(two))).asColors(),
                n2 to (p.gt(zero) and p.lt(one.div(three))).asColors()
        ).normalize(), p1.normalize())
        val p2 = fragmentOne.predecessors.invoke(n2)
        assertEquals(nodesOf(c,
                n1 to (p.gt(one.div(three)) and p.lt(two)).asColors(),
                n2 to (p.ge(one.div(4.toZ3())) and p.lt(two)).asColors()
        ).normalize(), p2.normalize())
    }

    @Test
    fun parameterTestTwo() {
        //dv2 = p(v1 - 2) - 1
        //(0) dv2 = p(-2) - 1 p>-1/2 => - // p < -1/2 => +
        //(1) dv2 = p(-1) - 1 p>-1 => - // p < -1 => +
        //(2) dv2 = p(0) - 1 // -1
        //(3) dv2 = p(1) - 1  p<1 => - // p > 1 => +
        val s0 = fragmentTwo.successors.invoke(n0)
        assertEquals(nodesOf(c,
                n0 to (q.ge(mOne) and q.lt(two)).asColors(),
                n1 to (q.gt(mTwo) and q.lt(mOne)).asColors()
        ).normalize(), s0.normalize())
        val s1 = fragmentTwo.successors.invoke(n1)
        assertEquals(nodesOf(c,
                n0 to (q.gt(mOne) and q.lt(two)).asColors(),
                n1 to (q.gt(mTwo) and q.le(mOne)).asColors()
        ).normalize(), s1.normalize())
        val s2 = fragmentTwo.successors.invoke(n2)
        assertEquals(nodesOf(c,
                n1 to (q.gt(mTwo) and q.lt(two)).asColors(),
                n2 to (q.ge(one) and q.lt(two)).asColors()
        ).normalize(), s2.normalize())
        val p0 = fragmentTwo.predecessors.invoke(n0)
        assertEquals(nodesOf(c,
                n0 to (q.ge(mOne) and q.lt(two)).asColors(),
                n1 to (q.gt(mOne) and q.lt(two)).asColors()
        ).normalize(), p0.normalize())
        val p = fragmentTwo.predecessors.invoke(n1)
        assertEquals(nodesOf(c,
                n0 to (q.gt(mTwo) and q.lt(mOne)).asColors(),
                n1 to (q.gt(mTwo) and q.le(mOne)).asColors(),
                n2 to (q.gt(mTwo) and q.lt(two)).asColors()
        ).normalize(), p.normalize())
        val p2 = fragmentTwo.predecessors.invoke(n2)
        assertEquals(nodesOf(c,
                n2 to (q.ge(one) and q.lt(two)).asColors()
        ).normalize(), p2.normalize())
    }

}
