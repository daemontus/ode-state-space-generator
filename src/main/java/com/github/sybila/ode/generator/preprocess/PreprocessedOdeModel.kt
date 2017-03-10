package com.github.sybila.ode.generator.preprocess

import com.github.sybila.checker.SequentialChecker
import com.github.sybila.huctl.HUCTLParser
import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.computeApproximation
import java.io.File
import java.util.*

class PreprocessedOdeModel(
        model: OdeModel,
        private val paramCoder: ParamCoder,
        createSelfLoops: Boolean = true
) : AbstractOdeFragment<MutableSet<Rectangle>>(model, createSelfLoops, RectangleSolver(Rectangle(
        model.parameters.indices.flatMap { listOf(0, paramCoder.lastIndex(it)) }.toIntArray()
))) {

    private val boundsRect = model.parameters.indices.flatMap { listOf(0, paramCoder.lastIndex(it)) }.toIntArray()

    private val positiveVertexCache = HashMap<Int, List<MutableSet<Rectangle>?>>()
    private val negativeVertexCache = HashMap<Int, List<MutableSet<Rectangle>?>>()

    override fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): MutableSet<Rectangle>? {
        return (if (positive) positiveVertexCache else negativeVertexCache).computeIfAbsent(vertex) {
            val p: List<MutableSet<Rectangle>?> = (0 until dimensions).map { dim ->
                var derivationValue = 0.0
                var denominator = 0.0
                var parameterIndex = -1

                //evaluate equations
                for (summand in model.variables[dim].equation) {
                    var partialSum = summand.constant
                    for (v in summand.variableIndices) {
                        partialSum *= model.variables[v].thresholds[encoder.vertexCoordinate(vertex, v)]
                    }
                    if (partialSum != 0.0) {
                        for (function in summand.evaluable) {
                            val index = function.varIndex
                            partialSum *= function(model.variables[index].thresholds[encoder.vertexCoordinate(vertex, index)])
                        }
                    }
                    if (summand.hasParam()) {
                        parameterIndex = summand.paramIndex
                        denominator += partialSum
                    } else {
                        derivationValue += partialSum
                    }
                }

                val bounds: MutableSet<Rectangle>? = if (parameterIndex == -1 || denominator == 0.0) {
                    //there is no parameter in this equation
                    if (derivationValue > 0 == positive) tt else ff
                } else {
                    //if you divide by negative number, you have to flip the condition
                    val newPositive = if (denominator > 0) positive else !positive
                    val range = 0 to paramCoder.lastIndex(parameterIndex)
                    //min <= split <= max
                    val split = min(
                            range.second,
                            max(
                                    range.first,
                                    paramCoder.valueToIndex(-derivationValue / denominator, parameterIndex)
                            )
                    )
                    val newLow = if (newPositive) split else range.first
                    val newHigh = if (newPositive) range.second else split

                    if (newLow >= newHigh) null else {
                        val r = boundsRect.clone()
                        r[2*parameterIndex] = newLow
                        r[2*parameterIndex+1] = newHigh
                        mutableSetOf(Rectangle(r))
                    }
                }
                bounds
            }
            //save also dual values
            (if (positive) negativeVertexCache else positiveVertexCache)[vertex] = p.map { it?.not() ?: tt }
            p
        }[dimension]
    }

}

fun main(args: Array<String>) {
    val model = File("/Users/daemontus/heap/sybila/terminal-components/for_benchmark/enumerative/repressilators - affine/2D/model_2D_2P_10kR.bio")
    val odeModel = Parser().parse(model).computeApproximation(fast = true)

    var timer = System.currentTimeMillis()
    val paramCoder = ParamCoder(odeModel)
    println("Pre-processing: ${System.currentTimeMillis() - timer}")

    val property = HUCTLParser().formula("EF (x > 10 || y > 10)")

    val checker = SequentialChecker(PreprocessedOdeModel(odeModel, paramCoder, true))

    checker.run {
        repeat(10) {
            timer = System.currentTimeMillis()
            val size = verify(property).states().asSequence().count()
            println("Evaluation: $size ${System.currentTimeMillis() - timer} ")
        }
    }

    checker.close()
}