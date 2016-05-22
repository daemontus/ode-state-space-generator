package com.github.sybila.ode

import com.github.sybila.checker.FunctionalPartitionFunction
import com.github.sybila.checker.IDNode
import com.github.sybila.checker.Nodes
import com.github.sybila.checker.withModelCheckers
import com.github.sybila.ctl.CTLParser
import com.github.sybila.ctl.normalize
import com.github.sybila.ode.generator.NodeEncoder
import com.github.sybila.ode.generator.rect.RectangleColors
import com.github.sybila.ode.generator.smt.*
import com.github.sybila.ode.model.Model
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File
import java.util.*
import java.util.logging.Logger

fun main(args: Array<String>) {

  /*  val p = z3.mkRealConst("p1")
    val p2 = z3.mkRealConst("p2")


    fun Double.toZ3() = z3.mkReal(this.toString())

    val zero = 0.0.toZ3()

    val set = PartialOrderSet(z3, listOf())

    println("Set: $set")
    set.add(z3.mkGt(p, 3.0.toZ3()))

    println(z3.mkNot(z3.mkNot(z3.mkGt(p, zero))).simplify())
*/
    /*val tactic = z3.mkTactic("qflra")
    val solver = z3.mkSolver(tactic)
    //val tactic = z3.mkTactic("ctx-solver-simplify")
    var goal = z3.mkGoal(false, false, false)
    val f = z3.parseSMTLIB2String("""
        (declare-const y_pRB Real)
        (assert (and (not (<= y_pRB (/ 630614511166382.0 246136313097620625.0)))
     (> y_pRB 0.0)
     (< y_pRB 1.0)))
    """, null, null, null, null)

    val bounds = z3.parseSMTLIB2String("""
        (declare-const y_pRB Real)
        (assert (and
     (> y_pRB 0.0)
     (< y_pRB 1.0)))
    """, null, null, null, null)

    val extra = z3.parseSMTLIB2String("""
        (declare-const y_pRB Real)
        (assert (< y_pRB (/ 1.0 2.0)))
    """, null, null, null, null)
    println("bounds: $bounds, extra: $extra")
    goal.add(f)
    goal.add(f)

    val start = System.currentTimeMillis()
    //solver.add(f)
    solver.add(bounds)
    //solver.add(extra)
    (0..1000).map {
        //goal.add(f)
        //println(solver.check())
        if (solver.check(extra) != Status.SATISFIABLE) throw IllegalStateException("Problem!")
        //assert(solver.check() == Status.SATISFIABLE)
        //assert(tactic.apply(goal).subgoals.size == 1)
      //  goal = tactic.apply(goal).subgoals.first()
    }
    println("Total time: ${System.currentTimeMillis() - start}")
*/

/*
Normalized formula: (pRB > 3.0 EU E2F1 > 6.0)
results: 216
Solver calls: 926/2329/3087, time in solver: 76132, cache hit: 169
Approx: 1462, computation: 76800
 */
    val name = "tcbb"//"model_31_reduced"

   // while (true) {
        val start = System.currentTimeMillis()
        val property = CTLParser().formula("pRB > 3.0 EU E2F1 > 6.0").normalize()
                // ("AF ! EF ! (GLY > 1.5 && ATOX < 3)") "AG (E2F1 > 4 && E2F1 < 7.5)" (pRB > 3.0 EU E2F1 > 6.0)
        val model = Parser().parse(File("models/$name.bio")).computeApproximation(fast = true, cutToRange = true)
        val encoder = NodeEncoder(model)

        println("Normalized formula: $property")
        //println("Model: $model")
        val processCount = 1

        val partitions = (0 until processCount).map { FunctionalPartitionFunction<IDNode>(it) { node -> node.id % processCount } }

        val fragments = partitions.map { SMTOdeFragment(model, it, createSelfLoops = false, order = PartialOrderSet(
                model.parameters, chainFile = File("chain.smt"), tautologyFile = File("taut.smt"), unsatFile = File("unsat.smt")
                )) }
        //val fragment = OdeFragment(model, UniformPartitionFunction<IDNode>())

        val computeStart = System.currentTimeMillis()

      //  println(fragment.successors.invoke(encoder.encodeNode(intArrayOf(11,12))).prettyPrint(model, encoder))
        if (false) {
            val f = fragments.first()
            println(f.allNodes().entries.count())
            var progress = 0
            f.allNodes().entries.forEach {
                if (progress % 10 == 0) {
                    println("Size: ${fragments.first().order.size} Width: ${fragments.first().order.width} Progress: $progress")
                }
                progress += 1
                fragments.first().successors.invoke(it.key)
            }
            //f.order.serialize(File("chain.smt"), File("taut.smt"), File("unsat.smt"))
        } else {

            withModelCheckers(fragments, { partitions[it] }, { Logger.getLogger("test") }) {
                println("start verification")
                val results = it.verify(property)
                for (entry in results.entries) {
                    //  entry.value.validate()
                    //if ((entry.value - fragments.first().fullColors).isNotEmpty()) throw IllegalStateException("Invalid: ${entry.value}")
                    //if ((fragments.first().fullColors - entry.value).isNotEmpty()) throw IllegalStateException("Invalid: ${entry.value}")
                    //println("${entry.key.prettyPrint(model, encoder)} - ${entry.value}")
                }
                println("results: ${results.entries.count()}")
            }
            /*
            withModelCheckers(fragments, partitions) { checker ->

                val results = checker.verify(property)
                            //for (entry in results.entries) {
                              //  entry.value.validate()
                              // println("${entry.key.prettyPrint(model, encoder)} - ${entry.value.normalize()}")
                          //}
                println("results: ${results.entries.count()}")
            }*/
        }

        println("Size: ${fragments.first().order.size} Width: ${fragments.first().order.width}")
   // println(fragments.first().order.toString())

        println("Solver calls: $solverCalls/$simplifyCalls, time in simplify: ${
            timeInSimplify / (1000 * 1000)
        } time in solver: ${timeInSolver / (1000 * 1000)}, time in ordering: ${timeInOrdering / 1000000L
            }, cache hit: $solverCacheHit")

        println("Approx: ${computeStart - start}, computation: ${System.currentTimeMillis() - computeStart}")
  //  }

}

fun Nodes<IDNode, RectangleColors>.prettyPrint(model: Model, encoder: NodeEncoder): String {
    return this.entries.map {
        "\n${it.key.prettyPrint(model, encoder)} - ${it.value}"
    }.joinToString()
}

fun IDNode.prettyPrint(model: Model, encoder: NodeEncoder): String {
    val coordinates = encoder.decodeNode(this)
    return coordinates.mapIndexed { i, c ->
        val t = model.variables[i].thresholds
        "[${t[c]},${t[c+1]}]($c)"
    }.joinToString()+"{${this.id}}"
}