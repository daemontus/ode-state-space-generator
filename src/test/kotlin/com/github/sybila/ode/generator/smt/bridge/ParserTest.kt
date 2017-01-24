package com.github.sybila.ode.generator.smt.bridge

import com.github.sybila.ode.generator.smt.remote.bridge.f
import com.github.sybila.ode.generator.smt.remote.bridge.readSMT
import com.github.sybila.ode.generator.smt.remote.bridge.t
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ParserTest {

    @Test
    fun terminal() {
        assertEquals("foo".t(), "foo".readSMT())
    }

    @Test
    fun nullFun() {
        assertEquals("foo".f(), "(foo)".readSMT())
    }

    @Test
    fun unaryFun() {
        assertEquals("foo".f("a".t()), "(foo a)".readSMT())
    }

    @Test
    fun binaryFun() {
        assertEquals("foo".f("a".t(), "b".t()), "(foo a b)".readSMT())
    }

    @Test
    fun skipParam() {
        assertEquals("foo".f("a".t(), "b".t()), "(foo a b :param val :param2 val)".readSMT())
    }

    @Test
    fun complexTest() {
        assertEquals("level1".f(
                "level2".f(
                        "t1".t(),
                        "t2".t()
                ), "t3".t(),
                "level2B".f(), "t4".t()
        ), "(level1 (level2 t1 t2) t3 (level2B) t4)".readSMT())
    }

    @Test
    fun failOnEmptyLine() {
        assertFails {
            "".readSMT()
        }
    }

    @Test
    fun failOnIncomplete() {
        assertFails {
            "(foo".readSMT()
        }
    }

}