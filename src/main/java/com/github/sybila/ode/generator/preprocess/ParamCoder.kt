package com.github.sybila.ode.generator.preprocess

import com.github.sybila.ode.generator.NodeEncoder
import com.github.sybila.ode.model.OdeModel
import java.util.*

class ParamCoder(model: OdeModel) {

    private val stateCoder = NodeEncoder(model)

    val mapping: Array<DoubleArray> = Array(model.parameters.size) { pIndex ->
        val range = model.parameters[pIndex].range
        val result = hashSetOf(range.first, range.second)
        for ((_, _, _, _, eq) in model.variables) {
            // WARNING: Here we assume that the model is rectangular, otherwise this is not correct...
            val param = eq.first { it.hasParam() }.paramIndex
            if (param == pIndex) {
                for (vertex in 0 until stateCoder.vertexCount) {
                    var derivationValue = 0.0
                    var denominator = 0.0

                    //evaluate equations
                    for (summand in eq) {
                        var partialSum = summand.constant
                        for (v in summand.variableIndices) {
                            partialSum *= model.variables[v].thresholds[stateCoder.vertexCoordinate(vertex, v)]
                        }
                        if (partialSum != 0.0) {
                            for (function in summand.evaluable) {
                                val index = function.varIndex
                                partialSum *= function(model.variables[index].thresholds[stateCoder.vertexCoordinate(vertex, index)])
                            }
                        }
                        if (summand.hasParam()) {
                            denominator += partialSum
                        } else {
                            derivationValue += partialSum
                        }
                    }

                    if (denominator != 0.0) {
                        val split = -derivationValue / denominator
                        if (split > range.first && split < range.second) {
                            result.add(split)
                        }
                    }
                }
            }
        }

        val resultArray = result.toDoubleArray()
        Arrays.sort(resultArray)
        resultArray
    }

    fun indexToValue(i: Int, p: Int): Double {
        return mapping[p][i]
    }

    fun valueToIndex(v: Double, p: Int): Int {
        return Arrays.binarySearch(mapping[p], v).apply {
            if (this < 0) throw IllegalStateException("Unexpected value $v for parameter $p")
        }
    }

    fun lastIndex(p: Int): Int {
        return mapping[p].lastIndex
    }

}