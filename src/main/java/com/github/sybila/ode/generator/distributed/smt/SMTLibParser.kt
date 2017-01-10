package com.github.sybila.ode.generator.distributed.smt

import java.util.*

sealed class SMT {

    class Terminal(val data: String) : SMT() {
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

        override fun toString(): String = "($funName ${funArgs.joinToString(separator = " ")})"
    }

}

val whitespace = charArrayOf(' ', '\t', '\n', '\r')
val parenthesis = charArrayOf('(', ')')

val stringInvalid = whitespace + parenthesis

fun Iterator<Char>.readSMT(): SMT {
    return AdvancingIterator(this).readSMT(true)
}

private fun AdvancingIterator<Char>.readSMT(topLevel: Boolean = false): SMT {
    println("Reading: ${get()}")
    return if (get() == '(') {
        nextToken()
        val funName = readString()
        val args = ArrayList<SMT>()
        while (get() != ')') {
            if (get() == ':') {
                //extra parameters are skipped
                nextToken()
                readString()    //key
                readString()    //value
            } else {
                args.add(readSMT())
            }
        }
        if (topLevel) {
            next()  //just skip ')'
        } else {
            nextToken() //skip also whitespace
        }
        SMT.Expression(funName, args)
    } else {
        SMT.Terminal(readString(topLevel))
    }
}

private fun AdvancingIterator<Char>.skipWhitespace() {
    this.skipWhile { it in com.github.sybila.ode.generator.distributed.smt.whitespace }
}

private fun AdvancingIterator<Char>.nextToken() {
    next(); skipWhitespace()
}

private fun AdvancingIterator<Char>.readString(topLevel: Boolean = false): String {
    val data = StringBuilder()
    while (get() !in stringInvalid) {
        data.append(get())
        next()
    }
    if (!topLevel) skipWhitespace()
    println("Read: \"${data.toString()}\"")
    return data.toString()
}
private class AdvancingIterator<I>(
        private val it: Iterator<I>
) : Iterator<I> {

    private var current = it.next()

    override fun hasNext(): Boolean = it.hasNext()

    override fun next(): I {
        current = it.next()
        return current
    }

    fun get(): I = current

}

private fun <I> AdvancingIterator<I>.skipWhile(predicate: (I) -> Boolean) {
    while (predicate(get())) next()
}