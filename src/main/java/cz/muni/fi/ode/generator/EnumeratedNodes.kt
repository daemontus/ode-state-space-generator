package cz.muni.fi.ode.generator

import com.github.sybila.checker.IDNode
import com.github.sybila.checker.MutableMapNodes
import com.github.sybila.checker.MutableNodes
import com.github.sybila.checker.Nodes
import cz.muni.fi.ode.model.Model
import java.util.*

/**
 * Symbolic representation of node set that shares one color. Used to speed up proposition resolution.
 *
 * Not done.
 */
class EnumeratedNodes(
    private val dimension: Int,
    private val upperBound: Int,
    private val lowerBound: Int,
    private val encoding: NodeEncoder,
    private val colors: RectangleColors
) : Nodes<IDNode, RectangleColors> {

    override val emptyColors: RectangleColors = RectangleColors()

    override val entries: Iterable<Map.Entry<IDNode, RectangleColors>> = object : Iterable<Map.Entry<IDNode, RectangleColors>> {

        override fun iterator(): Iterator<Map.Entry<IDNode, RectangleColors>> = object : Iterator<Map.Entry<IDNode, RectangleColors>> {

            override fun hasNext(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun next(): Map.Entry<IDNode, RectangleColors> {
                throw UnsupportedOperationException()
            }

        }

    }

    override fun contains(key: IDNode): Boolean {
        val coordinate = encoding.coordinate(key, dimension)
        return coordinate >= lowerBound && coordinate <= upperBound
    }

    override fun get(key: IDNode): RectangleColors = colors

    override fun intersect(other: Nodes<IDNode, RectangleColors>): Nodes<IDNode, RectangleColors> {
        throw UnsupportedOperationException()
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun minus(other: Nodes<IDNode, RectangleColors>): Nodes<IDNode, RectangleColors> {
        throw UnsupportedOperationException()
    }

    override fun plus(other: Nodes<IDNode, RectangleColors>): Nodes<IDNode, RectangleColors> {
        throw UnsupportedOperationException()
    }

    override fun toMutableMap(): MutableMap<IDNode, RectangleColors>
            = entries.associateTo(HashMap()) { Pair(it.key, it.value) }

    override fun toMutableNodes(): MutableNodes<IDNode, RectangleColors>
            = MutableMapNodes(emptyColors, entries.associateTo(HashMap()) { Pair(it.key, it.value) })

}