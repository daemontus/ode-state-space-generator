package com.github.sybila.ode.model

/**
 * This file contains functions that are used to compute optimal approximation
 * of continuous functions in a model, thus transforming it into a multi-affine
 * model.
 */


/**
 * Perform linear approximation of continuous functions used in the model
 */
fun Model.computeApproximation(fast: Boolean = true, cutToRange: Boolean = false): Model {

    val newThresholds = variables.indices.map { vIndex ->

        val variable = variables[vIndex]

        val functions = variables
                .flatMap { it.equation }.flatMap { it.evaluable }
                .filter {
                    (it is Hill && it.varIndex == vIndex) || (it is Sigmoid && it.varIndex == vIndex)
                }

        if (functions.isEmpty()) variable.thresholds
        else {
            //compute new thresholds
            val (pointCount, segmentCount) = variable.varPoints ?:
                    throw IllegalStateException("Can't run abstraction for ${variable.name} without specified var points!")
            val thresholds = computeThresholds(pointCount, segmentCount, functions, fast)

            (thresholds + variable.thresholds).filter {
                !cutToRange || (it >= variable.range.first && it <= variable.range.second)
            }.toSet().sorted()
        }
    }

    val variables = variables.indices.map { vIndex ->

        val variable = variables[vIndex]

        //create a copy replacing functions with approximation
        variable.copy(
                thresholds = newThresholds[vIndex],
                equation = variable.equation.map {
                    it.copy(
                            evaluable = it.evaluable.map { f ->
                                when (f) {
                                    is Hill, is Sigmoid -> {
                                        RampApproximation(
                                                f.varIndex,
                                                newThresholds[f.varIndex].toDoubleArray(),
                                                newThresholds[f.varIndex].map { x -> f(x) }.toDoubleArray()
                                        )
                                    }
                                    else -> f
                                }
                            }
                    )
                }
            )
    }

    return this.copy( variables = variables )       //parameters do not change
}

//compute approximated thresholds for one variable
private fun computeThresholds(pointCount: Int, segmentCount: Int, functions: List<Evaluable>, fast: Boolean): DoubleArray {

    val xPoints = findEvaluationPoints(pointCount, functions)
    val curves = Array(functions.size) { f -> DoubleArray(pointCount) { functions[f](xPoints[it]) } }

    return if (fast) {
        fastLinearApproximation(xPoints, curves, segmentCount)
    } else {
        linearApproximation(xPoints, curves, segmentCount)
    }

}

