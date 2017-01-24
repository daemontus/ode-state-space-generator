package com.github.sybila.ode.generator.smt.remote

import org.junit.Test
import kotlin.test.assertTrue

class Z3SolverTest {

    val solver = Z3Solver(listOf(0.0 to 4.0))

    val gt3 = "(> p0 3.0)".toParams()
    val gt2 = "(> p0 2.0)".toParams()
    val lt3 = "(< p0 3.0)".toParams()
    val lt2 = "(< p0 2.0)".toParams()

    @Test
    fun isEmptyTest() {
        solver.run {
            assertTrue(ff.isNotSat())
            assertTrue(tt.isSat())
            assertTrue(gt3.isSat())
        }
    }

    @Test
    fun andTest() {
        solver.run {
            assertTrue((tt and gt3).isSat())
            assertTrue((gt3 and gt3).isSat())
            assertTrue((gt3 and lt2).isNotSat())
            assertTrue((gt3 and gt2).isSat())
            assertTrue((lt2 and gt2).isNotSat())
        }
    }

    @Test
    fun plusTest() {
        solver.run {
            assertTrue((gt3 or tt).isSat())
            assertTrue((gt3 or ff).isSat())
            assertTrue((gt3 or gt2).isSat())
            assertTrue((gt3 or lt2).isSat())
            assertTrue((ff or ff).isNotSat())
        }
    }

    @Test
    fun minusTest() {
        solver.run {
            assertTrue((ff and tt.not()).isNotSat())
            assertTrue((tt and tt.not()).isNotSat())
            assertTrue((gt3 and gt2.not()).isNotSat())
            assertTrue((lt2 and lt3.not()).isNotSat())
            assertTrue((gt2 and gt3.not()).isSat())
            assertTrue((lt3 and lt2.not()).isSat())
        }
    }

}