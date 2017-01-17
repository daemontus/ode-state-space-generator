package com.github.sybila.ode.generator

import com.github.sybila.checker.Checker
import com.github.sybila.checker.SequentialChecker
import com.github.sybila.checker.channel.connectWithSharedMemory
import com.github.sybila.checker.partition.asUniformPartitions
import com.github.sybila.huctl.HUCTLParser
import com.github.sybila.ode.generator.smt.*
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    try {
        val cores = 1
        val pid = ManagementFactory.getRuntimeMXBean().name.takeWhile { it != '@' }
        System.err.println("PID: $pid")
        System.err.flush()

        val model = Parser().parse(File("/Users/daemontus/heap/sybila/tcbb.bio")).computeApproximation()
        val formulas = HUCTLParser().parse(File("/Users/daemontus/heap/sybila/test_prop.ctl"), onlyFlagged = true)

        val isRectangular = model.variables.all {
            it.equation.map { it.paramIndex }.filter { it >= 0 }.toSet().size <= 1
        }

        if (isRectangular) {
            val exe = Executors.newFixedThreadPool(cores)
            val models = (0 until cores).map {
                exe.submit(Callable {
                    val m = Z3OdeFragment(model)

                    /*for (state in 0 until m.stateCount) {
                        m.run {
                            state.successors(true)
                            state.predecessors(true)
                        }
                    }
                    println("Precomputed: ${m.stateCount}")*/
                    m

                })
            }.map { it.get() }.asUniformPartitions()
            exe.shutdown()
            repeat(10) {
                val start = System.currentTimeMillis()
                solverCalls.set(0)
                Checker(models.connectWithSharedMemory()).use { checker ->
                    System.err.println("Verification started...")
                    val r = checker.verify(formulas)
                    System.err.println("Verification finished. Printing results...")
                    /*for ((f, g) in r) {
                        println("$f: ${g.zip(models).map {
                            val (result, model) = it
                            model.run {
                                result.prettyPrint()
                            }
                        }}")
                    }*/
                    println("Calls: ${solverCalls.get()}")
                    val elapsed = System.currentTimeMillis() - start
                    println("Elapsed: $elapsed Throughput: ${solverCalls.get()/(elapsed/1000)}")
                    println("Sat: $satCalled from point $satFromPoint")
                    println("Sat time $satTime")
                    println("Min time $minTime")
                    println("C1: $c1")
                    println("C2: $c2")
                    println("Min called: $minCalled")
                    satCalled = 0
                    satFromPoint = 0
                }
            }
        } else {
            val start = System.currentTimeMillis()
            val fragment = Z3OdeFragment(model)
            SequentialChecker(fragment).use { checker ->
                fragment.run {
                    System.err.println("Verification started...")
                    val r = checker.verify(formulas)
                    System.err.println("Verification finished. Printing results...")
                    println("Elapsed: ${System.currentTimeMillis() - start}")
                }
            }
        }

        System.err.println("!!DONE!!")

    } catch (e: Exception) {
        e.printStackTrace()
        System.err.println("${e.message} (${e.javaClass.name})")
    }
}