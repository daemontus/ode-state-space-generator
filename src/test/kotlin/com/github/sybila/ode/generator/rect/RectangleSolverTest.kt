package com.github.sybila.ode.generator.rect

import com.github.sybila.ode.assertDeepEquals
import org.junit.Test
import kotlin.test.assertTrue


class RectangleSolverTest {

    @Test
    fun emptinessTest() {
        RectangleSolver(rectangleOf(0.0, 1.0)).run {
            assertTrue(ff.isNotSat())
            assertTrue(tt.isSat())
        }
    }

    @Test
    fun andTest() {
        RectangleSolver(rectangleOf(0.0, 3.0)).run {
            val c1 = rectangleOf(0.0, 1.0).asParams()
            val c2 = rectangleOf(1.0, 2.5).asParams()
            val c3 = mutableSetOf(rectangleOf(0.0, 1.0), rectangleOf(2.0, 3.0))

            val r = rectangleOf(2.0, 2.5).asParams()

            assertDeepEquals(c1, c1 and c1)

            assertTrue((c1 and c2).isNotSat())
            assertTrue((c2 and c1).isNotSat())

            assertDeepEquals(c1, c1 and c3)
            assertDeepEquals(c1, c3 and c1)

            assertDeepEquals(r, c2 and c3)
            assertDeepEquals(r, c3 and c2)
        }

    }

    @Test
    fun plusTest() {
        RectangleSolver(rectangleOf(0.0, 3.0)).run {
            val c1 = rectangleOf(0.0, 1.0).asParams()
            val c2 = rectangleOf(1.0, 2.5).asParams()
            val c3 = mutableSetOf(rectangleOf(0.0, 1.0), rectangleOf(2.0, 3.0))

            val r1 = rectangleOf(0.0, 2.5).asParams()
            val r2 = rectangleOf(0.0, 3.0).asParams()

            assertDeepEquals(c1, c1 or c1)

            assertDeepEquals(r1, c1 or c2)
            assertDeepEquals(r1, c2 or c1)

            assertDeepEquals(c3, c1 or c3)
            assertDeepEquals(c3, c3 or c1)

            assertDeepEquals(r2, c2 or c3)
            assertDeepEquals(r2, c3 or c2)
        }
    }

    @Test
    fun minusTest() {
        RectangleSolver(rectangleOf(0.0, 3.0)).run {
            val c1 = rectangleOf(0.0, 1.0).asParams()
            val c2 = rectangleOf(1.0, 2.5).asParams()
            val c3 = mutableSetOf(rectangleOf(0.0, 1.5), rectangleOf(2.0, 3.0))

            val r1 = mutableSetOf(rectangleOf(1.0, 1.5), rectangleOf(2.0, 3.0))
            val r2 = rectangleOf(1.5, 2.0).asParams()
            val r3 = mutableSetOf(rectangleOf(0.0, 1.0), rectangleOf(2.5, 3.0))

            assertDeepEquals(ff, c1 and c1.not())

            assertDeepEquals(c1, c1 and c2.not())
            assertDeepEquals(c2, c2 and c1.not())

            assertDeepEquals(ff, c1 and c3.not())
            assertDeepEquals(r1, c3 and c1.not())

            assertDeepEquals(r2, c2 and c3.not())
            assertDeepEquals(r3, c3 and c2.not())
        }
    }

}