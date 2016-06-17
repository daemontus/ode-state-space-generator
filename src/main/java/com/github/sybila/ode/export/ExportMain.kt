package com.github.sybila.ode.export

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.ode.generator.rect.RectangleOdeFragment
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

fun main(args: Array<String>) {

    val pretty = "--pretty"
    val model = Parser().parse(File(args.filter { it != pretty }.last())).computeApproximation(fast = false, cutToRange = true)
    val fragment = RectangleOdeFragment(model, UniformPartitionFunction<IDNode>(), createSelfLoops = true)
    val export = Model()
    for ((name, range, thresholds) in model.variables) {
        val exportVar = Variable()
        exportVar.name = name
        exportVar.thresholds.addAll(thresholds)
        export.variables.add(exportVar)
    }

    for ((state, colors) in fragment.allNodes().entries) {
        for ((successor, transColors) in fragment.successors.invoke(state).entries) {
            val t = Transition()
            export.transitions.add(t)
            t.source = fragment.encoder.decodeNode(state)
            t.destination = fragment.encoder.decodeNode(successor)
            t.colours = transColors.toRectangle().asTwoDimensionalArray()
        }
    }

    if (args.contains(pretty)) {
        println(GsonBuilder().setPrettyPrinting().create().toJson(export))
    } else {
        println(Gson().toJson(export))
    }
}