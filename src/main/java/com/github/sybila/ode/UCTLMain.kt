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
    val name = "HHR1Or_new"

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
    val dirUp = NameDirection("ERK", true)
    val dirDown = NameDirection("ERK", false)
    val dirNotUp = DNot(dirUp)
    val dirNotDown = DNot(dirDown)
    val tt = UProposition(True)

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

    val init = UAnd(UProposition(FloatProposition("ERK", CompareOp.LT, 0.0001)), UNot(stable))
    val notUp = UNot(UProposition(DirectionProposition("ERK", Direction.OUT, Facet.POSITIVE)))

    fun EF(pathDirection: DFormula, reach: UFormula): UFormula {
        return UEU(forward = true, pathDirection = pathDirection, reachDirection = anyDirection, path = tt, reach = reach)
    }

    fun AF(pathDirection: DFormula, reach: UFormula): UFormula {
        return UAU(forward = true, pathDirection = pathDirection, reachDirection = anyDirection, path = tt, reach = reach)
    }

    fun EX(pathDirection: DFormula, reach: UFormula): UFormula {
        return UEX(forward = true, direction = pathDirection, inner = reach)
    }

    fun AX(pathDirection: DFormula, reach: UFormula): UFormula {
        return UAX(forward = true, direction = pathDirection, inner = reach)
    }

    val p1 = UAnd(init, AX(dirUp, AF(dirNotDown, stable)))

    val p2 = UAnd(init, EF(anyDirection, EX(dirDown, tt)))

    val p3 = UAnd(init, EF(anyDirection, EX(dirDown, notUp)))

    val p4 = UAnd(init,
            EF(anyDirection,
                EF(dirUp,
                    EF(dirNotDown,
                        EF(dirNotUp,
                            EX(dirDown, tt)
                        )
                    )
                )
            )
    )

    val p5 = UAnd(init,
            EF(anyDirection,
                EF(dirUp,
                    EF(dirNotDown,
                        EF(dirNotUp,
                            EX(dirDown, notUp)
                        )
                    )
                )
            )
    )

    val p6 = UAnd(init,
            AX(dirUp,
                AF(dirNotDown,
                    AF(dirNotUp,
                        AX(dirDown,
                            AF(dirNotUp, stable)
                        )
                    )
                )
            )
    )

    val t1 = AF(anyDirection, EX(dirDown, stable))
    val t2 = EX(dirUp, AF(anyDirection, EX(dirDown, stable)))

    //val model = Parser().parse(File("models/$name.bio")).computeApproximation(fast = true, cutToRange = true)
    //val property = p3//UAnd(init, AF(dirUp, stable)) //UAnd(p5, UNot(p3))

    //println("Model: $model")
    val properties = listOf(p1, p2, p3, p4, p5, p6, t1, t2)
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