package com.github.sybila.ode.generator.smt

import com.github.sybila.ode.model.Model
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import java.util.*

fun Status.isSat() = this == Status.SATISFIABLE
fun Status.isUnknown() = this == Status.UNKNOWN
fun Status.isUnsat() = this == Status.UNSATISFIABLE

var timeToOrder = 0L


private val unsatCache = HashSet<BoolExpr>()
private val tautologyCache = HashSet<BoolExpr>()

class PartialOrderSet(
        private val z3: Context,
        val paramBounds: Array<BoolExpr>,
        private val logic: String = "qflra"
) {

    var size = 0
        private set

    val width: Int
        get() = chains.size

    fun String.toConst(z3: Context) = z3.mkRealConst(this)
    fun Double.toReal(z3: Context) = z3.mkReal(this.toString())
    fun BoolExpr.not() = z3.mkNot(this)

    constructor(
            z3: Context, params: List<Model.Parameter>, logic: String = "qflra"
    ) : this(z3, if (params.isEmpty()) Array<BoolExpr>(1) { z3.mkTrue() } else {
        params.flatMap {
            val p = z3.mkRealConst(it.name)
            listOf(z3.mkGt(p, z3.mkReal(it.range.first.toString())), z3.mkLt(p, z3.mkReal(it.range.second.toString())))
        }.toTypedArray()
    }, logic)


    //initialize solver with requested logic and parameter bounds
    private val solver = z3.mkSolver(z3.mkTactic(logic)).apply {
        this.add(*paramBounds)
    }

    fun BoolExpr.isSuperSet(other: BoolExpr): Boolean {
        val start = System.nanoTime()
        val result = solver.check(other, this.not()).isUnsat()
        timeToOrder += System.nanoTime() - start
        //println("Is $this superset of $other? $result")
        return result
    }

    private val chains = ArrayList<MutableList<BoolExpr>>()

    private val equations = HashMap<BoolExpr, List<BoolExpr>>()

    init {

    }

    fun bigger(e1: BoolExpr, e2: BoolExpr): BoolExpr? {
        if (e1 == e2) return null
       // println("compare")
        //if (e1 !in equations) {
          //  val r = add(e1)
         //   println("Add e1: $r")
        //}
        //if (e2 !in equations) {
          //  val r =add(e2)
           // println("Add e2: $r")
        //}
        if (e1 in tautologyCache) return e1
        if (e2 in tautologyCache) return e2
        if (e1 in unsatCache) return e2
        if (e2 in unsatCache) return e1
        if (equations[e1] !== equations[e2]) {  //Yes, we want a reference equivalence
            return null    //can't compare
        }
        val chain = equations[e1] ?: equations[e2] ?: throw IllegalStateException("Problem in $e1 and $e2")
        if (chain.indexOf(e1) < chain.indexOf(e2)) {
            return e2
        } else {
            return e1
        }
    }

    fun contains(e: BoolExpr) = e in equations

    /**
     * Returns true if given equation was added (therefore is satisfiable)
     */
    fun add(equation: BoolExpr): Boolean  //first, check if equation is satisfiable and fail fast if it is not
    {
        if (equation in equations) return true  //We already have this equation, don't double add
        if (equation in unsatCache) return false
        if (equation in tautologyCache) return true
        val start = System.nanoTime()
        val r = solver.check(equation)!!
        timeToOrder += System.nanoTime() - start
        return when (r) {
            Status.UNKNOWN -> throw IllegalStateException("Cannot decide this equation: $equation")
            Status.UNSATISFIABLE -> {
                unsatCache.add(equation)
                false
            }
            Status.SATISFIABLE -> {
                if (solver.check(equation.not()).isUnsat()) {
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
     */
    fun addBiggest(items: List<BoolExpr>): List<BoolExpr> {
        if (items.isEmpty()) return items
        val set = PartialOrderSet(z3, paramBounds, logic)
        var hasTrue = false
        for (i in items) {
            if (i.isTrue) {
                hasTrue = true
            } else if (i.isFalse) {
                //nothing
            } else {
                set.add(i)
            }
        }
        //return items
        val results = set.chains.map { it.last() }
        for (r in results) {
            add(r)
        }
        return if(hasTrue && results.size == 0) listOf(z3.mkTrue()) else results
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
            } else {    //incomparable (remember, they can't be equal!)
                return false
            }
        }
        //If we got here, we are comparable to everything that we've met along the way
        //especially an element before and above us.
        //And since partial order is transitive, that means we are comparable to entire chain.
        val insertIndex = low
        //println("Low: $low")
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