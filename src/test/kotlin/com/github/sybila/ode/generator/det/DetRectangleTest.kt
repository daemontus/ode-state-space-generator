package com.github.sybila.ode.generator.det

import com.github.sybila.ode.generator.rect.rectangleOf
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RectangleTest {

    val solver = RectangleSetSolver(0.0 to 1.0, 0.0 to 1.0)

    @Test
    fun basicEmptiness() {
        solver.run {
            assertTrue(tt.isSat())
            assertFalse(ff.isSat())
            assertTrue(rectangleOf(0.0, 0.5, 0.0, 0.5).toRectangleSet().isSat())
        }
    }

    @Test
    fun basicUnionTest() {
        solver.run {
            val s1 = rectangleOf(0.0, 0.1, 0.0, 0.1).toRectangleSet()
            val s2 = rectangleOf(0.2, 0.3, 0.2, 0.3).toRectangleSet()

            assertEquals(s1, s1 or s1)
            assertEquals(s2, s2 or s2)
            assertEquals(RectangleSet(
                    thresholdsX = doubleArrayOf(0.0, 0.1, 0.2, 0.3),
                    thresholdsY = doubleArrayOf(0.0, 0.1, 0.2, 0.3),
                    values = BitSet().apply { set(0); set(8) }
            ), s1 or s2)
        }
    }

    @Test
    fun basicIntersectTest() {
        solver.run {
            val s1 = rectangleOf(0.0, 0.2, 0.0, 0.2).toRectangleSet()
            val s2 = rectangleOf(0.1, 0.3, 0.1, 0.3).toRectangleSet()

            assertEquals(s1, s1 and s1)
            assertEquals(s2, s2 and s2)
            assertEquals(rectangleOf(0.1, 0.2, 0.1, 0.2).toRectangleSet(), s1 and s2)
        }
    }

    @Test
    fun basicNotTest() {
        solver.run {
            val s1 = rectangleOf(0.0, 1.0, 0.0, 0.5).toRectangleSet()
            val s2 = rectangleOf(0.0, 1.0, 0.5, 1.0).toRectangleSet()

            val k1 = rectangleOf(0.0, 0.5, 0.0, 1.0).toRectangleSet()
            val k2 = rectangleOf(0.5, 1.0, 0.0, 1.0).toRectangleSet()

            assertEquals(s2, s1.not())
            assertEquals(s1, s2.not())

            assertEquals(k1, k2.not())
            assertEquals(k2, k1.not())
        }
    }

    @Test
    fun complexUnion() {
        solver.run {
            val s1 = rectangleOf(0.0, 0.4, 0.0, 0.3).toRectangleSet()
            val s2 = rectangleOf(0.3, 1.0, 0.0, 0.5).toRectangleSet()
            val s3 = rectangleOf(0.0, 0.3, 0.2, 1.0).toRectangleSet()
            val s4 = rectangleOf(0.3, 1.0, 0.4, 1.0).toRectangleSet()

            assertEquals(tt, s1 or s2 or s3 or s4)
        }
    }

    @Test
    fun complexIntersect() {
        solver.run {
            val s1 = rectangleOf(0.0, 0.2, 0.0, 0.2).toRectangleSet()
            val s2 = rectangleOf(0.8, 1.0, 0.0, 0.2).toRectangleSet()
            val s3 = rectangleOf(0.0, 0.2, 0.8, 1.0).toRectangleSet()
            val s4 = rectangleOf(0.8, 1.0, 0.8, 1.0).toRectangleSet()

            val p = rectangleOf(0.1, 0.9, 0.1, 0.9).toRectangleSet()

            val r1 = rectangleOf(0.1, 0.2, 0.1, 0.2).toRectangleSet()
            val r2 = rectangleOf(0.1, 0.2, 0.8, 0.9).toRectangleSet()
            val r3 = rectangleOf(0.8, 0.9, 0.1, 0.2).toRectangleSet()
            val r4 = rectangleOf(0.8, 0.9, 0.8, 0.9).toRectangleSet()

            assertEquals((r1 or r2 or r3 or r4), (s1 or s2 or s3 or s4) and p)
        }
    }

}