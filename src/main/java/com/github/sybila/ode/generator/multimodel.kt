package com.github.sybila.ode.generator

import com.github.sybila.checker.*
import com.github.sybila.checker.map.mutable.HashStateMap
import com.github.sybila.huctl.*
import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.generator.rect.RectangleOdeModel
import com.github.sybila.ode.generator.rect.RectangleSolver
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File

fun main(args: Array<String>) {
    val odeParser = Parser()
    val modelFile1 = File("E:\\test\\multimodel\\model1.bio")
    val model1 = odeParser.parse(modelFile1).computeApproximation()
    val modelFile2 = File("E:\\test\\multimodel\\model2.bio")
    val model2 = odeParser.parse(modelFile2).computeApproximation()

    val models = listOf(model1, model2)
    val mm = MultiModel(models)

    val prop = EX(EF("x".asVariable() gt 3.0.asConstant()))
    val props = HUCTLParser().parse("E:...")

    val mc = SequentialChecker(mm)
    val resuls = mc.verify(props)
    print(resuls)

    // Model with one extra parameter
    val extendedModel = model1.copy(parameters = model1.parameters + OdeModel.Parameter("Model", 0.0 to models.size.toDouble()))

    val jsonString = printJsonRectResults(extendedModel, resuls)
    File("output path").writeText(jsonString)
}

typealias RParams = MutableSet<Rectangle>

/**
 * Multi-model is a structure which unifies several ODE odeModels into one executable model.
 *
 * For now, we assume each model has the same state space and parameter space (You can always
 * extend incomplete model with dummy variables).
 *
 * Multi-model will then add one extra parameter which corresponds to the model index and append this
 * parameter to all parameter sets.
 */
class MultiModel(
        odeModels: List<OdeModel>,
        solver: RectangleSolver = run {
            val realParamCount = odeModels[0].parameters.size
            RectangleSolver(Rectangle(DoubleArray((realParamCount + 1) * 2) {
                val pIndex = it / 2
                val isLow = it % 2 == 0
                if (pIndex < realParamCount) {
                    val pRange = odeModels[0].parameters[pIndex].range
                    if (isLow) pRange.first else pRange.second
                } else {
                    if (isLow) 0.0 else odeModels.size.toDouble()
                }
            }))
        }
) : Model<RParams>, Solver<RParams> by solver {

    private val models = odeModels.map { RectangleOdeModel(it) }

    override val stateCount: Int = models.first().stateCount

    override fun Formula.Atom.Float.eval(): StateMap<RParams> {
        val prop = this
        val map = HashStateMap(ff)
        models.forEachIndexed { m, model -> model.run {
            val partialMap = prop.eval()
            partialMap.entries().forEach { (s, p) ->
                map.setOrUnion(s, p.setModelIndex(m))
            }
        } }

        return map
    }

    override fun Formula.Atom.Transition.eval(): StateMap<RParams> {
        // We don't need this. This is just for var:in+ propositions.
        TODO("not implemented")
    }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<RParams>> = successors(!timeFlow)

    private val cacheSuccessor = arrayOfNulls<List<Transition<RParams>>>(stateCount)
    private val cachePredecessor = arrayOfNulls<List<Transition<RParams>>>(stateCount)

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<RParams>> { // sucessor(s: Int, timeFlow: Boolean)
        val sourceState = this
        val cache = if (timeFlow) cacheSuccessor else cachePredecessor
        if (cache[sourceState] == null) {
            val transitions = models.mapIndexed { m, model -> model.run {
                // compute successors in a specific model and extend them with model index
                sourceState.successors(timeFlow).asSequence().map { it.target to it.bound.setModelIndex(m) }.toList()
            } }.flatMap { it }.groupBy({ it.first }, { it.second }).mapValues {(_, params) ->
                // merge successors from different models
                params.fold(ff) { a, b -> a or b }
            }.map { (s, p) ->
                // create transitions
                Transition(s, DirectionFormula.Atom.True, p)
            }
            // save to cache
            cache[sourceState] = transitions    // this can happen concurrently, but it is safe because value only increases.
        }

        return cache[sourceState]!!.iterator()
    }

    // extend all rectangles with appropriate model index
    private fun RParams.setModelIndex(modelIndex: Int): RParams = this.mapTo(HashSet()) {
        it.extend(modelIndex.toDouble(), (modelIndex + 1).toDouble())
    }

}