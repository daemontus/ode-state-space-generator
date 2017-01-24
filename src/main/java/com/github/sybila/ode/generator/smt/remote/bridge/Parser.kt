package com.github.sybila.ode.generator.smt.remote.bridge

import java.util.*

private val whitespace = charArrayOf(' ', '\t', '\n', '\r')
private val parenthesis = charArrayOf('(', ')')

//A string can contains anything except whitespace and parenthesis.
//smt lib also allows quoted escaped strings, but we will consider these invalid
//because they can't be created by our app.
private val stringInvalid = whitespace + parenthesis

/**
 * Parser assumptions:
 *  - Each parsing will only concern one line! (This is not necessary, but
 * simplifies the request -> response matching)
 *  - :key value parameters are skipped
 *  - line is not empty and does NOT end with newline char
 */

fun String.readSMT(): SMT {
    return AdvancingIterator("${this.trim()}\n".iterator()).readSMT(true)
}

private fun AdvancingIterator.readSMT(topLevel: Boolean = false): SMT {
    return if (get() == '(') {
        nextToken() //move from '('
        val funName = readString()
        val args = java.util.ArrayList<SMT>()
        while (get() != ')') {
            if (get() == ':') {
                //extra parameters are skipped
                nextToken()     //move from ':'
                readString()    //key
                readString()    //value
            } else {
                args.add(readSMT())
            }
        }
        if (topLevel) {
            next()          //just skip ')'
        } else {
            nextToken()     //also skip whitespace
        }
        SMT.Expression(funName, args)
    } else {
        SMT.Terminal(readString(topLevel))
    }
}

private fun AdvancingIterator.skipWhitespace() {
    this.skipWhile { it in whitespace }
}

private fun AdvancingIterator.nextToken() {
    next(); skipWhitespace()
}

private fun AdvancingIterator.readString(topLevel: Boolean = false): String {
    val data = StringBuilder()
    while (get() !in stringInvalid) {
        data.append(get())
        next()
    }
    if (!topLevel) skipWhitespace()
    if (data.isEmpty()) throw IllegalStateException("empty terminal")
    return data.toString()
}

private fun AdvancingIterator.skipWhile(predicate: (Char) -> Boolean) {
    while (predicate(get())) next()
}

/**
 * A variant of char iterator that keeps a reference to the last iterated item.
 *
 * WARNING: It assumes the iterator is not empty and
 */
private class AdvancingIterator(
        private val it: CharIterator
) : CharIterator() {

    private var current = it.nextChar()

    override fun hasNext(): Boolean = it.hasNext()

    override fun nextChar(): Char {
        current = it.nextChar()
        return current
    }

    fun get(): Char = current

}