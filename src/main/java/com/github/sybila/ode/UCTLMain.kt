package com.github.sybila.ode

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.checker.uctl.*
import com.github.sybila.ctl.True
import com.github.sybila.ode.generator.rect.RectangleOdeFragment
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File

fun main(args: Array<String>) {
    val name = "tcbb"

    //val property = UEU(true, UProposition(True), UProposition(True), Direction(0, true), anyDirection)
    /*val property = UBind("x", UEU(
            true,
            UProposition(True),
            UName("x"),
            anyDirection, anyDirection
    ))*/
    val property = UBind("x", UEX(true, UAU(
            true, UProposition(True), UName("x"), anyDirection, anyDirection
    ), anyDirection))
    /*val property = UExists("s",
            UAnd(UAt("s",
                    UNot(
                            UEU(true,
                                    UProposition(True),
                                    UNot(
                                            UEU(true,
                                                    UProposition(True),
                                                    UName("s"),
                                                    anyDirection, anyDirection)
                                    ), anyDirection, anyDirection))
            ), UName("s"))
    )*/
    val model = Parser().parse(File("models/$name.bio")).computeApproximation(fast = true, cutToRange = true)

    println("Normalized formula: $property")

    val fragment = RectangleOdeFragment(model, UniformPartitionFunction<IDNode>(), true)

    val checker = UModelChecker(fragment, fragment.emptyColors, fragment.fullColors)
    println("All: ${fragment.allNodes().entries.count()}")
    val results = checker.verify(property, mapOf())
    println("Result: ${results.entries.count()}")
    for (entry in results.entries) {
        println("${entry.key.prettyPrint(model, fragment.encoder)} - ${entry.value}")
    }
}