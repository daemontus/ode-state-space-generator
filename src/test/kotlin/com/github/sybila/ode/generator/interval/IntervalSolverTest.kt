package com.github.sybila.ode.generator.interval

import org.junit.Test
import java.util.*

class IntervalSolverTest {

    //All strict tests have three parts:
    //1. Non-equal intersection - l1..l2..h1..h2
    //2. One is subset - l1..l2..h2..l1
    //3. No intersection - l1..h1 l2..h2

    //All equal tests have three parts:
    //1. Invariant - l1=l2..h1=h2
    //2. No intersection - l1..h1=l2..h2
    //3. One is subset - l1=l2..h1..h2 || l1..l2..h1=h2

    private fun assertEquals(expected: DoubleArray, actual: DoubleArray) {
        kotlin.test.assertEquals(expected.toList(), actual.toList())
    }

    @Test
    fun strictTimesTest() {
        IntervalSolver(0.0,3.0).run {
            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(0.0, 2.0) and doubleArrayOf(1.0, 3.0))
            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 3.0) and doubleArrayOf(0.0, 2.0))

            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(0.0, 3.0) and doubleArrayOf(1.0, 2.0))
            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 2.0) and doubleArrayOf(0.0, 3.0))

            assertEquals(doubleArrayOf(), doubleArrayOf(0.0, 1.0) and doubleArrayOf(2.0, 3.0))
            assertEquals(doubleArrayOf(), doubleArrayOf(2.0, 3.0) and doubleArrayOf(0.0, 1.0))
        }
    }

    @Test
    fun equalTimesTest() {
        IntervalSolver(0.0, 3.0).run {
            /*val a = doubleArrayOf(0.0,1.0)
            val b = doubleArrayOf(1.0,2.0)
            println(a.not().prettyPrint())
            println(b.not().prettyPrint())
            println((a.not() or b.not()).prettyPrint())*/
            assertEquals(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 1.0) and doubleArrayOf(0.0, 1.0))

            assertEquals(doubleArrayOf(), doubleArrayOf(0.0, 1.0) and doubleArrayOf(1.0, 2.0))
            assertEquals(doubleArrayOf(), doubleArrayOf(1.0, 2.0) and doubleArrayOf(0.0, 1.0))

            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 2.0) and doubleArrayOf(1.0, 3.0))
            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 3.0) and doubleArrayOf(1.0, 2.0))
            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 2.0) and doubleArrayOf(0.0, 2.0))
            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(0.0, 2.0) and doubleArrayOf(1.0, 2.0))
        }
    }

    @Test
    fun strictPlusTest() {
        IntervalSolver(0.0, 3.0).run {
            assertEquals(doubleArrayOf(0.0, 3.0), doubleArrayOf(0.0, 2.0) or doubleArrayOf(1.0, 3.0))
            assertEquals(doubleArrayOf(0.0, 3.0), doubleArrayOf(1.0, 3.0) or doubleArrayOf(0.0, 2.0))

            assertEquals(doubleArrayOf(0.0, 3.0), doubleArrayOf(0.0, 3.0) or doubleArrayOf(1.0, 2.0))
            assertEquals(doubleArrayOf(0.0, 3.0), doubleArrayOf(1.0, 2.0) or doubleArrayOf(0.0, 3.0))

            assertEquals(doubleArrayOf(0.0,1.0,2.0,3.0), doubleArrayOf(0.0, 1.0) or doubleArrayOf(2.0, 3.0))
            assertEquals(doubleArrayOf(0.0,1.0,2.0,3.0), doubleArrayOf(2.0, 3.0) or doubleArrayOf(0.0, 1.0))
        }
    }

    @Test
    fun equalPlusTest() {
        IntervalSolver(0.0, 3.0).run {
            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 2.0) or doubleArrayOf(1.0, 2.0))

            assertEquals(doubleArrayOf(0.0, 2.0), doubleArrayOf(0.0, 1.0) or doubleArrayOf(1.0, 2.0))
            assertEquals(doubleArrayOf(0.0, 2.0), doubleArrayOf(1.0, 2.0) or doubleArrayOf(0.0, 1.0))

            assertEquals(doubleArrayOf(1.0, 3.0), doubleArrayOf(1.0, 2.0) or doubleArrayOf(1.0, 3.0))
            assertEquals(doubleArrayOf(1.0, 3.0), doubleArrayOf(1.0, 3.0) or doubleArrayOf(1.0, 2.0))
            assertEquals(doubleArrayOf(0.0, 2.0), doubleArrayOf(1.0, 2.0) or doubleArrayOf(0.0, 2.0))
            assertEquals(doubleArrayOf(0.0, 2.0), doubleArrayOf(0.0, 2.0) or doubleArrayOf(1.0, 2.0))
        }
    }

    @Test
    fun strictMinusTest() {
        IntervalSolver(0.0,3.0).run {
            assertEquals(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 2.0) and doubleArrayOf(1.0, 3.0).not())
            assertEquals(doubleArrayOf(2.0, 3.0), doubleArrayOf(1.0, 3.0) and doubleArrayOf(0.0, 2.0).not())

            assertEquals(doubleArrayOf(0.0, 1.0, 2.0, 3.0), doubleArrayOf(0.0, 3.0) and doubleArrayOf(1.0, 2.0).not())
            assertEquals(doubleArrayOf(), doubleArrayOf(1.0, 2.0) and doubleArrayOf(0.0, 3.0).not())

            assertEquals(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 1.0) and doubleArrayOf(2.0, 3.0).not())
            assertEquals(doubleArrayOf(2.0, 3.0), doubleArrayOf(2.0, 3.0) and doubleArrayOf(0.0, 1.0).not())
        }
    }

    @Test
    fun equalMinusTest() {
        IntervalSolver(0.0, 3.0).run {

            assertEquals(doubleArrayOf(), doubleArrayOf(0.0, 1.0) and doubleArrayOf(0.0, 1.0).not())

            assertEquals(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 1.0) and doubleArrayOf(1.0, 2.0).not())
            assertEquals(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 2.0) and doubleArrayOf(0.0, 1.0).not())

            assertEquals(doubleArrayOf(), doubleArrayOf(1.0, 2.0) and doubleArrayOf(1.0, 3.0).not())
            assertEquals(doubleArrayOf(2.0, 3.0), doubleArrayOf(1.0, 3.0) and doubleArrayOf(1.0, 2.0).not())
            assertEquals(doubleArrayOf(), doubleArrayOf(1.0, 2.0) and doubleArrayOf(0.0, 2.0).not())
            assertEquals(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 2.0) and doubleArrayOf(1.0, 2.0).not())
        }
    }

}