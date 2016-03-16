package com.github.sybila.ode.generator

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File

/**
 * Use this main to regenerate integration tests.
 */
fun main(args: Array<String>) {
    val name = "model_31_reduced"
    val writer = File("models/$name.transitions.txt").outputStream().bufferedWriter()
    val model = Parser().parse(File("models/$name.bio")).computeApproximation()
    val fragment = RectangleOdeFragment(model, UniformPartitionFunction<IDNode>())
    val sortedNodes = fragment.allNodes().entries.toList().sortedBy { it.key.id }
    for (node in sortedNodes) {
        writer.write("Successors for ${node.key}:\n")
        for (successor in fragment.successors.invoke(node.key).entries.toList().sortedBy { it.key.id }) {
            writer.write("\t${successor.key} - ${successor.value}\n")
        }
        writer.write("Predecessors for ${node.key}:\n")
        for (predecessor in fragment.predecessors.invoke(node.key).entries.toList().sortedBy { it.key.id }) {
            writer.write("\t${predecessor.key} - ${predecessor.value}\n")
        }
    }
    writer.close()
}