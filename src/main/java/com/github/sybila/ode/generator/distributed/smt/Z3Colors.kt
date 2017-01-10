package com.github.sybila.ode.generator.distributed.smt

import com.microsoft.z3.BoolExpr

class Z3Params(
        var formula: BoolExpr,
        var sat: Boolean?,
        var minimal: Boolean = false
) {

    var asString: ByteArray? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Z3Params

        if (formula != other.formula) return false

        return true
    }

    override fun hashCode(): Int {
        return formula.hashCode()
    }

    override fun toString(): String = formula.toString()

}

fun BoolExpr.toParams(): Z3Params = Z3Params(this, null, false)