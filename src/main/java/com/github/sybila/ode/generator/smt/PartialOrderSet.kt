package com.github.sybila.ode.generator.smt

import com.github.sybila.ode.model.Model
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.RealExpr
import com.microsoft.z3.Status
import java.io.*
import java.util.*

//nanosecond time spent ordering the set
var timeInOrdering = 0L
var solverCallsInOrdering = 0L

class PartialOrderSet(
        val paramBounds: Array<BoolExpr>,
        val params: List<RealExpr>,
        private val logic: String = "qflra",
        private val unsatCache: MutableSet<BoolExpr> = HashSet<BoolExpr>(),
        private val tautologyCache: MutableSet<BoolExpr> = HashSet<BoolExpr>()
) {

    var size = 0
        private set

    val width: Int
        get() = chains.size

    //initialize solver with requested logic and parameter bounds
    val solver = z3.mkSolver(z3.mkTactic(logic)).apply {
        this.add(*paramBounds)  //spread array operator
    }!!

    private fun BoolExpr.isSuperSet(other: BoolExpr): Boolean {
        val start = System.nanoTime()
        val result = solver.check(other, this.not()).isUnsat()
        timeInOrdering += System.nanoTime() - start
        solverCallsInOrdering += 1
        return result
    }

    //chains of equations, sorted from smallest (false) to biggest (true)
    private val chains = ArrayList<MutableList<BoolExpr>>()

    //mapping from equations to their respective chains
    private val equations = HashMap<BoolExpr, List<BoolExpr>>()

    constructor(
            params: List<Model.Parameter>, logic: String = "qflra"
    ) : this(if (params.isEmpty()) Array<BoolExpr>(1) { z3True } else {
        params.flatMap {
            val p = it.name.toZ3()
            listOf(p gt it.range.first.toZ3(), p lt it.range.second.toZ3())
        }.toTypedArray()
    }, params.map { it.name.toZ3() }, logic)

    constructor(
            params: List<Model.Parameter>, logic: String = "qflra",
            chainFile: File, tautologyFile: File, unsatFile: File
    ) : this(params, logic) {

        ObjectInputStream(chainFile.inputStream()).use { stream ->
            var count = stream.readInt()
            repeat(count) {
                var serialized = stream.readObject() as Array<LongArray>
                val chain = serialized.map {
                    it.readBoolExpr(0, this.params).first
                }.toMutableList()
                chain.forEach { equations[it] = chain }
                chains.add(chain)
            }
        }

        ObjectInputStream(tautologyFile.inputStream()).use { stream ->
            var serialized = stream.readObject() as Array<LongArray>
            tautologyCache.addAll(serialized.map {
                it.readBoolExpr(0, this.params).first
            })
        }

        ObjectInputStream(unsatFile.inputStream()).use { stream ->
            var serialized = stream.readObject() as Array<LongArray>
            unsatCache.addAll(serialized.map {
                it.readBoolExpr(0, this.params).first
            })
        }

    }

    fun serialize(chainFile: File, tautologyFile: File, unsatFile: File) {
        ObjectOutputStream(chainFile.outputStream()).use { stream ->
            stream.writeInt(chains.size)
            chains.forEach { chain ->
                val serialized: Array<LongArray> = chain.map {
                    val size = it.calculateBufferSize()
                    val array = LongArray(size)
                    it.serialize(array, 0, params)
                    array
                }.toTypedArray()
                stream.writeObject(serialized)
            }
        }
        ObjectOutputStream(tautologyFile.outputStream()).use { stream ->
            val serialized: Array<LongArray> = tautologyCache.map {
                val size = it.calculateBufferSize()
                val array = LongArray(size)
                it.serialize(array, 0, params)
                array
            }.toTypedArray()
            stream.writeObject(serialized)
        }
        ObjectOutputStream(unsatFile.outputStream()).use { stream ->
            val serialized: Array<LongArray> = unsatCache.map {
                val size = it.calculateBufferSize()
                val array = LongArray(size)
                it.serialize(array, 0, params)
                array
            }.toTypedArray()
            stream.writeObject(serialized)
        }
    }

    /**
     * Return the bigger one of the arguments. If arguments are incomparable, returns null.
     * If they are semantically equal, there is no guarantee on what will be returned.
     * It will throw an exception if one of the arguments is not in the set.
     */
    fun bigger(e1: BoolExpr, e2: BoolExpr): BoolExpr? {
        if (e1 == e2) return null
        if (e1 in tautologyCache) return e1
        if (e2 in tautologyCache) return e2
        if (e1 in unsatCache) return e2
        if (e2 in unsatCache) return e1
        if (equations[e1] !== equations[e2]) {  //Yes, we want a reference equivalence
            return null    //can't compare
        }
        val chain = equations[e1] ?: equations[e2] ?: throw IllegalStateException("Problem comparing $e1 and $e2")
        if (chain.indexOf(e1) < chain.indexOf(e2)) {
            return e2
        } else {
            return e1
        }
    }

    fun contains(e: BoolExpr) = e in equations

    /**
     * Returns true if given equation was is satisfiable
     */
    fun add(equation: BoolExpr): Boolean  //first, check if equation is satisfiable and fail fast if it is not
    {
        if (equation in equations) return true  //We already have this equation, don't double add
        if (equation in unsatCache) return false
        if (equation in tautologyCache) return true
        val start = System.nanoTime()
        val r = solver.check(equation)!!
        solverCallsInOrdering += 1
        timeInOrdering += System.nanoTime() - start
        return when (r) {
            Status.UNKNOWN -> throw IllegalStateException("Cannot decide this equation: $equation")
            Status.UNSATISFIABLE -> {
                unsatCache.add(equation)
                false
            }
            Status.SATISFIABLE -> {
                val s = System.nanoTime()
                val tautology = solver.check(equation.not()).isUnsat()
                solverCallsInOrdering += 1
                timeInOrdering += System.nanoTime() - s
                if (tautology) {
                    //this is a tautology!
                    tautologyCache.add(equation)
                    return true
                }
                if (chains.any { tryInsert(equation, it) }) {
                    //we managed to insert it into some of the existing chains
                } else {
                    //we need a new chain
                    val chain = arrayListOf(equation)
                    chains.add(chain)
                    equations[equation] = chain
                }
                size += 1
                true
            }
        }
    }

    /**
     * Remove subsets and insert only relevant items. Return these items.
     * Items must be valid equations, not true/false/etc...
     * Returns empty list if all items are unsatisfiable and null if list contains a tautology.
     */
    fun addBiggest(items: List<BoolExpr>): List<BoolExpr>? {
        if (items.isEmpty()) return items
        val set = PartialOrderSet(paramBounds, params, logic, unsatCache, tautologyCache)
        for (i in items) {
            set.add(i)
            if (i in tautologyCache) {
                return null
            }
        }
        //return items
        val results = set.chains.map { it.last() }
        for (r in results) {
            add(r)
        }
        return results
    }

    private fun tryInsert(equation: BoolExpr, chain: MutableList<BoolExpr>): Boolean {
        //copy of binary search method - we need this here because we need to break if we find incomparable elements
        //so we can't use the method directly (maybe we could hack it somehow, but it would be kind of ugly)
        var low = 0
        var high = chain.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows
            val midVal = chain[mid]
            if (equation.isSuperSet(midVal)) {  //equation > midVal
                low = mid + 1
            } else if(midVal.isSuperSet(equation)) { //midVal < equation
                high = mid - 1
            } else {    //incomparable
                return false
            }
        }
        //If we got here, we are comparable to everything that we've met along the way
        //especially an element before and above us.
        //And since partial order is transitive, that means we are comparable to entire chain.
        val insertIndex = low
        if (insertIndex >= chain.size) {
            chain.add(equation)
        } else {
            chain.add(insertIndex, equation)
        }
        equations.put(equation, chain)
        return true
    }

    override fun toString(): String {
        return chains.toString()
    }
}