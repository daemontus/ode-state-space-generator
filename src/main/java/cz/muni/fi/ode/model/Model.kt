package cz.muni.fi.ode.model

import java.util.*

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

    private fun segmentError(x: DoubleArray, y: DoubleArray, first: Int, last: Int): Double {
        // Compute line segment coefficients
        val a = (y[last] - y[first]) / (x[last] - x[first]);
        val b = (y[first] * x[last] - y[last] * x[first]) / (x[last] - x[first]);

        // Compute error for the line segment
        var e = 0.0
        for (k in first..last) {
            e += (y[k] - a * x[k] - b) * (y[k] - a * x[k] - b)
        }
        e /= (a*a + 1)

        return e
    }

    private fun linearApproximation(xPoints: DoubleArray, curves: List<DoubleArray>, segmentCount: Int): DoubleArray {

        val mCost = Array(xPoints.size) { i -> DoubleArray(segmentCount) { Double.POSITIVE_INFINITY } }
        val hCost = Array(xPoints.size) { i -> DoubleArray(xPoints.size) { Double.POSITIVE_INFINITY } }

        mCost[1][0] = 0.0;

        val father = Array(xPoints.size) { IntArray(segmentCount) { 0 } }

        for (n in 2..xPoints.size) {

            var temp = Double.NEGATIVE_INFINITY

            for (curve in curves) {
                val err = segmentError(xPoints, curve, 0, n-1)
                temp = Math.max(err, temp)
            }

            mCost[n-1][0] = temp;
        }

        var minError: Double
        var minIndex: Int

        for (m in 1 until segmentCount) {

            for (n in 2 until xPoints.size) {

                minError = mCost[n-1][m-1];
                minIndex = n - 1;

                for (i in m..(n-2)) {

                    if (hCost[i][n] == Double.POSITIVE_INFINITY) {
                        var temp = Double.NEGATIVE_INFINITY

                        for (curve in curves) {
                            val err = segmentError(xPoints, curve, i, n)
                            temp = Math.max(err, temp)
                        }

                        hCost[i][n] = temp;
                    }

                    val currErr = mCost[i][m-1] + hCost[i][n];

                    if (currErr < minError) {
                        minError = currErr;
                        minIndex = i;
                    }
                }

                mCost[n][m]  = minError
                father[n][m] = minIndex
            }
        }

        val ib = IntArray(segmentCount+1) { 0 }
        val xb = DoubleArray(segmentCount+1) { 0.0 }

        ib[segmentCount] = xPoints.size-1;
        xb[segmentCount] = xPoints[ib[segmentCount]];

        for (i in (segmentCount-1).downTo(0)) {
            ib[i] = father[ib[i+1]][i];
            xb[i] = xPoints[ib[i]];
        }

        return xb
    }

    private inline fun <T> Iterable<T>.maxByDouble(action: (T) -> Double): Double {
        var max = Double.NEGATIVE_INFINITY
        for (e in this) {
            max = Math.max(max, action(e))
        }
        return max
    }

    private fun fastLinearApproximation(xPoints: DoubleArray, curves: List<DoubleArray>, segmentCount: Int): DoubleArray {

        val hCost = Array(xPoints.size) { i -> DoubleArray(xPoints.size) { Double.POSITIVE_INFINITY } }

        val father = Array(xPoints.size) { IntArray(segmentCount) { 0 } }

        val cost = DoubleArray(xPoints.size) { i ->
            curves.maxByDouble { curve ->
                segmentError(xPoints, curve, 0, i)
            }
        }
        cost[0] = 0.0

        val sy = Array(curves.size) { DoubleArray(xPoints.size) { 0.0 } }
        val sy2 = Array(curves.size) { DoubleArray(xPoints.size) { 0.0 } }
        val sxy = Array(curves.size) { DoubleArray(xPoints.size) { 0.0 } }

        val sx = DoubleArray(xPoints.size) { 0.0 }
        val sx2 = DoubleArray(xPoints.size) { 0.0 }

        for (ic in curves.indices) {
            sy2[ic][0] = curves[ic][0] * curves[ic][0];
            sy [ic][0] = curves[ic][0];
            sxy[ic][0] = curves[ic][0] * xPoints[0];

            for (ip in 1 until xPoints.size) {
                sy2[ic][ip] = sy2[ic][ip-1] + (curves[ic][ip] * curves[ic][ip]);
                sy [ic][ip] = sy [ic][ip-1] + (curves[ic][ip]);
                sxy[ic][ip] = sxy[ic][ip-1] + (curves[ic][ip] * xPoints[ip]);
            }
        }

        sx2[0] = xPoints[0] * xPoints[0];
        sx[0] = xPoints[0];

        for (ip in 1 until xPoints.size) {
            sx2[ip] = sx2[ip-1] + (xPoints[ip] * xPoints[ip]);
            sx [ip] = sx [ip-1]  + xPoints[ip];
        }

        var minError: Double
        var minIndex: Int

        val newCost = DoubleArray(xPoints.size) { 0.0 }

        for (m in 1 until segmentCount) {

            for (n in 2 until xPoints.size) {

                minError = cost[n-1]
                minIndex = n - 1

                for (i in m..(n-2)) {
                    if (hCost[i][n] == Double.POSITIVE_INFINITY) {

                        var temp = Double.NEGATIVE_INFINITY

                        for (ic in curves.indices) {
                            val a = (curves[ic][n] - curves[ic][0]) / (xPoints[n] - xPoints[0]);
                            val b = (curves[ic][0] * xPoints[n] - curves[ic][n] * xPoints[0]) / (xPoints[n] - xPoints[0]);
                            val seg_err = (sy2[ic][n] - sy2[ic][i-1]) - 2 * a * (sxy[ic][n] - sxy[ic][i-1]) - 2 * b * (sy[ic][n] - sy[ic][i-1]) + a * a * (sx2[n] - sx2[i-1]) + 2 * a * b * (sx[n] - sx[i-1]) + b * (n - i);

                            temp = Math.max(seg_err, temp);
                        }

                        hCost[i][n] = temp;
                    }

                    val currentError = cost[i] + hCost[i][n];

                    if (currentError < minError) {
                        minError = currentError;
                        minIndex = i;
                    }
                }

                newCost[n] = minError;
                father[n][m] = minIndex;

            }

            System.arraycopy(newCost, 0, cost, 0, cost.size)
        }

        val ib = IntArray(segmentCount+1) { 0 }
        val xb = DoubleArray(segmentCount+1) { 0.0 }

        ib[segmentCount] = xPoints.size-1;
        xb[segmentCount] = xPoints[ib[segmentCount]];

        for (i in (segmentCount-1).downTo(0)) {
            ib[i] = father[ib[i+1]][i];
            xb[i] = xPoints[ib[i]];
        }

        return xb
    }

    //compute points at which the function should be evaluated
    private fun findEvaluationPoints(pointCount: Int, functions: List<Evaluable>): DoubleArray {
        var max = 0.0
        for (e in functions) {  //find last evaluation point
            val newMax = if (e is Hill) {
                2.0 * e.theta + (5.0 / e.n) * e.theta
            } else if (e is Sigmoid) {
                e.theta + (2.0 / e.k) * 1.5
            } else throw IllegalStateException("Unsupported function $e")
            max = Math.max(newMax, max)
        }
        val dx = max / (pointCount-1)
        return DoubleArray(pointCount) { i -> dx * i }
    }

    private fun computeThresholds(pointCount: Int, segmentCount: Int, functions: List<Evaluable>, fast: Boolean): DoubleArray {

        val xPoints = findEvaluationPoints(pointCount, functions)
        val curves = functions.map { f -> DoubleArray(pointCount) { f(xPoints[it]) } }

        return if (fast) {
            fastLinearApproximation(xPoints, curves, segmentCount)
        } else {
            linearApproximation(xPoints, curves, segmentCount)
        }

    }

    fun computeApproximation(fast: Boolean = true, cutToRange: Boolean = false): Model {

        val variables = variables.indices.map { vIndex ->

            val variable = variables[vIndex]

            //find all functions that need to by approximated
            val functions = variables
                    .flatMap { it.equation }.flatMap { it.evaluable }
                    .filter {
                        (it is Hill && it.varIndex == vIndex) || (it is Sigmoid && it.varIndex == vIndex)
                    }

            if (functions.isEmpty()) variable  //this variable is OK!
            else {

                //compute new thresholds
                val (pointCount, segmentCount) = variable.varPoints ?:
                        throw IllegalStateException("Can't run abstraction for ${variable.name} without specified var points!")
                val newThresholds = computeThresholds(pointCount, segmentCount, functions, fast)

                //create a copy replacing functions with approximation
                variable.copy(
                        thresholds = (newThresholds + variable.thresholds).filter {
                            !cutToRange || (it >= variable.range.first && it <= variable.range.second)
                        }.toSet().sorted(),
                        equation = variable.equation.map {
                            it.copy(
                                    evaluable = it.evaluable.map { f ->
                                        when (f) {
                                            is Hill, is Sigmoid -> {
                                                RampApproximation(
                                                        f.varIndex,
                                                        newThresholds,
                                                        newThresholds.map { x -> f(x) }.toDoubleArray()
                                                )
                                            }
                                            else -> f
                                        }
                                    }
                            )
                        }
                )
            }
        }

        return this.copy( variables = variables )       //parameters do not change
    }
}