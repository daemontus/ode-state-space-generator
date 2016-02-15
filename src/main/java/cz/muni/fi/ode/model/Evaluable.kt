package cz.muni.fi.ode.model

/**
 * Class implementing this interface can be evaluated in certain point
 */
interface Evaluable : (Double) -> Double {
    fun eval(value: Double): Double
    override fun invoke(p1: Double): Double = eval(p1)
}