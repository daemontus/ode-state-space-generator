package com.github.sybila.ode.generator.preprocess

import java.nio.ByteBuffer
import java.util.*

/**
 * Shortcut for creating rectangles
 */
fun rectangleOf(vararg values: Int): Rectangle = Rectangle(values)

/**
 * Create rectangle defined by two n-dimensional points, not variable intervals
 */
fun rectangleFromPoints(vararg values: Int): Rectangle {
    val dimensions = values.size/2
    val newCoordinates = IntArray(values.size) { i ->
        if (i%2 == 0) {
            values[i/2]
        } else {
            values[dimensions+i/2]
        }
    }
    return Rectangle(newCoordinates)
}

fun rectangleFromBuffer(buffer: ByteBuffer): Rectangle {
    val size = buffer.int
    val coordinates = IntArray(size)
    for (i in 0 until size) {
        coordinates[i] = buffer.int
    }
    return Rectangle(coordinates)
}

internal fun max(l: Int, r: Int): Int = if (l > r) l else r
internal fun min(l: Int, r: Int): Int = if (l < r) l else r

/**
 * Non empty rectangle represented by it's coordinates in the parameter space.
 * Coordinates should contain intervals for each variable, not direct coordinates of corner vertices.
 */
class Rectangle(
        private val coordinates: IntArray
) {

    /**
     * Intersect these two rectangles.
     * If result is empty, return null.
     */
    operator fun times(other: Rectangle): Rectangle? {
        val newCoordinates = IntArray(coordinates.size) { i ->
            if (i % 2 == 0) {
                max(coordinates[i], other.coordinates[i])  //new lower bound
            } else {
                min(coordinates[i], other.coordinates[i])  //new higher bound
            }
        }
        //check integrity
        for (dim in 0 until (newCoordinates.size/2)) {
            if (newCoordinates[2*dim] >= newCoordinates[2*dim+1]) return null
        }
        return Rectangle(newCoordinates)
    }

    fun intersect(other: Rectangle, into: IntArray): Rectangle? {
        for (i in 0 until (this.coordinates.size / 2)) {
            val iL = 2*i
            val iH = 2*i+1
            val low = Math.max(coordinates[iL], other.coordinates[iL])
            val high = Math.min(coordinates[iH], other.coordinates[iH])
            if (low >= high) return null
            else {
                into[iL] = low
                into[iH] = high
            }
        }
        return Rectangle(into)
    }

    fun newArray(): IntArray = IntArray(coordinates.size)

    private fun encloses(other: Rectangle): Boolean {
        for (i in coordinates.indices) {
            if (i % 2 == 0 && coordinates[i] > other.coordinates[i]) return false
            if (i % 2 == 1 && coordinates[i] < other.coordinates[i]) return false
        }
        return true
    }

    /**
     * If possible, merge these two rectangles. If not possible, return null.
     */
    operator fun plus(other: Rectangle): Rectangle? {
        if (this.encloses(other)) return this
        if (other.encloses(this)) return other
        var mergeDimension = -1
        var mergeLow = Int.MIN_VALUE
        var mergeHigh = Int.MAX_VALUE
        for (dim in 0 until (coordinates.size/2)) {
            val l1 = coordinates[2*dim]
            val l2 = other.coordinates[2*dim]
            val h1 = coordinates[2*dim+1]
            val h2 = other.coordinates[2*dim+1]
            if (l1 == l2 && h1 == h2) {
                //this dimension won't change
                continue
            } else if (h2 < l1 || h1 < l2) {
                // l1..h1 ... l2..h2 || l2..h2 ... l1..h1 - we can't merge them, they are completely separate
                return null
            } else {
                //we have a possible merge dimension
                if (mergeDimension != -1) {
                    //more than one merge dimension, abort
                    return null
                } else {
                    mergeDimension = dim
                    mergeLow = Math.min(l1, l2)
                    mergeHigh = Math.max(h1, h2)
                }
            }
        }
        //if rectangles are equal, they are processed in encloses section - if we reach this point, merge must be valid
        val newCoordinates = coordinates.copyOf()
        newCoordinates[2*mergeDimension] = mergeLow
        newCoordinates[2*mergeDimension+1] = mergeHigh
        return Rectangle(newCoordinates)
    }

    /**
     * Create a set of smaller rectangles that together form a result of subtraction of given rectangle.
     */
    operator fun minus(other: Rectangle): MutableSet<Rectangle> {
        val workingCoordinates = coordinates.copyOf()
        val results = HashSet<Rectangle>()
        for (dim in 0 until (coordinates.size/2)) {
            val l1 = coordinates[2*dim]
            val l2 = other.coordinates[2*dim]
            val h1 = coordinates[2*dim+1]
            val h2 = other.coordinates[2*dim+1]
            if (l1 >= l2 && h1 <= h2) {
                //this dimension has a clean cut, no rectangles are created
                continue
            } else if (h2 <= l1 || h1 <= l2) {
                // l1..h1 ... l2..h2 || l2..h2 ... l1..h1 - these rectangles are completely separate, nothing should be cut
                return mutableSetOf(this)
            } else {
                if (l1 < l2) {
                    //there is an overlap on the lower side, create cut-rectangle and subtract it from working coordinates
                    val newCoordinates = workingCoordinates.copyOf()
                    newCoordinates[2*dim] = l1
                    newCoordinates[2*dim+1] = l2
                    results.add(Rectangle(newCoordinates))
                    workingCoordinates[2*dim] = l2
                }
                if (h1 > h2) {
                    //there is an overlap on the upper side, create cut-rectangle and subtract it from working coordinates
                    val newCoordinates = workingCoordinates.copyOf()
                    newCoordinates[2*dim] = h2
                    newCoordinates[2*dim+1] = h1
                    results.add(Rectangle(newCoordinates))
                    workingCoordinates[2*dim+1] = h2
                }
            }
        }
        return results
    }

    override fun equals(other: Any?): Boolean = other is Rectangle && Arrays.equals(coordinates, other.coordinates)

    override fun hashCode(): Int = Arrays.hashCode(coordinates)

    override fun toString(): String = Arrays.toString(coordinates)

    fun byteSize(): Int = 4 + Integer.BYTES * coordinates.size

    fun writeToBuffer(buffer: ByteBuffer) {
        buffer.putInt(coordinates.size)
        coordinates.forEach { buffer.putInt(it) }
    }

    fun asParams(): MutableSet<Rectangle> = mutableSetOf(this)

    fun asIntervals(coder: ParamCoder): List<List<Double>> {
        return (0 until (coordinates.size / 2)).map {
            listOf(
                    coder.indexToValue(coordinates[2 * it], it),
                    coder.indexToValue(coordinates[2* it +1], it)
            )
        }
    }
}