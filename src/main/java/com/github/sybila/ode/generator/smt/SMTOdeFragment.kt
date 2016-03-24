package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.PartitionFunction
import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.model.Model
import com.microsoft.z3.Context
import java.util.*

class SMTOdeFragment(
        model: Model,
        partitioning: PartitionFunction<IDNode>
) : AbstractOdeFragment<SMTColors>(model, partitioning) {

    private val paramCount = model.parameters.size

    internal val z3 = Context()
    internal val context = z3.run {
        SMTContext(this,
                this.mkTactic("ctx-solver-simplify"), this.mkGoal(false, false, false))
    }

    private val z3zero = z3.mkReal(0)
    internal val z3params = model.parameters.map {
        z3.mkRealConst(it.name)
    }

    override val emptyColors: SMTColors = SMTColors(z3.mkFalse(), context, false)
    override val fullColors: SMTColors = if (model.parameters.isEmpty()) {
        SMTColors(z3.mkTrue(), context, true)
    } else {
        SMTColors(z3.mkAnd(*model.parameters.flatMap { //* is a conversion to plain java varargs -_-
            val p = z3.mkRealConst(it.name)
            listOf((z3.mkGt(p, z3.mkReal(it.range.first.toString()))), z3.mkLt(p, z3.mkReal(it.range.second.toString())))
        }.toTypedArray()), context, true)
    }

    private val equationConstants = Array(dimensions) {
        Array<DoubleArray?>(encoder.vertexCount) { null }
    }

    private fun evaluate(dim: Int, coordinates: IntArray, vertex: Int): DoubleArray {
        val values = equationConstants[dim][vertex]
        if (values != null) {
            return values
        } else {
            val vector = DoubleArray(paramCount+1)
            for (summand in model.variables[dim].equation) {
                var partialSum = summand.constant
                for (v in summand.variableIndices) {
                    partialSum *= model.variables[v].thresholds[coordinates[v]]
                }
                if (partialSum != 0.0) {
                    for (function in summand.evaluable) {
                        val index = function.varIndex
                        partialSum *= function(model.variables[index].thresholds[coordinates[index]])
                    }
                }
                if (summand.hasParam()) {
                    vector[summand.paramIndex] += partialSum
                } else {
                    vector[paramCount] += partialSum
                }
            }
            equationConstants[dim][vertex] = vector
            return vector
        }
    }

    override fun getFacetColors(from: IDNode, dim: Int, upper: Boolean, incoming: Boolean): SMTColors {
        val myFacets = getFacets(from)
        val u = if (upper) 1 else 0
        val i = if (incoming) 1 else 0
        if (myFacets[dim][u][i] != null) {
            return myFacets[dim][u][i]!!;
        } else {
            //compute facet

            var formula = z3.mkOr()

            for (coordinates in facet(from, dim, upper)) {
                val vertex = encoder.encodeVertex(coordinates)
                val results = evaluate(dim, coordinates, vertex)
                var expression = z3.mkAdd(z3zero)
                for (p in 0 until paramCount) {
                    if (results[p] != 0.0) {
                        expression = z3.mkAdd(expression, z3.mkMul(z3.mkReal(results[p].toString()), z3params[p]))
                    }
                }
                //add constant
                expression = z3.mkAdd(expression, z3.mkReal(results[paramCount].toString()))

                val equation = if ((upper && incoming) || (!upper && !incoming)) {
                    z3.mkLt(expression, z3zero)
                } else {
                    z3.mkGt(expression, z3zero)
                }

                formula = z3.mkOr(formula, equation)
            }

            val colors = SMTColors(formula, context, null)

            //force colors to be simplified
            colors.normalize()

            myFacets[dim][u][i] = colors

            //also update facets for related nodes
            //(every facet is shared by two nodes, so if you compute upper incoming facet, you
            //have also computed lower outgoing facet for your upper neighbor
            if (upper) {
                encoder.higherNode(from, dim)?.apply {
                    val alternativeFacets = getFacets(this)
                    //lower facet, negation of incoming
                    alternativeFacets[dim][0][(i + 1) % 2] = colors
                }
            } else {
                encoder.lowerNode(from, dim)?.apply {
                    val alternativeFacets = getFacets(this)
                    //upper facet, negation of incoming
                    alternativeFacets[dim][1][(i + 1) % 2] = colors
                }
            }
            return colors
        }
    }


    /**
     * This map will cache all edge colors, so that we don't have to recompute them.
     */

    //First dimension: Nodes
    //second: dimensions
    //third: upper/lower facet
    //fourth: Incoming/outgoing
    private val facets = HashMap<IDNode, Array<Array<Array<SMTColors?>>>>()

    private fun getFacets(from: IDNode) = facets.getOrPut(from) { Array(dimensions) {
        Array(2) { Array<SMTColors?>(2) { null } }
    } }
}