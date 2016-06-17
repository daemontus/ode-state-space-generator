package com.github.sybila.ode.generator.rect

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import com.github.sybila.ode.prettyPrint
import java.io.File

/**
 * Use this main to regenerate integration tests.
 */
fun main(args: Array<String>) {
    val name = "HHR1N"//"model_31_reduced"
    //val writer = File("models/$name.12.06.transitions.txt").outputStream().bufferedWriter()
    val model = Parser().parse(File("models/$name.bio")).computeApproximation(fast = true, cutToRange = true)
    val fragment = RectangleOdeFragment(model, UniformPartitionFunction<IDNode>())
    val sortedNodes = fragment.allNodes().entries.toList().sortedBy { it.key.id }
    val node = IDNode(1)
    for (predecessor in fragment.predecessors.invoke(node).entries.toList().sortedBy { it.key.id }) {
        println("\t${predecessor.key.prettyPrint(model, fragment.encoder)} - ${predecessor.value}\n")
    }
    /*for (node in sortedNodes) {
        writer.write("Successors for ${node.key}:\n")
        for (successor in fragment.successors.invoke(node.key).entries.toList().sortedBy { it.key.id }) {
            writer.write("\t${successor.key.prettyPrint(model, fragment.encoder)} - ${successor.value}\n")
        }
        writer.write("Predecessors for ${node.key}:\n")
        for (predecessor in fragment.predecessors.invoke(node.key).entries.toList().sortedBy { it.key.id }) {
            writer.write("\t${predecessor.key.prettyPrint(model, fragment.encoder)} - ${predecessor.value}\n")
        }
    }
    writer.close()*/
}