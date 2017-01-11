package com.github.sybila.ode.generator.distributed

import com.github.sybila.checker.distributed.Checker
import com.github.sybila.checker.distributed.Solver
import com.github.sybila.checker.distributed.channel.connectWithSharedMemory
import com.github.sybila.checker.distributed.partition.asUniformPartitions
import com.github.sybila.huctl.HUCTLParser
import com.github.sybila.ode.generator.distributed.rect.RectangleOdeModel
import com.github.sybila.ode.generator.distributed.smt.Z3OdeFragment
import com.github.sybila.ode.generator.distributed.smt.readSMT
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import java.io.File
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val model = Parser().parse(File("/Users/daemontus/heap/sybila/models/tcbb.bio")).computeApproximation()
    val prop = HUCTLParser().parse(File("/Users/daemontus/heap/sybila/models/test_prop.huctl"))

    val workers = 4
    val fragments = (1..workers).map { RectangleOdeModel(model) }.asUniformPartitions()

    println()

    fragments.forEach { fragment ->
        for (state in 0 until fragment.stateCount) {
            fragment.run {
                state.successors(true)
                state.predecessors(true)
            }
        }
        println("Precomputed: ${fragment.stateCount}")
    }

    repeat(10) {
        val elapsed = measureTimeMillis {
            Checker(fragments.connectWithSharedMemory()).use { checker ->
                val r = checker.verify(prop)
                for ((f, map) in r) {
                    fragments.first().run {
                        println("$f ${map.map { it.sizeHint }}")
                    }
                }
            }
        }
        println("Elapsed: $elapsed")
    }




    //1000  /   1   : 8.9s
    //500   /   2   : 5.4s
    //250   /   4   : 4.8s
    //166   /   6   : 5.0s
    /*println(Runtime.getRuntime().availableProcessors())
    (0..5).map {
        Thread {
            /*val ctx = Context()
            val p1 = ctx.mkRealConst("p1")
            val zero = ctx.mkReal("0")
            val solver = ctx.mkSolver()
            (0 until 500).forEach {
                println(it)
                val formula = ctx.mkGt(p1, zero)
                solver.add(formula)
                if (solver.check() != Status.SATISFIABLE) throw IllegalStateException()
                solver.reset()
            }*/

        }.start()
    }*/
    /*val c = Context()
    var counter = 0L
    var time = System.currentTimeMillis()
    while (true) {
        if (System.currentTimeMillis() - 1000 > time) {
            println("$counter/second")
            time = System.currentTimeMillis()
            counter = 0L
        }
        counter += 1
        val p = c.mkReal("123")
        val q = c.mkRealConst("x")
        val gt = c.mkGt(p, q)
    }*/
    /*val start = System.currentTimeMillis()
    val process = Runtime.getRuntime().exec("/Users/daemontus/Downloads/z3-master/build-clang/z3 -in")
    val output = process.outputStream.bufferedWriter()
    process.inputStream.bufferedReader().useLines { lines ->
        val chars = lines.flatMap {
            "$it ".asSequence() //add extra space as replacement for new-line
        }.iterator()
        println("Process started")
        output.write("(declare-const p1 Real)\n")
        output.write("(assert (< p1 0.0))\n")
        output.write("(check-sat)\n")
        output.flush()
        println("Flushed")
        chars.readSMT().apply(::println)
        output.write("(assert (< p1 0.0))\n")
        output.write("(apply ctx-solver-simplify)\n")
        output.flush()
        println("Flushed")
        chars.readSMT().apply(::println)
        //lines.forEach { println(it) }
        //chars.forEach { println(it) }
    }
    process.destroy()*/
}