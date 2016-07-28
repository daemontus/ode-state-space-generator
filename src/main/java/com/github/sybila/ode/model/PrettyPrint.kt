package com.github.sybila.ode.model

import java.util.*


fun RampApproximation.prettyPrint(): String {
    return (0 until thresholds.size-1).map { i ->
        "ramp(${thresholds[i]}, ${thresholds[i+1]}, ${values[i]}, ${values[i+1]})"
    }.joinToString(separator = "+", prefix = "(", postfix = ")")
}

fun Summand.prettyPrint(model: Model): String {
    val root = if (paramIndex >= 0) "($constant)*${model.parameters[paramIndex].name}" else "($constant)"
    val elements = variableIndices.map { model.variables[it].name } + evaluable.map {
        if (it is RampApproximation) {
            it.prettyPrint()
        } else {
            it.toString()
        }
    }
    if (elements.isNotEmpty()) {
        return "$root*${elements.joinToString(separator = "*")}"
    } else return root
}

fun Model.prettyPrint(): String {
    return variables.map { it.equation }.map {
        it.joinToString(separator = "+", transform = { it.prettyPrint(this) })
    }.joinToString(separator = "\n")
}

fun <T> Iterable<T>.chunksOf(size: Int): List<List<T>> {
    val results = ArrayList<List<T>>()
    val iter = this.iterator()
    while (iter.hasNext()) {
        val chunk = ArrayList<T>()
        while (iter.hasNext() && chunk.size < size) {
            chunk.add(iter.next())
        }
        results.add(chunk)
    }
    return results
}