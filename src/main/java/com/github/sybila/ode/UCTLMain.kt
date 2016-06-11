package com.github.sybila.ode

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.checker.uctl.*
import com.github.sybila.ctl.*
import com.github.sybila.ode.generator.rect.RectangleColors
import com.github.sybila.ode.generator.rect.RectangleOdeFragment
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    val name = "tcbb"

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
    val efX = UEU(true, UProposition(True), UName("x"), anyDirection, anyDirection)
    val stableX = UNot(UEU(true, UProposition(True), UNot(efX), anyDirection, anyDirection))
    //val property = UExists("x", UAnd(UName("x"), UAt("x", stableX)))
    val property = UBind("x", stableX)
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
    val model = Parser().parse(File("models/$name.bio")).computeApproximation(fast = true, cutToRange = true)

    println("Normalized formula: $property")

    val fragment = RectangleOdeFragment(model, UniformPartitionFunction<IDNode>(), true)

    val checker = UModelChecker(fragment, fragment.emptyColors, fragment.fullColors)
    println("All: ${fragment.allNodes().entries.count()}")
    val results = checker.verify(property, mapOf())
    println("Result: ${results.entries.count()}")
    /*for (entry in results.entries) {
        println("FOR ${entry.key.prettyPrint(model, fragment.encoder)} - ${entry.value}")
        /*for (entry2 in results.entries) {
            if (entry != entry2 && entry.value.intersect(entry2.value).isNotEmpty()) {
                println("\t ${entry2.key.prettyPrint(model, fragment.encoder)} - ${entry2.value}")
            }
        }*/
    }*/

    //second phase!
    /*val prop2 = UAnd(UExistsIn("x", results, UExistsIn("y", results,
        UAnd(UEU(true, UProposition(True), UName("x"), DNot(Direction(1, false) /*!South*/), anyDirection), UAnd(
                UEU(true, UProposition(True), UName("y"), DNot(Direction(1, true) /*!North*/), anyDirection), UAnd(
                UAt("x", UNot(UEU(true, UProposition(True), UName("y"), anyDirection, anyDirection))),
                UAt("y", UNot(UEU(true, UProposition(True), UName("x"), anyDirection, anyDirection)))
                ))
    ))), UNot(UBind("z", UAX(false, UName("z"), anyDirection))))
    */

    val prop2 = UExistsIn("x", results, UExistsIn("y", results, UAnd( UName("x"),
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
    }
}