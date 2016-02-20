package cz.muni.fi.ode

import cz.muni.fi.ode.model.Parser
import java.io.File


fun main(args: Array<String>) {

    val parser = Parser()

    val model = parser.parse(File("/Users/daemontus/Workspace/Sybila/Runtime/ODE/model.bio"))

    println("Original model: $model")

    println("Approximation: ${model.computeApproximation()}")

}