//compute points at which the function should be evaluated
private fun findEvaluationPoints(pointCount: Int, functions: List<Evaluable>): DoubleArray {
    var max = 0.0
    @Suppress("LoopToCallChain")    // much faster this way
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

//performs approximation using accurate cost function
private fun linearApproximation(xPoints: DoubleArray, curves: Array<DoubleArray>, segmentCount: Int): DoubleArray {

    val hCost = Array(xPoints.size) { n -> DoubleArray(xPoints.size) { i ->
        if (i > n-2 || i == 0) {
            0.0 //no one will read this!
        } else {
            curves.maxByDoubleIndexed { ic -> segmentError(xPoints, curves[ic], i, n) }
        }
    }}

    return approximation(segmentCount, xPoints.size, xPoints, curves, hCost)
}

//performs approximation using simplified cost function
private fun fastLinearApproximation(xPoints: DoubleArray, curves: Array<DoubleArray>, segmentCount: Int): DoubleArray {

    val sy = Array(curves.size) { DoubleArray(xPoints.size) { 0.0 } }
    val sy2 = Array(curves.size) { DoubleArray(xPoints.size) { 0.0 } }
    val sxy = Array(curves.size) { DoubleArray(xPoints.size) { 0.0 } }

    val sx = DoubleArray(xPoints.size) { 0.0 }
    val sx2 = DoubleArray(xPoints.size) { 0.0 }

    for (ic in curves.indices) {
        sy2[ic][0] = curves[ic][0] * curves[ic][0]
        sy [ic][0] = curves[ic][0]
        sxy[ic][0] = curves[ic][0] * xPoints[0]

        for (ip in 1 until xPoints.size) {
            sy2[ic][ip] = sy2[ic][ip-1] + (curves[ic][ip] * curves[ic][ip])
            sy [ic][ip] = sy [ic][ip-1] + (curves[ic][ip])
            sxy[ic][ip] = sxy[ic][ip-1] + (curves[ic][ip] * xPoints[ip])
        }
    }

    sx2[0] = xPoints[0] * xPoints[0]
    sx[0] = xPoints[0]

    for (ip in 1 until xPoints.size) {
        sx2[ip] = sx2[ip-1] + (xPoints[ip] * xPoints[ip])
        sx [ip] = sx [ip-1]  + xPoints[ip]
    }

    val hCost = Array(xPoints.size) { n -> DoubleArray(xPoints.size) { i ->
        if (i > n-2 || i == 0) {
            0.0 //no one will read this!
        } else {
            curves.maxByDoubleIndexed { ic ->
                val a = (curves[ic][n] - curves[ic][0]) / (xPoints[n] - xPoints[0])
                val b = (curves[ic][0] * xPoints[n] - curves[ic][n] * xPoints[0]) / (xPoints[n] - xPoints[0])
                (sy2[ic][n] - sy2[ic][i-1]) - 2 * a * (sxy[ic][n] - sxy[ic][i-1]) - 2 * b * (sy[ic][n] - sy[ic][i-1]) + a * a * (sx2[n] - sx2[i-1]) + 2 * a * b * (sx[n] - sx[i-1]) + b * (n - i)
            }
        }
    }}

    return approximation(segmentCount, xPoints.size, xPoints, curves, hCost)
}

//performs approximation process using given cost function
private fun approximation(
        segmentCount: Int,
        pointCount: Int,
        points: DoubleArray,
        values: Array<DoubleArray>,
        costs: Array<DoubleArray>
): DoubleArray {

    val father = Array(segmentCount) { IntArray(pointCount) { 0 } }

    val cost = DoubleArray(pointCount - 1) { i ->
        values.maxByDouble { curve ->
            segmentError(points, curve, 0, i + 1)
        }
    }

    for (m in 1 until segmentCount) {

        for (n in (pointCount - 1).downTo(2)) {

            var minError = cost[n-2]
            var minIndex = n - 1

            for (i in m..(n-2)) {

                val currentError = cost[i-1] + costs[n][i]

                if (currentError < minError) {
                    minError = currentError
                    minIndex = i
                }
            }

            cost[n-1] = minError
            father[m][n] = minIndex

        }

    }


    val results = DoubleArray(segmentCount+1) { 0.0 }

    var pointIndex = pointCount - 1
    results[segmentCount] = points[pointIndex]

    for (i in (segmentCount-1).downTo(0)) {
        pointIndex = father[i][pointIndex]
        results[i] = points[pointIndex]
    }

    return results
}

private fun segmentError(x: DoubleArray, y: DoubleArray, first: Int, last: Int): Double {
    // Compute line segment coefficients
    val a = (y[last] - y[first]) / (x[last] - x[first])
    val b = (y[first] * x[last] - y[last] * x[first]) / (x[last] - x[first])

    // Compute error for the line segment
    var e = 0.0
    @Suppress("LoopToCallChain")    // much faster this way
    for (k in first..last) {
        e += (y[k] - a * x[k] - b) * (y[k] - a * x[k] - b)
    }
    e /= (a*a + 1)

    return e
}

//Utility functions used when computing the approximated model
private inline fun <T> Array<T>.maxByDouble(action: (T) -> Double): Double {
    var max = Double.NEGATIVE_INFINITY
    @Suppress("LoopToCallChain")    // much faster this way
    for (e in this) {
        max = Math.max(max, action(e))
    }
    return max
}

private inline fun <T> Array<T>.maxByDoubleIndexed(action: (Int) -> Double): Double {
    var max = Double.NEGATIVE_INFINITY
    @Suppress("LoopToCallChain")    // much faster this way
    for (e in this.indices) {
        max = Math.max(max, action(e))
    }
    return max
}
