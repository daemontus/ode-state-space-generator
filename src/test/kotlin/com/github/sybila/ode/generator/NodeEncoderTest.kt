package com.github.sybila.ode.generator

import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Summand
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class NodeEncoderTest {

    private val simpleModel = OdeModel(listOf(
            OdeModel.Variable(
                    name = "v1", range = Pair(0.0, 10.0), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5, 1.5, 2.0, 3.0, 3.5, 4.0, 6.0, 8.0, 10.0)  //9 states
            )
    ), listOf())

    private val complexModel = OdeModel(listOf(
            OdeModel.Variable(
                    name = "v1", range = Pair(0.0, 10.0), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5, 1.5, 2.0, 3.0, 3.5, 4.0, 6.0, 8.0, 10.0)  //9 states
            ),
            OdeModel.Variable(
                    name = "v2", range = Pair(0.0, 5.0), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5, 1.5, 2.0, 3.0, 3.5, 5.0)  //6 states
            ),
            OdeModel.Variable(
                    name = "v3", range = Pair(0.0, 10.0), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5, 1.5, 2.0, 3.0, 3.5, 4.0, 6.0, 8.0, 10.0)  //9 states
            ),
            OdeModel.Variable(
                    name = "v4", range = Pair(0.0, 0.5), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5)   //1 state
            ),
            OdeModel.Variable(
                    name = "v5", range = Pair(0.0, 13.5), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 1.5, 13.5) //2 states
            )
    ), listOf())

    private val simpleEncoder = NodeEncoder(simpleModel)
    private val complexEncoder = NodeEncoder(complexModel)

    @Test
    fun simpleEncodingTest() {
        assertEquals(0, simpleEncoder.encodeNode(intArrayOf(0)))
        assertEquals(1, simpleEncoder.encodeNode(intArrayOf(1)))
        assertEquals(2, simpleEncoder.encodeNode(intArrayOf(2)))
        assertEquals(3, simpleEncoder.encodeNode(intArrayOf(3)))
        assertEquals(4, simpleEncoder.encodeNode(intArrayOf(4)))
        assertEquals(5, simpleEncoder.encodeNode(intArrayOf(5)))
        assertEquals(6, simpleEncoder.encodeNode(intArrayOf(6)))
        assertEquals(7, simpleEncoder.encodeNode(intArrayOf(7)))
        assertEquals(8, simpleEncoder.encodeNode(intArrayOf(8)))
    }

    @Test
    fun simpleDecodingTest() {
        assertTrue(Arrays.equals(intArrayOf(0), simpleEncoder.decodeNode(0)))
        assertTrue(Arrays.equals(intArrayOf(1), simpleEncoder.decodeNode(1)))
        assertTrue(Arrays.equals(intArrayOf(2), simpleEncoder.decodeNode(2)))
        assertTrue(Arrays.equals(intArrayOf(3), simpleEncoder.decodeNode(3)))
        assertTrue(Arrays.equals(intArrayOf(4), simpleEncoder.decodeNode(4)))
        assertTrue(Arrays.equals(intArrayOf(5), simpleEncoder.decodeNode(5)))
        assertTrue(Arrays.equals(intArrayOf(6), simpleEncoder.decodeNode(6)))
        assertTrue(Arrays.equals(intArrayOf(7), simpleEncoder.decodeNode(7)))
        assertTrue(Arrays.equals(intArrayOf(8), simpleEncoder.decodeNode(8)))
    }

    @Test
    fun simpleHigherTest() {
        assertEquals(1, simpleEncoder.higherNode(0, 0))
        assertEquals(2, simpleEncoder.higherNode(1, 0))
        assertEquals(3, simpleEncoder.higherNode(2, 0))
        assertEquals(4, simpleEncoder.higherNode(3, 0))
        assertEquals(5, simpleEncoder.higherNode(4, 0))
        assertEquals(6, simpleEncoder.higherNode(5, 0))
        assertEquals(7, simpleEncoder.higherNode(6, 0))
        assertEquals(8, simpleEncoder.higherNode(7, 0))
        assertEquals(null, simpleEncoder.higherNode(8, 0))
    }

    @Test
    fun simpleLowerTest() {
        assertEquals(null, simpleEncoder.lowerNode(0, 0))
        assertEquals(0, simpleEncoder.lowerNode(1, 0))
        assertEquals(1, simpleEncoder.lowerNode(2, 0))
        assertEquals(2, simpleEncoder.lowerNode(3, 0))
        assertEquals(3, simpleEncoder.lowerNode(4, 0))
        assertEquals(4, simpleEncoder.lowerNode(5, 0))
        assertEquals(5, simpleEncoder.lowerNode(6, 0))
        assertEquals(6, simpleEncoder.lowerNode(7, 0))
        assertEquals(7, simpleEncoder.lowerNode(8, 0))
    }

    @Test
    fun complexEncodingTest() {
        for (v1 in 0..8) {
            for (v2 in 0..5) {
                for (v3 in 0..8) {
                    val v4 = 0
                    for (v5 in 0..1) {
                        assertEquals(
                                (v1) + (v2 * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.encodeNode(intArrayOf(v1, v2, v3, v4, v5))
                        )
                    }
                }
            }
        }
    }

    @Test
    fun complexDecodingTest() {
        for (v1 in 0..8) {
            for (v2 in 0..5) {
                for (v3 in 0..8) {
                    val v4 = 0
                    for (v5 in 0..1) {
                        assertTrue(Arrays.equals(
                                intArrayOf(v1, v2, v3, v4, v5),
                                complexEncoder.decodeNode(
                                        (v1) + (v2 * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1)
                                )
                        ))
                    }
                }
            }
        }
    }

    @Test
    fun complexHigherTest() {
        for (v1 in 0..8) {
            for (v2 in 0..5) {
                for (v3 in 0..8) {
                    val v4 = 0
                    for (v5 in 0..1) {
                        val source = complexEncoder.encodeNode(intArrayOf(v1, v2, v3, v4, v5))
                        assertEquals(
                                if (v1 == 8) null else (v1 + 1) + (v2 * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.higherNode(source, 0)
                        )
                        assertEquals(
                                if (v2 == 5) null else (v1) + ((v2 + 1) * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.higherNode(source, 1)
                        )
                        assertEquals(
                                if (v3 == 8) null else (v1) + (v2 * 9) + ((v3 + 1) * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.higherNode(source, 2)
                        )
                        assertEquals(
                                null,
                                complexEncoder.higherNode(source, 3)
                        )
                        assertEquals(
                                if (v5 == 1) null else (v1) + (v2 * 9) + (v3 * 9 * 6) + ((v5 + 1) * 9 * 6 * 9 * 1),
                                complexEncoder.higherNode(source, 4)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun complexLowerTest() {
        for (v1 in 0..8) {
            for (v2 in 0..5) {
                for (v3 in 0..8) {
                    val v4 = 0
                    for (v5 in 0..1) {
                        val source = complexEncoder.encodeNode(intArrayOf(v1, v2, v3, v4, v5))
                        assertEquals(
                                if (v1 == 0) null else (v1 - 1) + (v2 * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.lowerNode(source, 0)
                        )
                        assertEquals(
                                if (v2 == 0) null else (v1) + ((v2 - 1) * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.lowerNode(source, 1)
                        )
                        assertEquals(
                                if (v3 == 0) null else (v1) + (v2 * 9) + ((v3 - 1) * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.lowerNode(source, 2)
                        )
                        assertEquals(
                                null,
                                complexEncoder.lowerNode(source, 3)
                        )
                        assertEquals(
                                if (v5 == 0) null else (v1) + (v2 * 9) + (v3 * 9 * 6) + ((v5 - 1) * 9 * 6 * 9 * 1),
                                complexEncoder.lowerNode(source, 4)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun modelTooBigTest() {
        assertFails {
            NodeEncoder(OdeModel(listOf(
                    OdeModel.Variable(name = "v1", range = Pair(0.0, 1000.0), varPoints = null, equation = listOf(Summand()),
                            thresholds = (0..1000).map(Int::toDouble)),
                    OdeModel.Variable(name = "v1", range = Pair(0.0, 1000.0), varPoints = null, equation = listOf(Summand()),
                            thresholds = (0..1000).map(Int::toDouble)),
                    OdeModel.Variable(name = "v1", range = Pair(0.0, 1000.0), varPoints = null, equation = listOf(Summand()),
                            thresholds = (0..1000).map(Int::toDouble)),
                    OdeModel.Variable(name = "v1", range = Pair(0.0, 1000.0), varPoints = null, equation = listOf(Summand()),
                            thresholds = (0..1000).map(Int::toDouble))
            ), listOf()))
        }
    }
}