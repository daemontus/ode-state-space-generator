package com.github.sybila.ode.generator.rect

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.ode.generator.rect.RectangleOdeFragment
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

/**
 * This test takes previously computed results for known models and compares them to values obtained from
 * the new generator.
 *
 * It requires a one-to-one correspondence, so if it fails, first check whether you preserve ordering
 * of variables and stuff like that.
 *
 * Note: don't use these tests as benchmarks - the JIT compiler can brutally optimize whole process
 * but one test is not enough to see the results...
 *
 * If you want to create/regenerate integration tests, use attached main function.
 *
 * Note that ordering of nodes is preserved and each color set should have unique string representation
 * since it has only one rectangle.
 */


class IntegrationTest {

    //TODO: Reevaluate
    //@Test fun TCBBTest() = integrationTest(File("models/tcbb.bio"), File("models/tcbb.transitions.txt"))
    //@Test fun Model31Test() = integrationTest(File("models/model_31_reduced.bio"), File("models/model_31_reduced.transitions.txt"))

    private fun integrationTest(modelFile: File, resultsFile: File) {

        val model = Parser().parse(modelFile).computeApproximation()

        val results = resultsFile.inputStream().bufferedReader()

        val fragment = RectangleOdeFragment(model, UniformPartitionFunction<IDNode>())

        for (node in fragment.allNodes().entries.toList().sortedBy { it.key.id }) {
            assertEquals(results.readLine(), "Successors for ${node.key}:")
            for (successor in fragment.successors.invoke(node.key).entries.toList().sortedBy { it.key.id }) {
                assertEquals(results.readLine(), "\t${successor.key} - ${successor.value}")
            }
            assertEquals(results.readLine(), "Predecessors for ${node.key}:")
            for (predecessor in fragment.predecessors.invoke(node.key).entries.toList().sortedBy { it.key.id }) {
                assertEquals(results.readLine(), "\t${predecessor.key} - ${predecessor.value}")
            }
        }

    }

}