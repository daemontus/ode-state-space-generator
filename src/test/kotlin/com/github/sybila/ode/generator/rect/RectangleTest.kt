package com.github.sybila.ode.generator.rect

import org.junit.Test
import kotlin.test.assertEquals

class SingleDimensionTests {

    //All strict tests have three parts:
    //1. Non-equal intersection - l1..l2..h1..h2
    //2. One is subset - l1..l2..h2..l1
    //3. No intersection - l1..h1 l2..h2

    //All equal tests have three parts:
    //1. Invariant - l1=l2..h1=h2
    //2. No intersection - l1..h1=l2..h2
    //3. One is subset - l1=l2..h1..h2 || l1..l2..h1=h2

    @Test
    fun strictTimesTest() {
        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(0.0, 2.0) * rectangleOf(1.0, 3.0))
        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(1.0, 3.0) * rectangleOf(0.0, 2.0))

        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(0.0, 3.0) * rectangleOf(1.0, 2.0))
        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(1.0, 2.0) * rectangleOf(0.0, 3.0))

        assertEquals(null, rectangleOf(0.0, 1.0) * rectangleOf(2.0, 3.0))
        assertEquals(null, rectangleOf(2.0, 3.0) * rectangleOf(0.0, 1.0))
    }

    @Test
    fun equalTimesTest() {
        assertEquals(rectangleOf(0.0, 1.0), rectangleOf(0.0, 1.0) * rectangleOf(0.0, 1.0))

        assertEquals(null, rectangleOf(0.0, 1.0) * rectangleOf(1.0, 2.0))
        assertEquals(null, rectangleOf(1.0, 2.0) * rectangleOf(0.0, 1.0))

        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(1.0, 2.0) * rectangleOf(1.0, 3.0))
        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(1.0, 3.0) * rectangleOf(1.0, 2.0))
        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(1.0, 2.0) * rectangleOf(0.0, 2.0))
        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(0.0, 2.0) * rectangleOf(1.0, 2.0))
    }

    @Test
    fun strictPlusTest() {
        assertEquals(rectangleOf(0.0, 3.0), rectangleOf(0.0, 2.0) + rectangleOf(1.0, 3.0))
        assertEquals(rectangleOf(0.0, 3.0), rectangleOf(1.0, 3.0) + rectangleOf(0.0, 2.0))

        assertEquals(rectangleOf(0.0, 3.0), rectangleOf(0.0, 3.0) + rectangleOf(1.0, 2.0))
        assertEquals(rectangleOf(0.0, 3.0), rectangleOf(1.0, 2.0) + rectangleOf(0.0, 3.0))

        assertEquals(null, rectangleOf(0.0, 1.0) + rectangleOf(2.0, 3.0))
        assertEquals(null, rectangleOf(2.0, 3.0) + rectangleOf(0.0, 1.0))
    }

    @Test
    fun equalPlusTest() {
        assertEquals(rectangleOf(1.0, 2.0), rectangleOf(1.0, 2.0) + rectangleOf(1.0, 2.0))

        assertEquals(rectangleOf(0.0, 2.0), rectangleOf(0.0, 1.0) + rectangleOf(1.0, 2.0))
        assertEquals(rectangleOf(0.0, 2.0), rectangleOf(1.0, 2.0) + rectangleOf(0.0, 1.0))

        assertEquals(rectangleOf(1.0, 3.0), rectangleOf(1.0, 2.0) + rectangleOf(1.0, 3.0))
        assertEquals(rectangleOf(1.0, 3.0), rectangleOf(1.0, 3.0) + rectangleOf(1.0, 2.0))
        assertEquals(rectangleOf(0.0, 2.0), rectangleOf(1.0, 2.0) + rectangleOf(0.0, 2.0))
        assertEquals(rectangleOf(0.0, 2.0), rectangleOf(0.0, 2.0) + rectangleOf(1.0, 2.0))
    }

    @Test
    fun strictMinusTest() {
        assertEquals(setOf(rectangleOf(0.0, 1.0)), rectangleOf(0.0, 2.0) - rectangleOf(1.0, 3.0))
        assertEquals(setOf(rectangleOf(2.0, 3.0)), rectangleOf(1.0, 3.0) - rectangleOf(0.0, 2.0))

        assertEquals(setOf(rectangleOf(0.0, 1.0), rectangleOf(2.0, 3.0)), rectangleOf(0.0, 3.0) - rectangleOf(1.0, 2.0))
        assertEquals(setOf(), rectangleOf(1.0, 2.0) - rectangleOf(0.0, 3.0))

        assertEquals(setOf(rectangleOf(0.0, 1.0)), rectangleOf(0.0, 1.0) - rectangleOf(2.0, 3.0))
        assertEquals(setOf(rectangleOf(2.0, 3.0)), rectangleOf(2.0, 3.0) - rectangleOf(0.0, 1.0))
    }

    @Test
    fun equalMinusTest() {
        assertEquals(setOf(), rectangleOf(0.0, 1.0) - rectangleOf(0.0, 1.0))

        assertEquals(setOf(rectangleOf(0.0, 1.0)), rectangleOf(0.0, 1.0) - rectangleOf(1.0, 2.0))
        assertEquals(setOf(rectangleOf(1.0, 2.0)), rectangleOf(1.0, 2.0) - rectangleOf(0.0, 1.0))

        assertEquals(setOf(), rectangleOf(1.0, 2.0) - rectangleOf(1.0, 3.0))
        assertEquals(setOf(rectangleOf(2.0, 3.0)), rectangleOf(1.0, 3.0) - rectangleOf(1.0, 2.0))
        assertEquals(setOf(), rectangleOf(1.0, 2.0) - rectangleOf(0.0, 2.0))
        assertEquals(setOf(rectangleOf(0.0, 1.0)), rectangleOf(0.0, 2.0) - rectangleOf(1.0, 2.0))
    }

}

