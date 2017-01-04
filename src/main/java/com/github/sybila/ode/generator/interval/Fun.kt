package com.github.sybila.ode.generator.interval

import com.github.sybila.checker.SequentialChecker
import com.github.sybila.huctl.HUCTLParser
import com.github.sybila.ode.generator.rect.RectangleOdeModel
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File

fun main(args: Array<String>) {
    val model = Parser().parse(File("/Users/daemontus/heap/sybila/models/tcbb.bio")).computeApproximation()
    val prop = HUCTLParser().parse(File("/Users/daemontus/heap/sybila/models/test_prop.huctl"), onlyFlagged = true)
    val fragment = IntervalOdeModel(model)

    val start = System.currentTimeMillis()
    fragment.run {
        SequentialChecker(fragment).use { checker ->
            val r = checker.verify(prop)
            println("Elapsed: ${System.currentTimeMillis() - start}")
            r.forEach { println("${it.key}: ${it.value.entries().asSequence().filter { it.second.isSat() }.count()}") }
        }
    }

}