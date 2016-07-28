package com.github.sybila.ode

import com.github.sybila.checker.UniformPartitionFunction
import com.github.sybila.ode.generator.rect.RectangleOdeFragment
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import com.github.sybila.ode.model.prettyPrint
import java.io.File

fun main(args: Array<String>) {
    val model = Parser().parse(File(args.last())).computeApproximation(fast = true, cutToRange = true)
    val fragment = RectangleOdeFragment(model, UniformPartitionFunction())
    fragment.printDerivations()
}