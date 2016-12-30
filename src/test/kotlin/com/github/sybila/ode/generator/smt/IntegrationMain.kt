package com.github.sybila.ode.generator.smt

/*
/**
 * Use this main to regenerate integration tests.
 */
fun main(args: Array<String>) {
    val name = "tcbb"//"model_31_reduced"
    val writer = File("models/$name.transitions.smt.txt").outputStream().bufferedWriter()
    val model = Parser().parse(File("models/$name.bio")).computeApproximation()
    val fragment = SMTOdeFragment(model, UniformPartitionFunction<IDNode>())
    val sortedNodes = fragment.allNodes().entries.toList().sortedBy { it.key.id }
    var i = 0
    for (node in sortedNodes) {
        i += 1
        println("Done: $i/${sortedNodes.size}")
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
}*/