class TwoDimensionsTests {

    //Here strict and equal tests are mixed. All tests should test these cases:
    // - Identity
    // - No intersection - both different
    // - No intersection - both different, one equal (line)
    // - No intersection - both different, both equal (point)
    // - No intersection - one identical
    // - No intersection - one identical, other equal (line)
    // - One is subset - both different
    // - One is subset - both different, one equal
    // - One is subset - both different, both equal
    // - One is subset - one identical
    // - One is subset - one identical, other equal
    // - Non-equal intersection - both different
    // - Non-equal intersection - one identical
    // When in doubt, draw images!

    //a = 0, b = 1, c = 2,...
    val aabb = rectangleFromPoints(0.0, 0.0, 1.0, 1.0)
    val aacc = rectangleFromPoints(0.0, 0.0, 2.0, 2.0)
    val aadd = rectangleFromPoints(0.0, 0.0, 3.0, 3.0)
    val bbcc = rectangleFromPoints(1.0, 1.0, 2.0, 2.0)
    val bbdd = rectangleFromPoints(1.0, 1.0, 3.0, 3.0)
    val ccdd = rectangleFromPoints(2.0, 2.0, 3.0, 3.0)

    val aabc = rectangleFromPoints(0.0, 0.0, 1.0, 2.0)
    val aabd = rectangleFromPoints(0.0, 0.0, 1.0, 3.0)
    val aacb = rectangleFromPoints(0.0, 0.0, 2.0, 1.0)
    val aadc = rectangleFromPoints(0.0, 0.0, 3.0, 2.0)
    val abbd = rectangleFromPoints(0.0, 1.0, 1.0, 3.0)
    val bacb = rectangleFromPoints(1.0, 0.0, 2.0, 1.0)
    val bacc = rectangleFromPoints(1.0, 0.0, 2.0, 2.0)
    val bacd = rectangleFromPoints(1.0, 0.0, 2.0, 3.0)
    val badc = rectangleFromPoints(1.0, 0.0, 3.0, 2.0)
    val badd = rectangleFromPoints(1.0, 0.0, 3.0, 3.0)
    val bccd = rectangleFromPoints(1.0, 2.0, 2.0, 3.0)
    val cadb = rectangleFromPoints(2.0, 0.0, 3.0, 1.0)
    val cadc = rectangleFromPoints(2.0, 0.0, 3.0, 2.0)
    val cadd = rectangleFromPoints(2.0, 0.0, 3.0, 3.0)
    val cbdd = rectangleFromPoints(2.0, 1.0, 3.0, 3.0)

