package cz.muni.fi.ode.model

data class Model(
        val variables: List<Variable>,
        val parameters: List<Parameter>
) {

    data class Variable(
            val name: String,
            val range: Pair<Double, Double>,
            val thresholds: List<Double>,
            val varPoints: Pair<Int, Int>?,
            val equation: List<Summand>
    )

    data class Parameter(
            val name: String,
            val range: Pair<Double, Double>
    )

}