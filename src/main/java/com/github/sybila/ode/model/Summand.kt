package com.github.sybila.ode.model

data class Summand(
        val constant: Double = 1.0,
        val paramIndex: Int = -1,
        val variableIndices: List<Int> = listOf(),
        val evaluable: List<Evaluable> = listOf()
) {

    constructor(
            constant: Double = 1.0,
            paramIndex: Int = -1,
            variableIndices: List<Int> = listOf(),
            vararg evaluables: Evaluable
    ) : this(constant, paramIndex, variableIndices, evaluables.toList())

    fun hasParam(): Boolean = paramIndex >= 0

    operator fun times(other: Summand): Summand {
        if (paramIndex >= 0 && other.paramIndex >= 0) {
            throw IllegalStateException("Can't create summand with two parameters or squared parameter! Parameter indices: $paramIndex, ${other.paramIndex}")
        }
        return Summand(
                constant * other.constant,
                Math.max(paramIndex, other.paramIndex),
                variableIndices + other.variableIndices,
                evaluable + other.evaluable
        )
    }

    operator fun plus(other: Summand): Summand? {
        if (
            paramIndex != other.paramIndex ||
            variableIndices != other.variableIndices ||
            evaluable != other.evaluable
        ) {
            return null
        } else {
            return this.copy(constant + other.constant)
        }
    }



    override fun toString(): String {
        val root = "$constant*Param($paramIndex)"
        val elements = variableIndices.map { "Var($it)" } + evaluable
        if (elements.isNotEmpty()) {
            return "$root*${elements.joinToString(separator = "*")}"
        } else return root
    }

}