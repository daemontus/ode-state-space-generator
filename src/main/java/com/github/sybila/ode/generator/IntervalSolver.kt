package com.github.sybila.ode.generator

/**
 * Simple interface implemented by all parameter implementations which can be exported to
 * interval sets.
 */
interface IntervalSolver<T: Any> {

    /**
     * The exported structure is as follows:
     * Array of rectangles, each rectangle being an array of intervals, each interval being a
     * two-element array of doubles.
     *
     * (Yes, this is ugly, but very easy to serialize using json.)
     */
    fun T.asIntervals(): Array<Array<DoubleArray>>

}