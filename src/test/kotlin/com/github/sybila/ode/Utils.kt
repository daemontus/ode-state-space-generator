package com.github.sybila.ode

import com.github.sybila.checker.Solver
import com.github.sybila.checker.Transition

fun <P: Any> Solver<P>.assertTransitionEquals(t1: Transition<P>, t2: Transition<P>) {
    if (t1.target != t2.target || t1.direction != t2.direction || !(t1.bound equals t2.bound)) {
        throw AssertionError("Expected $t1, but got $t2")
    }
}

fun <P: Any> Solver<P>.assertTransitionEquals(t1: Iterator<Transition<P>>, vararg t2: Transition<P>) {
    val actual = t1.asSequence().groupBy { it.target }
    val expected = t2.asSequence().groupBy { it.target }
    if (expected.keys != actual.keys) {
        throw AssertionError("Expected ${expected.values}, got ${actual.values} ${expected.keys} ${actual.keys}")
    }
    expected.keys.forEach {
        val ac = actual[it]!!
        for (e in expected[it]!!) {
            val a = ac.find { it.direction == e.direction } ?:
                    throw AssertionError("Expected ${expected.values}, got ${actual.values}")
            assertTransitionEquals(e, a)
        }
    }
}

fun <P: Any> Solver<P>.assertDeepEquals(expected: P, actual: P) {
    if (!(expected equals actual)) {
        throw AssertionError("Expected $expected, but got $actual")
    }
}