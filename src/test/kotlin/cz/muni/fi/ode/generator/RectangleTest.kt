package cz.muni.fi.ode.generator

/*
class SubtractTests {

    val r1 = Rect(Array(3) {
        Interval(0.0, 3.0)
    })

    val r2 = Rect(Array(3) {
        Interval(1.0, 2.0)
    })

    val r3 = Rect(Array(3) {
        Interval(2.0, 4.0)
    })

    @Test
    fun emptyEmpty() {
        assertEquals(setOf(Rect.Companion.empty(3)), Rect.Companion.empty(3) subtract Rect.Companion.empty(3))
    }

    @Test
    fun oneEmpty() {
        assertEquals(setOf(Rect.Companion.empty(3)), Rect.Companion.empty(3) subtract r1)
        assertEquals(setOf(r1), r1 subtract Rect.Companion.empty(3))
    }

    @Test
    fun identity() {
        assertEquals(setOf<Rect>(), r1 subtract r1)
    }

    @Test
    fun complex() {
        assertEquals(setOf(
                Rect(arrayOf(
                        Interval(0.0, 1.0),
                        Interval(0.0, 3.0),
                        Interval(0.0, 3.0)
                )),
                Rect(arrayOf(
                        Interval(2.0, 3.0),
                        Interval(0.0, 3.0),
                        Interval(0.0, 3.0)
                )),
                Rect(arrayOf(
                        Interval(1.0, 2.0),
                        Interval(0.0, 1.0),
                        Interval(0.0, 3.0)
                )),
                Rect(arrayOf(
                        Interval(1.0, 2.0),
                        Interval(2.0, 3.0),
                        Interval(0.0, 3.0)
                )),
                Rect(arrayOf(
                        Interval(1.0, 2.0),
                        Interval(1.0, 2.0),
                        Interval(0.0, 1.0)
                )),
                Rect(arrayOf(
                        Interval(1.0, 2.0),
                        Interval(1.0, 2.0),
                        Interval(2.0, 3.0)
                ))
        ), r1 subtract r2)

        assertEquals(setOf(
                Rect(arrayOf(
                        Interval(0.0, 2.0),
                        Interval(0.0, 3.0),
                        Interval(0.0, 3.0)
                )),
                Rect(arrayOf(
                        Interval(2.0, 3.0),
                        Interval(0.0, 2.0),
                        Interval(0.0, 3.0)
                )),
                Rect(arrayOf(
                        Interval(2.0, 3.0),
                        Interval(2.0, 3.0),
                        Interval(0.0, 2.0)
                ))
        ), r1 subtract r3)
    }

}

class MergeTests {

    val r1 = Rect(Array(3) {
        Interval(0.0, 3.0)
    })

    val r2 = Rect(arrayOf(
            Interval(0.0, 3.0),
            Interval(1.0, 4.0),
            Interval(0.0, 3.0)
    ))

    val r3 = Rect(arrayOf(
            Interval(0.0, 3.0),
            Interval(0.0, 4.0),
            Interval(0.0, 3.0)
    ))

    @Test
    fun emptyEmpty() {
        assertTrue((Rect.Companion.empty(3) merge Rect.Companion.empty(3))!!.isEmpty())
    }

    @Test
    fun oneEmpty() {
        assertEquals(r1, r1 merge Rect.Companion.empty(3))
        assertEquals(r1, Rect.Companion.empty(3) merge r1)
    }

    @Test
    fun identity() {
        assertEquals(r1, r1 merge r1)
    }

    @Test
    fun complex() {
        assertEquals(r3, r1 merge r2)
    }

}

class EnclosesTests {

    val r03 = Rect(Array(3) {
        Interval(0.0, 3.0)
    })

    val rX = Rect(arrayOf(
            Interval(0.0, 2.5),
            Interval(1.5, 2.8),
            Interval(0.002, 3.0)
    ))

    @Test
    fun emptyEmpty() {
        assertTrue(Rect.Companion.empty(3) encloses Rect.Companion.empty(3))
    }

    @Test
    fun oneEmpty() {
        assertTrue(r03 encloses Rect.Companion.empty(3))
        assertFalse(Rect.Companion.empty(3) encloses r03)
    }

    @Test
    fun identity() {
        assertTrue(r03 encloses r03)
    }

    @Test
    fun complex() {
        assertTrue(r03 encloses rX)
        assertFalse(rX encloses r03)
    }

}

class IntersectTests {

    val r03 = Rect(Array(3) {
        Interval(0.0, 3.0)
    })

    val r12 = Rect(Array(3) {
        Interval(1.0, 2.0)
    })

    val r24 = Rect(Array(3) {
        Interval(2.0, 4.0)
    })

    @Test
    fun emptyEmpty() {
        assertEquals(Rect.Companion.empty(3), Rect.Companion.empty(3) intersect Rect.Companion.empty(3))
    }

    @Test
    fun oneEmpty() {
        assertTrue((r03 intersect Rect.Companion.empty(3)).isEmpty())
        assertTrue((Rect.Companion.empty(3) intersect r03).isEmpty())
    }

    @Test
    fun identity() {
        assertEquals(r03, r03 intersect r03)
        assertEquals(r12, r12 intersect r12)
        assertEquals(r24, r24 intersect r24)
    }

    @Test
    fun complex() {
        assertEquals(r12, r03 intersect r12)
        assertEquals(r12, r12 intersect r03)

        val k1 = Rect(Array(3) {
            Interval(2.0, 3.0)
        })
        assertEquals(k1, r03 intersect r24)
        assertEquals(k1, r24 intersect r03)

        val k2 = Rect(Array(3) {
            Interval(2.0, 2.0)
        })
        assertEquals(k2, r12 intersect r24)
        assertEquals(k2, r24 intersect r12)
    }

}*/