    @Test
    fun timesTest() {
        assertEquals(aabb, aabb * aabb)

        assertEquals(null, aabb * ccdd)
        assertEquals(null, ccdd * aabb)

        assertEquals(null, bbcc * aabd)
        assertEquals(null, aabd * bbcc)

        assertEquals(null, aabb * bbcc)
        assertEquals(null, bbcc * aabb)

        assertEquals(null, aabb * cadb)
        assertEquals(null, cadb * aabb)

        assertEquals(null, aabb * bacb)
        assertEquals(null, bacb * aabb)

        assertEquals(bbcc, aadd * bbcc)
        assertEquals(bbcc, bbcc * aadd)

        assertEquals(bacb, bacb * aacc)
        assertEquals(bacb, aacc * bacb)

        assertEquals(aabb, aabb * aadd)
        assertEquals(aabb, aadd * aabb)

        assertEquals(bacd, bacd * aadd)
        assertEquals(bacd, aadd * bacd)

        assertEquals(aabd, aabd * aadd)
        assertEquals(aabd, aadd * aabd)

        assertEquals(bbcc, aacc * bbdd)
        assertEquals(bbcc, bbdd * aacc)

        assertEquals(bacc, aacc * badc)
        assertEquals(bacc, badc * aacc)
    }


    @Test
    fun plusTest() {
        assertEquals(aabb, aabb + aabb)

        assertEquals(null, aabb + ccdd)
        assertEquals(null, ccdd + aabb)

        assertEquals(null, bbcc + aabd)
        assertEquals(null, aabd + bbcc)

        assertEquals(null, aabb + bbcc)
        assertEquals(null, bbcc + aabb)

        assertEquals(null, aabb + cadb)
        assertEquals(null, cadb + aabb)

        assertEquals(aacb, aabb + bacb)
        assertEquals(aacb, bacb + aabb)

        assertEquals(aadd, aadd + bbcc)
        assertEquals(aadd, bbcc + aadd)

        assertEquals(aacc, bacb + aacc)
        assertEquals(aacc, aacc + bacb)

        assertEquals(aadd, aabb + aadd)
        assertEquals(aadd, aadd + aabb)

        assertEquals(aadd, bacd + aadd)
        assertEquals(aadd, aadd + bacd)

        assertEquals(aadd, aabd + aadd)
        assertEquals(aadd, aadd + aabd)

        assertEquals(null, aacc + bbdd)
        assertEquals(null, bbdd + aacc)

        assertEquals(aadc, aacc + badc)
        assertEquals(aadc, badc + aacc)
    }

    @Test
    fun minusTest() {
        assertEquals(setOf(), aabb - aabb)

        assertEquals(setOf(aabb), aabb - ccdd)
        assertEquals(setOf(ccdd), ccdd - aabb)

        assertEquals(setOf(bbcc), bbcc - aabd)
        assertEquals(setOf(aabd), aabd - bbcc)

        assertEquals(setOf(aabb), aabb - bbcc)
        assertEquals(setOf(bbcc), bbcc - aabb)

        assertEquals(setOf(aabb), aabb - cadb)
        assertEquals(setOf(cadb), cadb - aabb)

        assertEquals(setOf(aabb), aabb - bacb)
        assertEquals(setOf(bacb), bacb - aabb)

        assertEquals(setOf(), bbcc - aadd)
        assertEquals(setOf(aabd, cadd, bacb, bccd), aadd - bbcc)

        assertEquals(setOf(), bacb - aacc)
        assertEquals(setOf(aabc, bbcc), aacc - bacb)

        assertEquals(setOf(), aabb - aadd)
        assertEquals(setOf(abbd, badd), aadd - aabb)

        assertEquals(setOf(), bacd - aadd)
        assertEquals(setOf(aabd, cadd), aadd - bacd)

        assertEquals(setOf(), aabd - aadd)
        assertEquals(setOf(badd), aadd - aabd)

        assertEquals(setOf(aabc, bacb), aacc - bbdd)
        assertEquals(setOf(bccd, cbdd), bbdd - aacc)

        assertEquals(setOf(aabc), aacc - badc)
        assertEquals(setOf(cadc), badc - aacc)
    }
}