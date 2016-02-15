package cz.muni.fi.ode.model


data class Sigmoid(
        val varIndex: Int,
        val k: Double,
        val theta: Double,
        val a: Double,
        val b: Double
) : Evaluable {

    constructor(varIndex: Int, k: Double, theta: Double, a: Double, b: Double, positive: Boolean):
    this(varIndex, k, theta,
            if (positive) Math.min(a,b) else Math.max(a,b),
            if (positive) Math.max(a,b) else Math.min(a,b)
    )

    override fun eval(value: Double): Double
        = a + (b - a) * ((1 + Math.tanh((k*(value - theta)))*0.5))

    override fun toString(): String
        = "S(${if (a <= b) '+' else '-'})($varIndex, $k, $theta, $a, $b)"

}