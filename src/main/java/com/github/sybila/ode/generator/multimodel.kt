package com.github.sybila.ode.generator

import com.github.sybila.checker.Model
import com.github.sybila.checker.Solver
import com.github.sybila.checker.StateMap
import com.github.sybila.checker.Transition
import com.github.sybila.huctl.Formula
import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.generator.rect.RectangleOdeModel
import com.github.sybila.ode.generator.rect.RectangleSolver
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File

fun main(args: Array<String>) {
    val odeParser = Parser()
    val modelFile1 = File("/meej/...")
    val model1 = odeParser.parse(modelFile1).computeApproximation()
    val gen1 = RectangleOdeModel(model1)
    val modelFile2 = File("/meej/...")
    val model2 = odeParser.parse(modelFile1).computeApproximation()
    val gen2 = RectangleOdeModel(model1)

    val mm = Multimodel(listOf(gen1,gen2),model1.parameters.size,gen1)
}

typealias RParams = MutableSet<Rectangle>

class Multimodel(
        private val models: List<RectangleOdeModel>,
        private val pCount: Int,
        solver: Solver<RParams>
) : Model<RParams>, Solver<RParams> by solver {

    override val stateCount: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun Formula.Atom.Float.eval(): StateMap<RParams> {

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun Formula.Atom.Transition.eval(): StateMap<RParams> {
        TODO("not implemented")
    }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<RParams>> = successors(!timeFlow)

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<RParams>> { // sucessor(s: Int, timeFlow: Boolean)
        // s.successors(true)
        // successors(s, true)
        val s = this
        val m1 = models[0]
        m1.run {
            val rect = tt.iterator().next()
            val p = rect.restrict(pCount - 1, 0.0, 1.0)
            val pSet = mutableSetOf(p)
            val list: List<Transition<RParams>> = s.successors(timeFlow).asSequence().map {
                it.copy(bound = it.bound and pSet)
            }.toList()
        }

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}