package cz.muni.fi.ode.model

data class Step private constructor(
        val varIndex: Int,
        val theta: Double,
        val a: Double,
        val b: Double
) : Evaluable {

    /**
     * Secondary constructor that automatically sorts a/b to match supposed direction of step.
     */
    constructor(varIndex: Int, theta: Double, a: Double, b: Double, positive: Boolean):
    this(varIndex, theta,
            if (positive) Math.min(a,b) else Math.max(a,b),
            if (positive) Math.max(a,b) else Math.min(a,b)
    )

    override fun eval(value: Double): Double
            = if (value < theta) a else b

    override fun toString(): String
            = "H(${if (a <= b) '+' else '-'})($varIndex,$theta,$a,$b)"

}