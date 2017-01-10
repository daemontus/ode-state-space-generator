package com.github.sybila.ode.generator.shared.rect

import com.github.sybila.checker.shared.*
import com.github.sybila.checker.shared.solver.solverCalled
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RectParams(
        val rectangles: List<Rectangle>
) : Params {
    override fun toString(): String = rectangles.toString()
}

fun List<Rectangle>.asParams(): RectParams = RectParams(this)

class RectangleSolver(
        val bounds: Rectangle
) : Solver {

    private val max = AtomicInteger(0)

    override fun Params.extendWith(other: Params): Params? {
        solverCalled()
        val current = this.toRectangles()
        val new = (current + other.toRectangles()).minimize()
        return if (new != current) new.asParams() else null
    }

    override fun Params.isSat(): Params? {
        solverCalled()
        val rectangles = this.toRectangles().minimize()
        if (rectangles.size > max.get()) {
            max.set(rectangles.size)
            println("Max: ${max.get()}")
        }
        //println("isSat $this to $rectangles")
        return if (rectangles.isNotEmpty()) rectangles.asParams() else null
    }

    private fun Params.toRectangles(): List<Rectangle> {
        return when (this) {
            is TT -> listOf(bounds)
            is FF -> listOf()
            is RectParams -> rectangles
            is Or -> args.flatMap { it.toRectangles() }
            is And -> args.fold(listOf<Rectangle>(bounds)) { current, params ->
                val new = params.toRectangles()
                val result = ArrayList<Rectangle>()
                for (item1 in current) {
                    for (item2 in new) {
                        item1.intersect(item2)?.let { r ->
                            result.add(r)
                        }
                    }
                }
                result
            }
            is Not -> And(inner.toRectangles()
                    .map { RectParams(bounds - it) }).toRectangles()
            else -> throw UnsupportedParameterValue(this)
        }
    }

    private fun Params.purge(): Params {
        return when (this) {
            is Or -> 
        }
    }

    private fun List<Rectangle>.minimize(): List<Rectangle> {
        if (this.size < 2) return this
        val result = this.toMutableSet()
        do {
            var merged = false
            search@ for (c in result) {
                for (i in result) {
                    if (i == c) continue

                    val union = i + c
                    if (union != null) {
                        result.remove(i)
                        result.remove(c)
                        result.add(union)
                        merged = true
                        break@search
                    }
                }
            }
            if (this.size < 2) break
        } while (merged)
        return result.toList()
    }
}