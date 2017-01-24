package com.github.sybila.ode.generator.smt.remote.bridge

/**
 * An abstract syntax tree of a smt lib 2 formula.
 *
 * Key-value parameters (:param val) are not allowed.
 */
sealed class SMT {

    class Terminal(val data: String) : SMT() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as SMT.Terminal

            if (data != other.data) return false

            return true
        }

        override fun hashCode(): Int {
            return data.hashCode()
        }

        override fun toString(): String = data
    }

    class Expression(
            val funName: String,
            val funArgs: List<SMT>
    ) : SMT() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as SMT.Expression

            if (funName != other.funName) return false
            if (funArgs != other.funArgs) return false

            return true
        }

        override fun hashCode(): Int {
            var result = funName.hashCode()
            result = 31 * result + funArgs.hashCode()
            return result
        }

        override fun toString(): String = if (funArgs.isNotEmpty()) {
            "($funName ${funArgs.joinToString(separator = " ")})"
        } else {
            "($funName)"
        }
    }

    //abstract fun asZ3Formula(): Z3Formula

}

fun String.f(vararg args: SMT): SMT {
    return SMT.Expression(this, args.toList())
}

fun String.t(): SMT {
    return SMT.Terminal(this)
}