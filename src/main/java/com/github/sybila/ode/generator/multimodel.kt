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
    val modelFile1 = File("E:\\test\\multimodel\\model1.bio")
    val model1 = odeParser.parse(modelFile1).computeApproximation()
    val gen1 = RectangleOdeModel(model1)
    val enc1 = NodeEncoder(model1)
    val modelFile2 = File("E:\\test\\multimodel\\model2.bio")
    val model2 = odeParser.parse(modelFile2).computeApproximation()
    val gen2 = RectangleOdeModel(model2)
    val enc2 = NodeEncoder(model2)

    val mm = Multimodel(listOf(gen1,gen2), listOf(enc1, enc2), model1.parameters.size,gen1)


}

typealias RParams = MutableSet<Rectangle>

class Multimodel(
        private val models: List<RectangleOdeModel>,
        private val encoders: List<NodeEncoder>,
        private val pCount: Int,
        solver: Solver<RParams>
) : Model<RParams>, Solver<RParams> by solver {

    override val stateCount: Int
    val dimensions: Int
    val modelCount: Int


    init {
        // assuming the models got unified variables with same thresholds, hence same states
        //I
        modelCount = models.size
        println("# of models: " + modelCount)

        //dim (new dimensions)
        var tempdimensions=0
        for ((model, encoder) in models.zip(encoders)) if (encoder.dimensions > tempdimensions) tempdimensions=encoder.dimensions
        dimensions=tempdimensions
        println("new dimensions: " +dimensions)

        for ((model, encoder) in models.zip(encoders)) println("state count: " + model.stateCount + " dimensions: " +encoder.dimensions)

        /* checking dimensions
        for ((model, encoder) in models.zip(encoders)) println("state count: " + model.stateCount + " dimensions: " +encoder.dimensions + " dimensionStateCounts: " + encoder.dimensionStateCounts.joinToString())
        */

        /*
        var i:Int
        i=0
        for ((model, encoder) in models.zip(encoders)) {
            i++
            for (node in 1..model.stateCount) println("model: "+ i +" node "+ node + " ="+encoder.decodeNode(node).joinToString())
        }
        */
        
        this.stateCount = models[0].stateCount
        println(stateCount)
    }



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