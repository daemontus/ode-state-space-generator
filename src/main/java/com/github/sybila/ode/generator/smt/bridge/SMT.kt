package com.github.sybila.ode.generator.smt.bridge

import com.github.sybila.huctl.CompareOp
import com.github.sybila.ode.generator.smt.Z3Formula

/**
 * An abstract syntax tree of a smt lib 2 formula.
 *
 * Key-value parameters (:param val) are not allowed.
 */
sealed class SMT {

    class Terminal(val data: String) : SMT() {

        override fun asZ3Formula(): Z3Formula {
            //Note: Inequalities are handled separately
            return when (this.data) {
                "true" -> Z3Formula.True
                "false" -> Z3Formula.False
                else -> Z3Formula.Value(this.data)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Terminal

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

        override fun asZ3Formula(): Z3Formula {
            return when (funName) {
                "-" -> Z3Formula.Minus(funArgs.map(SMT::asZ3Formula))
                "+" -> Z3Formula.Plus(funArgs.map(SMT::asZ3Formula))
                "*" -> Z3Formula.Times(funArgs.map(SMT::asZ3Formula))
                "/" -> Z3Formula.Div(funArgs.map(SMT::asZ3Formula))
                ">=" -> Z3Formula.Compare(funArgs[0].asZ3Formula(), CompareOp.GE, funArgs[1].asZ3Formula())
                ">" -> Z3Formula.Compare(funArgs[0].asZ3Formula(), CompareOp.GT, funArgs[1].asZ3Formula())
                "<=" -> Z3Formula.Compare(funArgs[0].asZ3Formula(), CompareOp.LE, funArgs[1].asZ3Formula())
                "<" -> Z3Formula.Compare(funArgs[0].asZ3Formula(), CompareOp.LT, funArgs[1].asZ3Formula())
                "not" -> {
                    if (funArgs.size != 1) throw IllegalStateException("Invalid formula $this")
                    val inner = funArgs[0].asZ3Formula()
                    Z3Formula.Not(inner)
                }
                "and" -> Z3Formula.And(funArgs.map(SMT::asZ3Formula))
                "or" -> Z3Formula.Or(funArgs.map(SMT::asZ3Formula))
                else -> throw IllegalStateException("Unknown operator: $this")
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Expression

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

    abstract fun asZ3Formula(): Z3Formula

}

fun String.f(vararg args: SMT): SMT {
    return SMT.Expression(this, args.toList())
}

fun String.t(): SMT {
    return SMT.Terminal(this)
}