package cz.muni.fi.ode.model

data class Summand(
        val constant: Double,
        val paramIndex: Int,
        val variableIndices: List<Int>,
        val ramps: List<Ramp>,
        val steps: List<Step>,
        val hills: List<Hill>,
        val sigmoids: List<Sigmoid>
) {

    fun hasParam(): Boolean = paramIndex >= 0

    override fun toString(): String {
        val root = "$constant*Param($paramIndex)}"
        val elements = listOf(
                variableIndices.map { "Var($it)" }, ramps, steps, hills, sigmoids
        ).map { it.joinToString(separator = "*") }
        if (elements.isNotEmpty()) {
            return "$root*${elements.joinToString(separator = "*")}"
        } else return root
    }

}