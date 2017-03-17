package com.github.sybila.ode.generator.rect

import com.github.sybila.checker.MutableStateMap
import com.github.sybila.checker.Solver
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RectangleSolver(
        private val bounds: Rectangle
) : Solver<MutableSet<Rectangle>> {

    override val ff: MutableSet<Rectangle> = mutableSetOf()
    override val tt: MutableSet<Rectangle> = mutableSetOf(bounds)

    private val cache = ThreadLocal<DoubleArray?>()

    private var coreSize = AtomicInteger(2)

    override fun MutableSet<Rectangle>.and(other: MutableSet<Rectangle>): MutableSet<Rectangle> {
        return if (this.isEmpty()) this
        else if (other.isEmpty()) other
        else if (this == tt) other
        else if (other == tt) this
        else {
            val newItems = HashSet<Rectangle>()
            for (item1 in this) {
                for (item2 in other) {
                    var c = cache.get()
                    if (c == null) c = item1.newArray()
                    val r = item1.intersect(item2, c)
                    //val r = item1 * item2
                    if (r != null) {
                        cache.set(null)
                        newItems.add(r)
                    } else {
                        cache.set(c)
                    }
                }
            }
            newItems
        }
    }

    override fun MutableSet<Rectangle>.isSat(): Boolean {
        //SolverStats.solverCall()
        return this.isNotEmpty()
    }

    override fun MutableSet<Rectangle>.not(): MutableSet<Rectangle> {
        return if (this.isEmpty()) {
            tt
        } else if (this == tt) {
            ff
        } else {
            //!(a | b) <=> (!a & !b) <=> ((a1 | a2) && (b1 | b2))
            this.map { bounds - it }.fold(tt) { a, i -> a and i }.toMutableSet()
        }
    }

    override fun MutableSet<Rectangle>.andNot(other: MutableSet<Rectangle>): Boolean {
        //(a | b) && !(c | d) = (a | b) && (c1 | c2) && (d1 | d2)
        return if (this == ff) false
        else if (other == tt) false
        else {
            other.asSequence().fold(this.asSequence()) { acc, rect ->
                acc.flatMap { (it - rect).asSequence() }
            }.any()
        }
    }

    override fun MutableSet<Rectangle>.or(other: MutableSet<Rectangle>): MutableSet<Rectangle> {
        return if (this.isEmpty()) other
        else if (other.isEmpty()) this
        else if (this == tt || other == tt) tt
        else {
            val result = HashSet<Rectangle>()
            result.addAll(this)
            val unmergable = HashSet<Rectangle>(other)
            for (toMerge in other) {
                for (candidate in result) {
                    val union = toMerge + candidate
                    if (union != null) {
                        result.remove(candidate)
                        unmergable.remove(toMerge)
                        result.add(union)
                        break
                    }
                }
            }
            result.addAll(unmergable)
            return if (result != this) result or result
            else result
        }
    }

    override fun MutableSet<Rectangle>.minimize() {
        if (4 * this.size < coreSize.get()) return
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
            //if (this.size < 2) return
        } while (merged)
        coreSize.set(this.size)
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


    override fun MutableStateMap<MutableSet<Rectangle>>.setOrUnion(state: Int, value: MutableSet<Rectangle>): Boolean {
        return if (value.isNotSat()) false
        else if (state !in this && value.isSat()) {
            this[state] = value
            true
        } else {
            val current = this[state]
            if (value andNot current) {
                this[state] = value or current
                true
            } else false
        }
    }
}