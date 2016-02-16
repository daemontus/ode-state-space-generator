package cz.muni.fi.ode.model

/**
 * Sigmoid function: https://en.wikipedia.org/wiki/Sigmoid_function
 *
 * Original hill function has image <0,1)
 * Values a and b serve as scaling factor so that the image is stretched to <a, b).
 * In case the function is negative, a and b are switched, and then the
 * image of the function is <b, a) and whole function is decreasing.
 *
 */
data class Sigmoid private constructor(
        val varIndex: Int,
        val k: Double,
        val theta: Double,
        val a: Double,
        val b: Double
) : Evaluable {

    companion object {

        fun positive(varIndex: Int, k: Double, theta: Double, a: Double, b: Double)
            = Sigmoid(varIndex, k, theta, Math.min(a,b), Math.max(a,b))

        fun negative(varIndex: Int, k: Double, theta: Double, a: Double, b: Double)
                = Sigmoid(varIndex, k, theta, Math.max(a,b), Math.min(a,b))

        fun positiveInverse(varIndex: Int, k: Double, theta: Double, a: Double, b: Double): Sigmoid {
            val a1 = Math.min(a,b)
            val b1 = Math.max(a,b)
            return Sigmoid(varIndex, k, theta + Math.log(a1/b1) / (2*k), 1/a1, 1/b1)
        }

        fun negativeInverse(varIndex: Int, k: Double, theta: Double, a: Double, b: Double): Sigmoid {
            val a1 = Math.min(a,b)
            val b1 = Math.max(a,b)
            return Sigmoid(varIndex, k, theta + Math.log(a1/b1) / (2*k), 1/b1, 1/a1)
        }

    }

    override fun eval(value: Double): Double
        = a + (b - a) * ((1 + Math.tanh((k*(value - theta)))*0.5))

    override fun toString(): String
        = "S(${if (a <= b) '+' else '-'})($varIndex, $k, $theta, $a, $b)"

}