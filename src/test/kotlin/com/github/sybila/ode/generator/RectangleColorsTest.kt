package com.github.sybila.ode.generator

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RectangleColorsTest {

    @Test
    fun emptinessTest() {
        assertTrue(RectangleColors().isEmpty())
        assertTrue(RectangleColors(rectangleOf(0.0, 1.0)).isNotEmpty())
    }

    @Test
    fun intersectTest() {
        val c1 = RectangleColors(rectangleOf(0.0, 1.0))
        val c2 = RectangleColors(rectangleOf(1.0, 2.5))
        val c3 = RectangleColors(rectangleOf(0.0, 1.0), rectangleOf(2.0, 3.0))

        assertEquals(c1, c1 intersect c1)

        assertTrue((c1 intersect c2).isEmpty())
        assertTrue((c2 intersect c1).isEmpty())

        assertEquals(c1, c1 intersect c3)
        assertEquals(c1, c3 intersect c1)

        assertEquals(RectangleColors(rectangleOf(2.0, 2.5)), c2 intersect c3)
        assertEquals(RectangleColors(rectangleOf(2.0, 2.5)), c3 intersect c2)
    }

    @Test
    fun plusTest() {
        val c1 = RectangleColors(rectangleOf(0.0, 1.0))
        val c2 = RectangleColors(rectangleOf(1.0, 2.5))
        val c3 = RectangleColors(rectangleOf(0.0, 1.0), rectangleOf(2.0, 3.0))

        assertEquals(c1, c1 + c1)

        assertEquals(RectangleColors(rectangleOf(0.0, 2.5)), c1 + c2)
        assertEquals(RectangleColors(rectangleOf(0.0, 2.5)), c2 + c1)

        assertEquals(c3, c1 + c3)
        assertEquals(c3, c3 + c1)

        assertEquals(RectangleColors(rectangleOf(0.0, 3.0)), c2 + c3)
        assertEquals(RectangleColors(rectangleOf(0.0, 3.0)), c3 + c2)
    }

    @Test
    fun minusTest() {
        val c1 = RectangleColors(rectangleOf(0.0, 1.0))
        val c2 = RectangleColors(rectangleOf(1.0, 2.5))
        val c3 = RectangleColors(rectangleOf(0.0, 1.5), rectangleOf(2.0, 3.0))

        assertEquals(RectangleColors(), c1 - c1)

        assertEquals(c1, c1 - c2)
        assertEquals(c2, c2 - c1)

        assertEquals(RectangleColors(), c1 - c3)
        assertEquals(RectangleColors(rectangleOf(1.0, 1.5), rectangleOf(2.0, 3.0)), c3 - c1)

        assertEquals(RectangleColors(rectangleOf(1.5, 2.0)), c2 - c3)
        assertEquals(RectangleColors(rectangleOf(0.0, 1.0), rectangleOf(2.5, 3.0)), c3 - c2)
    }

}