package com.github.sybila.ode.generator.smt.local

import org.junit.Test
import kotlin.test.assertTrue

class Z3SolverTest {

    val solver = Z3Solver(listOf(0.0 to 4.0))

    private fun Z3Solver.x() = params[0]
    private fun Z3Solver.three() = 3.toZ3()
    private fun Z3Solver.two() = 2.toZ3()

    private fun Z3Solver.gt3() = (x() gt three()).toParams()
    private fun Z3Solver.gt2() = (x() gt two()).toParams()
    private fun Z3Solver.lt3() = (x() lt three()).toParams()
    private fun Z3Solver.lt2() = (x() lt two()).toParams()

    @Test
    fun isEmptyTest() {
        solver.run {
            assertTrue(ff.isNotSat())
            assertTrue(tt.isSat())
            assertTrue(gt3().isSat())
        }
    }

    @Test
    fun andTest() {
        solver.run {
            assertTrue((tt and gt3()).isSat())
            assertTrue((gt3() and gt3()).isSat())
            assertTrue((gt3() and lt2()).isNotSat())
            assertTrue((gt3() and gt2()).isSat())
            assertTrue((lt2() and gt2()).isNotSat())
        }
    }

    @Test
    fun plusTest() {
        solver.run {
            assertTrue((gt3() or tt).isSat())
            assertTrue((gt3() or ff).isSat())
            assertTrue((gt3() or gt2()).isSat())
            assertTrue((gt3() or lt2()).isSat())
            assertTrue((ff or ff).isNotSat())
        }
    }

    @Test
    fun minusTest() {
        solver.run {
            assertTrue((ff and tt.not()).isNotSat())
            assertTrue((tt and tt.not()).isNotSat())
            assertTrue((gt3() and gt2().not()).isNotSat())
            assertTrue((lt2() and lt3().not()).isNotSat())
            assertTrue((gt2() and gt3().not()).isSat())
            assertTrue((lt3() and lt2().not()).isSat())
        }
    }

}