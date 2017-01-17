package com.github.sybila.ode.generator.smt

import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.safeString
import java.util.*

class Z3OdeFragment(
        model: OdeModel,
        createSelfLoops: Boolean = true,
        solver: Z3Solver = Z3Solver(
                model.parameters.map { it.range },
                model.parameters.map { it.name }
        )
) : AbstractOdeFragment<Z3Params>(model, createSelfLoops, solver), Z3SolverBase by solver {

    private val positiveVertexCache = HashMap<Int, List<Z3Params?>>()
    private val negativeVertexCache = HashMap<Int, List<Z3Params?>>()

    private val const = DoubleArray(model.parameters.size + 1) //evaluation results cache

    override fun getVertexColor(vertex: Int, dimension: Int, positive: Boolean): Z3Params? {
        return (if (positive) positiveVertexCache else negativeVertexCache).computeIfAbsent(vertex) {
            val p: List<Z3Params?> = (0 until dimensions).map { dim ->

                //evaluate equations
                Arrays.fill(const, 0.0)
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
                        const[summand.paramIndex+1] += partialSum
                    } else {
                        const[0] += partialSum
                    }
                }

                val solution = const.asSequence()
                val constant = solution.take(1).map { if (it == 0.0) null else it.safeString() }
                val params = solution.drop(1).mapIndexed { i, d ->
                    if (d == 0.0) null else "(* ${model.parameters[i].name} ${d.safeString()})"
                }
                val equation = (constant + params).filterNotNull().toList()
                val eq: Z3Params? = if (equation.isEmpty()) {
                    null
                } else {
                    val cmp = if (positive) ">" else "<"
                    val formula = equation.joinToString(prefix = "($cmp (+ ", postfix = ") 0.0)", separator = " ")
                    Z3Params(formula, null, false)
                }

                eq?.minimize(true)
                if (eq?.isSat() ?: true) eq else null
            }
            //null only if all is zero, dual value is then also zero (null)
            //TODO weird (if (positive) negativeVertexCache else positiveVertexCache)[vertex] = p.map { it?.not() }
            p
        }[dimension]
    }

}