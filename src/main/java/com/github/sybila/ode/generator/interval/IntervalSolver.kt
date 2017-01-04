package com.github.sybila.ode.generator.interval

import com.github.sybila.checker.Solver
import java.nio.ByteBuffer
import java.util.*

class IntervalSolver(
        private val lowerBound: Double,
        private val upperBound: Double
) : Solver<DoubleArray> {

    private var lastProgressPrint = 0L
    private var solverCalls = 0L

    override val ff: DoubleArray = DoubleArray(0)
    override val tt: DoubleArray = doubleArrayOf(lowerBound, upperBound)

    override fun DoubleArray.and(other: DoubleArray): DoubleArray {
        return if (this.isEmpty()) this
        else if (other.isEmpty()) other
        else {
            (0 until this.size/2).asSequence().flatMap { left ->
                (0 until other.size/2).asSequence().map { right ->
                    val newLow = Math.max(this[2*left], other[2*right])
                    val newHigh = Math.min(this[2*left+1], other[2*right+1])
                    if (newLow >= newHigh) null else newLow to newHigh
                }
            }.filterNotNull().sortedBy { it.first }.flatMap { sequenceOf(it.first, it.second) }
                    .toList().toDoubleArray()
        }
    }

    override fun DoubleArray.isSat(): Boolean {
        solverCalls += 1
        if (System.currentTimeMillis() > lastProgressPrint + 2000) {
            System.err.println("Processing: ${solverCalls / 2.0} per second")
            solverCalls = 0
            lastProgressPrint = System.currentTimeMillis()
        }
        return this.isNotEmpty()
    }

    override fun DoubleArray.canSat(): Boolean = this.isNotEmpty()
    override fun DoubleArray.canNotSat(): Boolean = this.isEmpty()

    override fun DoubleArray.andNot(other: DoubleArray): Boolean
            = ((this.not() or other).not()).isSat()

    override fun DoubleArray.minimize() {
        // do nothing
    }

    override fun DoubleArray.not(): DoubleArray {
        return if (this.isEmpty()) tt
        else {
            val result = ArrayList<Double>()
            if (this[0] > lowerBound) {
                result.add(lowerBound)
                result.add(this[0])
            }
            var i = 0
            while (2*(i+1) < this.size) {
                result.add(this[2*i+1])
                result.add(this[2*(i+1)])
                i += 1
            }
            if (this[lastIndex] < upperBound) {
                result.add(this[lastIndex])
                result.add(upperBound)
            }
            result.toDoubleArray()
        }
    }

    override fun DoubleArray.or(other: DoubleArray): DoubleArray {
        return if (this.isEmpty()) {
            other
        } else if (other.isEmpty()) {
            this
        } else {
            val result = ArrayList<Double>()
            var left = this
            var right = other
            var iL = 0
            var iR = 0
            var added = -1
            while (2*iL < left.size && iR < right.size) {
                //println("$iL $iR")
                if (left[2*iL] > right[2*iR]) {
                    //make sure left interval begins first
                    val tmp1 = left
                    left = right
                    right = tmp1
                    val tmp2 = iL
                    iL = iR
                    iR = tmp2
                } else {
                    //merge left interval
                    val lastUpperBound = if (added >= 0) result[2*added+1] else lowerBound
                    val low = left[2*iL]
                    val high = left[2*iL+1]
                    if (low <= lastUpperBound && added >= 0) {
                        if (high > lastUpperBound) {
                            //intervals overlap
                            result[2*added+1] = high
                        } //else this interval is a strict subset
                    } else {
                        result.add(low)
                        result.add(high)
                        added += 1
                    }
                    iL += 1
                }
            }
            while (2*iL < left.size) {
                val lastUpperBound = result.last()  //must exists
                val low = left[2*iL]
                val high = left[2*iL+1]
                if (low <= lastUpperBound) {
                    if (high > lastUpperBound) {
                        result[result.lastIndex] = high
                    }
                } else {
                    result.add(low)
                    result.add(high)
                }
                iL += 1
            }
            while (2*iR < right.size) {
                val lastUpperBound = result.last()  //must exists
                val low = right[2*iR]
                val high = right[2*iR+1]
                if (low <= lastUpperBound) {
                    if (high > lastUpperBound) {
                        result[result.lastIndex] = high
                    }
                } else {
                    result.add(low)
                    result.add(high)
                }
                iR += 1
            }
            result.toDoubleArray()
        }
    }

    override fun DoubleArray.prettyPrint(): String = Arrays.toString(this)

    override fun ByteBuffer.getColors(): DoubleArray = DoubleArray(this.int) { this.double }

    override fun DoubleArray.byteSize(): Int = 4 + 8 * this.size

    override fun ByteBuffer.putColors(colors: DoubleArray): ByteBuffer = this.apply {
        putInt(colors.size)
        colors.forEach { putDouble(it) }
    }

    override fun DoubleArray.transferTo(solver: Solver<DoubleArray>): DoubleArray = this

}