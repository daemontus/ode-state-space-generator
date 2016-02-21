package cz.muni.fi.ode.generator

import cz.muni.fi.checker.Colors
import java.util.*

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

/**
 * Non empty rectangle represented by it's coordinates in the parameter space.
 * Coordinates should contain intervals for each variable, not direct coordinates of corner vertices.
 */
class Rectangle(
        val coordinates: DoubleArray
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
    operator fun minus(other: Rectangle): Set<Rectangle> {
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
                return setOf(this)
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
}


class RectangleColors(
        private val rectangles: Set<Rectangle> = setOf()
) : Colors<RectangleColors> {

    constructor(vararg values: Rectangle): this(values.toSet())

    override fun intersect(other: RectangleColors): RectangleColors {
        val newItems = ArrayList<Rectangle>()
        for (item1 in rectangles) {
            for (item2 in other.rectangles) {
                val r = item1 * item2
                if (r != null) newItems.add(r)
            }
        }
        return RectangleColors(newItems.toSet())
    }

    override fun isEmpty(): Boolean = rectangles.isEmpty()

    override fun minus(other: RectangleColors): RectangleColors {
        //go through all rectangles in other and subtract them one by one from all our rectangles
        return RectangleColors(
                other.rectangles.fold(rectangles.toList()) {
                    acc, rect -> acc.flatMap { it - rect }
                }.toSet()
        )
    }

    override fun plus(other: RectangleColors): RectangleColors {
        val newItems = ArrayList(rectangles)
        for (extra in other.rectangles) {
            var merged = false
            for (i in newItems.indices) {
                val r = extra + newItems[i]
                if (r != null) {
                    merged = true
                    newItems[i] = r
                    break   //we need to break, so that we don't do multiple merges
                }
            }
            if (!merged) newItems.add(extra)
        }
        //resulting array still might not be optimal
        var merged = true
        while (merged) {
            merged = false
            search@ for (c in newItems.indices) {
                for (i in newItems.indices) {
                    if (i == c) continue
                    val r = newItems[c] + newItems[i]
                    if (r != null) {
                        merged = true
                        newItems[c] = r
                        newItems.removeAt(i)
                        break@search
                    }
                }
            }
        }
        return RectangleColors(newItems.toSet())
    }

    override fun equals(other: Any?): Boolean = other is RectangleColors && other.rectangles == rectangles

    override fun hashCode(): Int = rectangles.hashCode()

    override fun toString(): String = rectangles.toString()

}