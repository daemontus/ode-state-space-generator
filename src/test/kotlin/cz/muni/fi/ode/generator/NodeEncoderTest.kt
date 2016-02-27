package cz.muni.fi.ode.generator

import cz.muni.fi.checker.IDNode
import cz.muni.fi.ode.model.Model
import cz.muni.fi.ode.model.Summand
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class NodeEncoderTest {

    private val simpleModel = Model(listOf(
            Model.Variable(
                    name = "v1", range = Pair(0.0, 10.0), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5, 1.5, 2.0, 3.0, 3.5, 4.0, 6.0, 8.0, 10.0)  //9 states
            )
    ), listOf())

    private val complexModel = Model(listOf(
            Model.Variable(
                    name = "v1", range = Pair(0.0, 10.0), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5, 1.5, 2.0, 3.0, 3.5, 4.0, 6.0, 8.0, 10.0)  //9 states
            ),
            Model.Variable(
                    name = "v2", range = Pair(0.0, 5.0), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5, 1.5, 2.0, 3.0, 3.5, 5.0)  //6 states
            ),
            Model.Variable(
                    name = "v3", range = Pair(0.0, 10.0), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5, 1.5, 2.0, 3.0, 3.5, 4.0, 6.0, 8.0, 10.0)  //9 states
            ),
            Model.Variable(
                    name = "v4", range = Pair(0.0, 0.5), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 0.5)   //1 state
            ),
            Model.Variable(
                    name = "v5", range = Pair(0.0, 13.5), varPoints = null, equation = listOf(Summand()),
                    thresholds = listOf(0.0, 1.5, 13.5) //2 states
            )
    ), listOf())

    private val simpleEncoder = NodeEncoder(simpleModel)
    private val complexEncoder = NodeEncoder(complexModel)

    @Test
    fun simpleEncodingTest() {
        assertEquals(0, simpleEncoder.encodeNode(intArrayOf(0)).id)
        assertEquals(1, simpleEncoder.encodeNode(intArrayOf(1)).id)
        assertEquals(2, simpleEncoder.encodeNode(intArrayOf(2)).id)
        assertEquals(3, simpleEncoder.encodeNode(intArrayOf(3)).id)
        assertEquals(4, simpleEncoder.encodeNode(intArrayOf(4)).id)
        assertEquals(5, simpleEncoder.encodeNode(intArrayOf(5)).id)
        assertEquals(6, simpleEncoder.encodeNode(intArrayOf(6)).id)
        assertEquals(7, simpleEncoder.encodeNode(intArrayOf(7)).id)
        assertEquals(8, simpleEncoder.encodeNode(intArrayOf(8)).id)
    }

    @Test
    fun simpleDecodingTest() {
        assertTrue(Arrays.equals(intArrayOf(0), simpleEncoder.decodeNode(IDNode(0))))
        assertTrue(Arrays.equals(intArrayOf(1), simpleEncoder.decodeNode(IDNode(1))))
        assertTrue(Arrays.equals(intArrayOf(2), simpleEncoder.decodeNode(IDNode(2))))
        assertTrue(Arrays.equals(intArrayOf(3), simpleEncoder.decodeNode(IDNode(3))))
        assertTrue(Arrays.equals(intArrayOf(4), simpleEncoder.decodeNode(IDNode(4))))
        assertTrue(Arrays.equals(intArrayOf(5), simpleEncoder.decodeNode(IDNode(5))))
        assertTrue(Arrays.equals(intArrayOf(6), simpleEncoder.decodeNode(IDNode(6))))
        assertTrue(Arrays.equals(intArrayOf(7), simpleEncoder.decodeNode(IDNode(7))))
        assertTrue(Arrays.equals(intArrayOf(8), simpleEncoder.decodeNode(IDNode(8))))
    }

    @Test
    fun simpleHigherTest() {
        assertEquals(IDNode(1), simpleEncoder.higherNode(IDNode(0), 0))
        assertEquals(IDNode(2), simpleEncoder.higherNode(IDNode(1), 0))
        assertEquals(IDNode(3), simpleEncoder.higherNode(IDNode(2), 0))
        assertEquals(IDNode(4), simpleEncoder.higherNode(IDNode(3), 0))
        assertEquals(IDNode(5), simpleEncoder.higherNode(IDNode(4), 0))
        assertEquals(IDNode(6), simpleEncoder.higherNode(IDNode(5), 0))
        assertEquals(IDNode(7), simpleEncoder.higherNode(IDNode(6), 0))
        assertEquals(IDNode(8), simpleEncoder.higherNode(IDNode(7), 0))
        assertEquals(null, simpleEncoder.higherNode(IDNode(8), 0))
    }

    @Test
    fun simpleLowerTest() {
        assertEquals(null, simpleEncoder.lowerNode(IDNode(0), 0))
        assertEquals(IDNode(0), simpleEncoder.lowerNode(IDNode(1), 0))
        assertEquals(IDNode(1), simpleEncoder.lowerNode(IDNode(2), 0))
        assertEquals(IDNode(2), simpleEncoder.lowerNode(IDNode(3), 0))
        assertEquals(IDNode(3), simpleEncoder.lowerNode(IDNode(4), 0))
        assertEquals(IDNode(4), simpleEncoder.lowerNode(IDNode(5), 0))
        assertEquals(IDNode(5), simpleEncoder.lowerNode(IDNode(6), 0))
        assertEquals(IDNode(6), simpleEncoder.lowerNode(IDNode(7), 0))
        assertEquals(IDNode(7), simpleEncoder.lowerNode(IDNode(8), 0))
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
                                complexEncoder.encodeNode(intArrayOf(v1, v2, v3, v4, v5)).id
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
                                complexEncoder.decodeNode(IDNode(
                                        (v1) + (v2 * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1)
                                ))
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
                                if (v1 == 8) null else (v1+1) + (v2 * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.higherNode(source, 0)?.id
                        )
                        assertEquals(
                                if (v2 == 5) null else (v1) + ((v2 + 1) * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.higherNode(source, 1)?.id
                        )
                        assertEquals(
                                if (v3 == 8) null else (v1) + (v2 * 9) + ((v3 + 1) * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.higherNode(source, 2)?.id
                        )
                        assertEquals(
                                null,
                                complexEncoder.higherNode(source, 3)?.id
                        )
                        assertEquals(
                                if (v5 == 1) null else (v1) + (v2 * 9) + (v3 * 9 * 6) + ((v5 + 1) * 9 * 6 * 9 * 1),
                                complexEncoder.higherNode(source, 4)?.id
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
                                complexEncoder.lowerNode(source, 0)?.id
                        )
                        assertEquals(
                                if (v2 == 0) null else (v1) + ((v2 - 1) * 9) + (v3 * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.lowerNode(source, 1)?.id
                        )
                        assertEquals(
                                if (v3 == 0) null else (v1) + (v2 * 9) + ((v3 - 1) * 9 * 6) + (v5 * 9 * 6 * 9 * 1),
                                complexEncoder.lowerNode(source, 2)?.id
                        )
                        assertEquals(
                                null,
                                complexEncoder.lowerNode(source, 3)?.id
                        )
                        assertEquals(
                                if (v5 == 0) null else (v1) + (v2 * 9) + (v3 * 9 * 6) + ((v5 - 1) * 9 * 6 * 9 * 1),
                                complexEncoder.lowerNode(source, 4)?.id
                        )
                    }
                }
            }
        }
    }

    @Test
    fun modelTooBigTest() {
        assertFails {
            NodeEncoder(Model(listOf(
                    Model.Variable(name = "v1", range = Pair(0.0, 1000.0), varPoints = null, equation = listOf(Summand()),
                        thresholds = (0..1000).map { it.toDouble() }),
                    Model.Variable(name = "v1", range = Pair(0.0, 1000.0), varPoints = null, equation = listOf(Summand()),
                        thresholds = (0..1000).map { it.toDouble() }),
                    Model.Variable(name = "v1", range = Pair(0.0, 1000.0), varPoints = null, equation = listOf(Summand()),
                        thresholds = (0..1000).map { it.toDouble() }),
                    Model.Variable(name = "v1", range = Pair(0.0, 1000.0), varPoints = null, equation = listOf(Summand()),
                        thresholds = (0..1000).map { it.toDouble() })
            ), listOf()))
        }
    }
}