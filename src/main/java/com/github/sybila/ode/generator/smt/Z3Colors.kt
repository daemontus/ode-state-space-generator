package com.github.sybila.ode.generator.smt

import com.github.sybila.huctl.CompareOp

sealed class Z3Formula {

    class Value(
            val value: String
    ) : Z3Formula() {
        override fun asCommand(): String = if (value.endsWith("?"))
            value.substring(0, value.lastIndex)
        else value
    }

    class Plus(
            val args: List<Z3Formula>
    ) : Z3Formula() {
        override fun asCommand(): String
                = "(+ ${args.joinToString(separator = " ", transform = { it.asCommand() } )})"
    }

    class Minus(
            val args: List<Z3Formula>
    ) : Z3Formula() {
        override fun asCommand(): String
                = "(- ${args.joinToString(separator = " ", transform = { it.asCommand() } )})"
    }

    class Times(
            val args: List<Z3Formula>
    ) : Z3Formula() {
        override fun asCommand(): String
                = "(* ${args.joinToString(separator = " ", transform = { it.asCommand() } )})"
    }

    class Div(
            val args: List<Z3Formula>
    ) : Z3Formula() {
        override fun asCommand(): String
                = "(/ ${args.joinToString(separator = " ", transform = { it.asCommand() } )})"
    }

    class Compare(
            val left: Z3Formula,
            val cmp: CompareOp,
            val right: Z3Formula
    ) : Z3Formula() {

        override fun asCommand(): String {
            return "($cmp ${left.asCommand()} ${right.asCommand()})"
        }

    }

    class And(
            val args: List<Z3Formula>
    ) : Z3Formula() {
        override fun asCommand(): String
                = "(and ${args.joinToString(separator = " ", transform = { it.asCommand() } )})"
    }

    class Or(
            val args: List<Z3Formula>
    ) : Z3Formula() {
        override fun asCommand(): String
                = "(or ${args.joinToString(separator = " ", transform = { it.asCommand() } )})"
    }

    class Not(
            val inner: Z3Formula
    ) : Z3Formula() {
        override fun asCommand(): String = "(not ${inner.asCommand()})"
    }

    object True : Z3Formula() {
        override fun asCommand(): String = "true"
    }
    object False : Z3Formula() {
        override fun asCommand(): String = "false"
    }

    abstract fun asCommand(): String

    val isTrue: Boolean
            get() = this is True
    val isFalse: Boolean
            get() = this is False

    override fun toString(): String = asCommand()
}

class Z3Params(
        var formula: Z3Formula,
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

fun Z3Formula.toParams(): Z3Params = Z3Params(this, null, false)