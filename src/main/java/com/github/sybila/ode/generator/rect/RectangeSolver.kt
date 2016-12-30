package com.github.sybila.ode.generator.rect

import com.github.sybila.checker.Solver
import java.nio.ByteBuffer
import java.util.*

class RectangleSolver(
        private val bounds: Rectangle
) : Solver<MutableSet<Rectangle>> {

    override val ff: MutableSet<Rectangle> = mutableSetOf()
    override val tt: MutableSet<Rectangle> = mutableSetOf(bounds)

    override fun MutableSet<Rectangle>.and(other: MutableSet<Rectangle>): MutableSet<Rectangle> {
        val newItems = HashSet<Rectangle>()
        for (item1 in this) {
            for (item2 in other) {
                val r = item1 * item2
                if (r != null) newItems.add(r)
            }
        }
        return newItems
    }

    override fun MutableSet<Rectangle>.isSat(): Boolean = this.isNotEmpty()

    override fun MutableSet<Rectangle>.not(): MutableSet<Rectangle> {
        //!(a | b) <=> (!a & !b) <=> ((a1 | a2) && (b1 | b2))
        return this.map { bounds - it }.fold(tt) { a, i -> a and i }.toMutableSet()
    }

    override fun MutableSet<Rectangle>.or(other: MutableSet<Rectangle>): MutableSet<Rectangle> {
        val result = HashSet<Rectangle>()
        result.addAll(this)
        result.addAll(other)
        return result
    }

    override fun MutableSet<Rectangle>.minimize() {
        do {
            var merged = false
            search@ for (c in this) {
                for (i in this) {
                    if (i == c) continue

                    val union = i + c
                    if (union != null) {
                        this.remove(i)
                        this.remove(c)
                        this.add(union)
                        merged = true
                        break@search
                    }
                }
            }
        } while (merged)
    }

    override fun MutableSet<Rectangle>.prettyPrint(): String = toString()

    override fun ByteBuffer.putColors(colors: MutableSet<Rectangle>): ByteBuffer {
        this.putInt(colors.size)
        colors.forEach { it.writeToBuffer(this) }
        return this
    }

    override fun ByteBuffer.getColors(): MutableSet<Rectangle> {
        return (0 until this.int).map { rectangleFromBuffer(this) }.toMutableSet()
    }

    override fun MutableSet<Rectangle>.byteSize(): Int {
        //assumption: All rectangles have the same size
        val rectangleSize = this.firstOrNull()?.byteSize() ?: 0
        return 4 + rectangleSize * this.size
    }

    override fun MutableSet<Rectangle>.transferTo(solver: Solver<MutableSet<Rectangle>>): MutableSet<Rectangle> {
        //rectangle is immutable, all we need to copy is the set
        return HashSet(this)
    }




}