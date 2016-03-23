package com.github.sybila.ode.generator

import com.github.sybila.ode.generator.smt.SMTColors
import com.github.sybila.ode.generator.smt.SMTContext
import com.microsoft.z3.Context
import org.junit.Test
import kotlin.test.assertTrue

class SMTColorsTest {

    private val z3 = Context()
    private val context = z3.run {
        SMTContext(this,
                this.mkTactic("ctx-solver-simplify"), this.mkGoal(false, false, false))
    }

    private val tt = SMTColors(z3.mkTrue(), context)
    private val ff = SMTColors(z3.mkFalse(), context)
    private val x = z3.mkRealConst("x")
    private val three = z3.mkReal(3)
    private val two = z3.mkReal(2)

    private val gt3 = SMTColors(z3.mkGt(x, three), context)
    private val gt2 = SMTColors(z3.mkGt(x, two), context)
    private val lt3 = SMTColors(z3.mkLt(x, three), context)
    private val lt2 = SMTColors(z3.mkLt(x, two), context)

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