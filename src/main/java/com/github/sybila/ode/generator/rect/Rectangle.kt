package com.github.sybila.ode.generator.rect

import com.github.sybila.ode.generator.det.RectangleSet
import java.nio.ByteBuffer
import java.util.*

private val PRECISION = 0.00001

/**
 * Shortcut for creating rectangles
 */
fun rectangleOf(vararg values: Double): Rectangle = Rectangle(values)

/**
 * Create rectangle defined by two n-dimensional points, not variable intervals
 */
fun rectangleFromPoints(vararg values: Double): Rectangle {
    val dimensions = values.size/2
    val newCoordinates = DoubleArray(values.size) { i ->
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
    val coordinates = DoubleArray(size)
    for (i in 0 until size) {
        coordinates[i] = buffer.double
    }
    return Rectangle(coordinates)
}

/**
 * Non empty rectangle represented by it's coordinates in the parameter space.
 * Coordinates should contain intervals for each variable, not direct coordinates of corner vertices.
 */
class Rectangle(
        private val coordinates: DoubleArray
) {

    /**
     * Intersect these two rectangles.
     * If result is empty, return null.
     */
    operator fun times(other: Rectangle): Rectangle? {
        val newCoordinates = DoubleArray(coordinates.size) { i ->
            if (i % 2 == 0) {
                Math.max(coordinates[i], other.coordinates[i])  //new lower bound
            } else {
                Math.min(coordinates[i], other.coordinates[i])  //new higher bound
            }
        }
        //check integrity
        for (dim in 0 until (newCoordinates.size/2)) {
            if (newCoordinates[2*dim] >= newCoordinates[2*dim+1]) return null
        }
        return Rectangle(newCoordinates)
    }

    fun intersect(other: Rectangle, into: DoubleArray): Rectangle? {
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

    fun newArray(): DoubleArray = DoubleArray(coordinates.size)

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
        var mergeLow = Double.NEGATIVE_INFINITY
        var mergeHigh = Double.POSITIVE_INFINITY
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

    fun byteSize(): Int = 4 + 8 * coordinates.size

    fun writeToBuffer(buffer: ByteBuffer) {
        buffer.putInt(coordinates.size)
        coordinates.forEach { buffer.putDouble(it) }
    }

    fun asParams(): MutableSet<Rectangle> = mutableSetOf(this)

    fun asIntervals(): List<List<Double>> {
        return (0 until (coordinates.size / 2)).map {
            listOf(coordinates[2* it], coordinates[2* it +1])
        }
    }

    fun toRectangleSet(): RectangleSet {
        if (this.coordinates.size != 4) throw IllegalStateException("Wrong rectangle dimension")
        return RectangleSet(
            thresholdsX = doubleArrayOf(coordinates[0], coordinates[1]),
            thresholdsY = doubleArrayOf(coordinates[2], coordinates[3]),
            values = BitSet().apply { set(0) }
        )
    }
}