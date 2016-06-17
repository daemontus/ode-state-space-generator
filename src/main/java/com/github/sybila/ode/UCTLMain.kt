package com.github.sybila.ode

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.checker.uctl.*
import com.github.sybila.ctl.*
import com.github.sybila.ctl.Direction
import com.github.sybila.ode.generator.rect.RectangleColors
import com.github.sybila.ode.generator.rect.RectangleOdeFragment
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    val name = "HHR1N"

    //val property = UEU(true, UProposition(True), UProposition(True), Direction(0, true), anyDirection)
    /*val property = UBind("x", UEU(
            true,
            UProposition(True),
            UName("x"),
            anyDirection, anyDirection
    ))*/
    //E x: (x && At x: ! EF ! EF x)
    //val props = CTLParser().parse("p1 = E2F1 > 6.220146764509673; p2 = E2F1 < 6.568378919279519; p3 = pRB > 4.4129419613075385; p4 = pRB < 4.8332221480987325")
    //val x = UAnd(UAnd(UProposition(props["p1"] as Atom), UProposition(props["p2"] as Atom)), UAnd(UProposition(props["p3"] as Atom), UProposition(props["p4"] as Atom)))
    val init = UProposition(FloatProposition("ERK", CompareOp.LT, 0.0001))
    val down = UProposition(DirectionProposition("ERK", Direction.OUT, Facet.NEGATIVE))
    val stable = UAnd(
            UAnd(
                    UAnd(
                            UProposition(DirectionProposition("ERK", Direction.IN, Facet.POSITIVE)),
                            UNot(UProposition(DirectionProposition("ERK", Direction.OUT, Facet.POSITIVE)))
                    ),
                    UAnd(
                            UProposition(DirectionProposition("ERK", Direction.IN, Facet.NEGATIVE)),
                            UNot(UProposition(DirectionProposition("ERK", Direction.OUT, Facet.NEGATIVE)))
                    )
            ),
            UAnd(
                    UAnd(
                            UProposition(DirectionProposition("FRS2", Direction.IN, Facet.POSITIVE)),
                            UNot(UProposition(DirectionProposition("FRS2", Direction.OUT, Facet.POSITIVE)))
                    ),
                    UAnd(
                            UProposition(DirectionProposition("FRS2", Direction.IN, Facet.NEGATIVE)),
                            UNot(UProposition(DirectionProposition("FRS2", Direction.OUT, Facet.NEGATIVE)))
                    )
            ))
    val prop1 = UAnd(
            init,
            UAU(forward = true, path = UProposition(True), reach = UAU(forward = true,
                path = UProposition(True), reach = stable,
                    pathDirection = DNot(NameDirection("ERK", false)), reachDirection = anyDirection
            ), pathDirection = NameDirection("ERK", true), reachDirection = anyDirection)
    )
    val prop2 = UAnd(
            init,
            UEU(forward = true, path = UProposition(True), reach = stable,
                    pathDirection = NameDirection("ERK", true), reachDirection = anyDirection)
    )
    val prop3 = UAnd(
            init, UAU(forward = true,
                path = UProposition(True), reach = UAU(forward = true,
                    path = UProposition(True), reach = UAU(forward = true,
                        path = UProposition(True), reach = UAU(forward = true,
                            path = UProposition(True), reach = stable,
                        pathDirection = NameDirection("ERK", false), reachDirection = anyDirection),
                    pathDirection = DNot(NameDirection("ERK", true)), reachDirection = anyDirection),
                pathDirection = DNot(NameDirection("ERK", false)), reachDirection = anyDirection),
            pathDirection = NameDirection("ERK", true), reachDirection = anyDirection)
    )
    val dirDown = NameDirection("ERK", false)
    val dirNotDown = DNot(dirDown)
    val tt = UProposition(True)
    val prop4 = UAnd(
            init, UEX(forward = true, direction = dirNotDown, inner =
                UEU(forward = true, path = tt, pathDirection = dirNotDown, reachDirection = anyDirection, reach =
                UEX(forward = true, direction = dirDown, inner =
                UEU(forward = true, path = tt, pathDirection = dirDown, reachDirection = anyDirection, reach = stable)))
            )
    )
    //val efX = UEU(true, UProposition(True), UName("x"), anyDirection, anyDirection)
    //val stableX = UNot(UEU(true, UProposition(True), UNot(efX), anyDirection, anyDirection))
    //val property = UBind("x", stableX)
    //val property = UBind("x", UEX(true, UAnd(efX, UNot(UName("x"))), anyDirection))
    //E x : x && AX AF x
    /*val property = UExists("x", UAnd(UAt("x", UAX(true, UAU(
            true, UProposition(True), UName("x"), anyDirection, anyDirection
    ), anyDirection)), UName("x")))*/
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
    //val model = Parser().parse(File("models/$name.bio")).computeApproximation(fast = true, cutToRange = true)
    //val property = prop4
    val properties = listOf(prop1, prop2, prop3, prop4)
    val i = try {
        args[1].toInt()
    } catch (e: Exception) {
        0
    }
    val property = properties[i]
    val model = Parser().parse(File(args[0])).computeApproximation(fast = true, cutToRange = true)

    println("Normalized formula: $property")

    val fragment = RectangleOdeFragment(model, UniformPartitionFunction<IDNode>(), true)

    val checker = UModelChecker(fragment, fragment.emptyColors, fragment.fullColors)
    println("State count: ${fragment.allNodes().entries.count()}")
    val results = checker.verify(property, mapOf())
    println("Result count: ${results.entries.count()}")
    for (entry in results.entries) {
        println("${entry.key.prettyPrint(model, fragment.encoder)} - ${entry.value}")
        /*for (entry2 in results.entries) {
            if (entry != entry2 && entry.value.intersect(entry2.value).isNotEmpty()) {
                println("\t ${entry2.key.prettyPrint(model, fragment.encoder)} - ${entry2.value}")
            }
        }*/
    }

    //second phase!
    /*val prop2 = UAnd(UExistsIn("x", results, UExistsIn("y", results,
        UAnd(UEU(true, UProposition(True), UName("x"), DNot(Direction(1, false) /*!South*/), anyDirection), UAnd(
                UEU(true, UProposition(True), UName("y"), DNot(Direction(1, true) /*!North*/), anyDirection), UAnd(
                UAt("x", UNot(UEU(true, UProposition(True), UName("y"), anyDirection, anyDirection))),
                UAt("y", UNot(UEU(true, UProposition(True), UName("x"), anyDirection, anyDirection)))
                ))
    ))), UNot(UBind("z", UAX(false, UName("z"), anyDirection))))
    */

    /*val prop2 = UExistsIn("x", results, UExistsIn("y", results, UAnd( UName("x"),
            UAnd(
                    UAt("x", UNot(UEU(true, UProposition(True), UName("y"), anyDirection, anyDirection))),
                    UAt("y", UNot(UEU(true, UProposition(True), UName("x"), anyDirection, anyDirection)))
            ))))

    val results2 = results.subtract(checker.verify(prop2, mapOf()))
    println("Higher:")
    for (entry in results2.intersect(fragment.validNodes(FloatProposition("E2F1", CompareOp.GT, 4.0))).entries) {
        println("${entry.key.prettyPrint(model, fragment.encoder)} - ${entry.value}")
    }
    println("Lower:")
    for (entry in results2.intersect(fragment.validNodes(FloatProposition("E2F1", CompareOp.LT, 4.0))).entries) {
        println("${entry.key.prettyPrint(model, fragment.encoder)} - ${entry.value}")
    }*/
}