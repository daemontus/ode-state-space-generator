package com.github.sybila.ode.generator.smt

import com.microsoft.z3.BoolExpr
import org.junit.Test
import kotlin.test.assertTrue

class SMTColorsTest {

    private val order = PartialOrderSet(listOf())

    fun BoolExpr.toColors() = SMTColors(CNF(setOf(Clause(setOf(this), order)), order), order)

    private val tt = SMTColors(CNF(setOf(), order), order)
    private val ff = SMTColors(CNF(setOf(Clause(setOf(), order)), order), order)
    private val x = "x".toZ3()
    private val three = 3.toZ3()
    private val two = 2.toZ3()

    private val gt3 = (x gt three).toColors()
    private val gt2 = (x gt two).toColors()
    private val lt3 = (x lt three).toColors()
    private val lt2 = (x lt two).toColors()

    @Test
    fun isEmptyTest() {
        assertTrue(ff.isEmpty())
        assertTrue(tt.isNotEmpty())
        assertTrue(gt3.isNotEmpty())
    }

    @Test
    fun intersectTest() {
        assertTrue((tt intersect gt3).isNotEmpty())
        assertTrue((tt intersect gt3).isNotEmpty())
        assertTrue(gt3.intersect(gt3).isNotEmpty())
        assertTrue(gt3.intersect(lt2).isEmpty())
        assertTrue(gt3.intersect(gt2).isNotEmpty())
        assertTrue(lt2.intersect(gt2).isEmpty())
    }

    @Test
    fun plusTest() {
        assertTrue(gt3.union(tt).isNotEmpty())
        assertTrue(gt3.union(ff).isNotEmpty())
        assertTrue(gt3.union(gt2).isNotEmpty())
        assertTrue(gt3.union(lt2).isNotEmpty())
        assertTrue(ff.union(ff).isEmpty())
    }

    @Test
    fun minusTest() {
        assertTrue(ff.plus(tt).isNotEmpty())
        assertTrue(ff.plus(ff).isEmpty())
        assertTrue(gt3.subtract(gt2).isEmpty())
        assertTrue(lt2.subtract(lt3).isEmpty())
        assertTrue(gt2.subtract(gt3).isNotEmpty())
        assertTrue(lt3.subtract(lt2).isNotEmpty())
    }

}