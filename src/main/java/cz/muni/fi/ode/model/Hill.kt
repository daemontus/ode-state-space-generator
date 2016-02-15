package cz.muni.fi.ode.model

data class Hill(
        val varIndex: Int,
        val theta: Double,
        val n: Double,
        val a: Double,
        val b: Double
) : Evaluable {

    constructor(varIndex: Int, theta: Double, n: Double, a: Double, b: Double, positive: Boolean):
            this(varIndex, theta, n,
                    if (positive) Math.min(a,b) else Math.max(a,b),
                    if (positive) Math.max(a,b) else Math.min(a,b)
            )

    override fun eval(value: Double): Double
        = a + (b - a) * (Math.pow(theta/value, n))

    override fun toString(): String
        = "Hill(${if(a <= b) '+' else '-'})($varIndex, $theta, $n, $a, $b)"

}