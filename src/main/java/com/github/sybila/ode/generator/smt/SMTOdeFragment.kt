package com.github.sybila.ode.generator.smt

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.PartitionFunction
import com.github.sybila.ode.generator.AbstractOdeFragment
import com.github.sybila.ode.model.Model
import com.microsoft.z3.ArithExpr
import com.microsoft.z3.BoolExpr
import java.util.*

class SMTOdeFragment(
        model: Model,
        partitioning: PartitionFunction<IDNode>,
        createSelfLoops: Boolean = true,
        val order: PartialOrderSet = PartialOrderSet(model.parameters)
) : AbstractOdeFragment<SMTColors>(model, partitioning, createSelfLoops) {

    private val paramCount = model.parameters.size

    private val z3zero = z3.mkReal(0)
    internal val z3params = model.parameters.map {
        z3.mkRealConst(it.name)
    }

    val emptyCNF = CNF(setOf(Clause(setOf(), order)), order)
    val fullCNF = CNF(setOf(), order)

    override val emptyColors: SMTColors = SMTColors(emptyCNF, order, false)
    override val fullColors: SMTColors = if (model.parameters.isEmpty()) {
        SMTColors(fullCNF, order, true)
    } else {
        SMTColors(fullCNF, order, true)
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

            //since this is an or, transition is active if bool is true or formulas are not empty
            var transitionActive = false
            var formulas = ArrayList<BoolExpr>()

            for (coordinates in facet(from, dim, upper)) {
                val vertex = encoder.encodeVertex(coordinates)
                val results = evaluate(dim, coordinates, vertex)
                var expression: ArithExpr = z3zero
                var hasParam = false
                for (p in 0 until paramCount) {
                    if (results[p] != 0.0) {
                        hasParam = true
                        expression = expression plus (results[p].toZ3() times z3params[p])
                    }
                }
                if (hasParam) {
                    //add constant
                    expression = expression plus results[paramCount].toZ3()

                    val equation = if ((upper && incoming) || (!upper && !incoming)) {
                        expression lt z3zero
                    } else {
                        expression gt z3zero
                    }

                    formulas.add(equation)
                } else {
                    //This equation is without parameters, so we can eval it to true/false right away
                    if ((upper && incoming) || (!upper && !incoming)) {
                        if (results[paramCount] < 0.0) {
                            transitionActive = true
                        }
                    } else {
                        if (results[paramCount] > 0.0) {
                            transitionActive = true
                        }
                    }
                }
            }

            val relevant = order.addBiggest(formulas)
            val colors = when {
                relevant == null -> fullColors
                relevant.isEmpty() && transitionActive -> fullColors
                relevant.isEmpty() && !transitionActive -> emptyColors
                else -> {
                    SMTColors(CNF(setOf(Clause(relevant.toSet(), order)), order), order, true)
                }
            }

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