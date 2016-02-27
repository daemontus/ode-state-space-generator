package cz.muni.fi.ode.generator

import cz.muni.fi.checker.IDNode
import cz.muni.fi.checker.UniformPartitionFunction
import cz.muni.fi.ode.model.Parser
import cz.muni.fi.ode.model.computeApproximation
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
 * If you want to create/regenerate integration tests, use this code snippet to reproduce the format:
 *
 * val name = "model_31_reduced"
 * val writer = File("models/$name.transitions.txt").outputStream().bufferedWriter()
 * val model = Parser().parse(File("models/$name.bio")).computeApproximation()
 * val fragment = OdeFragment(model, UniformPartitionFunction<IDNode>())
 * for (node in fragment.allNodes().entries) {
 *    writer.write("Successors for ${node.key}:\n")
 *    for (successor in fragment.successors.invoke(node.key).entries) {
 *       writer.write("\t${successor.key} - ${successor.value}\n")
 *    }
 *    writer.write("Predecessors for ${node.key}:\n")
 *    for (predecessor in fragment.predecessors.invoke(node.key).entries) {
 *       writer.write("\t${predecessor.key} - ${predecessor.value}\n")
 *    }
 * }
 * writer.close()
 *
 */
class IntegrationTest {

    @Test fun TCBBTest() = integrationTest(File("models/tcbb.bio"), File("models/tcbb.transitions.txt"))
    @Test fun Model31Test() = integrationTest(File("models/model_31_reduced.bio"), File("models/model_31_reduced.transitions.txt"))

    private fun integrationTest(modelFile: File, resultsFile: File) {

        val model = Parser().parse(modelFile).computeApproximation()

        val results = resultsFile.inputStream().bufferedReader()

        val fragment = OdeFragment(model, UniformPartitionFunction<IDNode>())

        for (node in fragment.allNodes().entries) {
            assertEquals(results.readLine(), "Successors for ${node.key}:")
            for (successor in fragment.successors.invoke(node.key).entries) {
                assertEquals(results.readLine(), "\t${successor.key} - ${successor.value}")
            }
            assertEquals(results.readLine(), "Predecessors for ${node.key}:")
            for (predecessor in fragment.predecessors.invoke(node.key).entries) {
                assertEquals(results.readLine(), "\t${predecessor.key} - ${predecessor.value}")
            }
        }

    }

}