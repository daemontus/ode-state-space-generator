package cz.muni.fi.ode.model

data class Summand(
        val constant: Double = 1.0,
        val paramIndex: Int = -1,
        val variableIndices: List<Int> = listOf(),
        val evaluable: List<Evaluable> = listOf()
) {

    fun hasParam(): Boolean = paramIndex >= 0

    operator fun times(other: Summand): Summand {
        if (paramIndex >= 0 && other.paramIndex >= 0) {
            throw IllegalStateException("Can't create summand with two parameters or squared parameter! Parameter indices: $paramIndex, $paramIndex")
        }
        return Summand(
                constant * other.constant,
                Math.max(paramIndex, other.paramIndex),
                variableIndices + other.variableIndices,
                evaluable + other.evaluable
        )
    }

    override fun toString(): String {
        val root = "$constant*Param($paramIndex)}"
        val elements = listOf(
                variableIndices.map { "Var($it)" }, evaluable
        ).map { it.joinToString(separator = "*") }
        if (elements.isNotEmpty()) {
            return "$root*${elements.joinToString(separator = "*")}"
        } else return root
    }

}