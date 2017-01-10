package com.github.sybila.ode.generator.shared

import com.github.sybila.checker.shared.Checker
import com.github.sybila.huctl.HUCTLParser
import com.github.sybila.ode.generator.shared.rect.RectangleOdeModel
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val elapsed = measureTimeMillis {
        val model = Parser().parse(File("/Users/daemontus/heap/sybila/models/tcbb.bio")).computeApproximation()
        val prop = HUCTLParser().parse(File("/Users/daemontus/heap/sybila/models/test_prop.huctl"))

        val fragment = RectangleOdeModel(model)
        println()
/*
        for (state in 0 until fragment.stateCount) {
            fragment.run {
                state.successors(true)
                state.predecessors(true)
            }
        }
        println("Precomputed: ${fragment.stateCount}")*/

        Checker(fragment, parallelism = 1).use { checker ->
            val r = checker.verify(prop)
            for ((f, map) in r) {
                println("$f ${map.entries.count()}")
            }
        }
    }
    println("Elapsed: $elapsed")
}