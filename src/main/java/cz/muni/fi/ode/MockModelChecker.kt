package cz.muni.fi.ode

import cz.muni.fi.ctl.Formula

interface ColorSet {

    fun intersect(set: ColorSet?)
    fun union(set: ColorSet?) : Boolean
    fun subtract(set: ColorSet?)
    fun isEmpty(): Boolean

}

interface Node

interface ModelAdapter<N: Node, C: ColorSet> {

    fun predecessorsFor(to: N, borders: C): Map<N, C>
    fun successorsFor(to: N, borders: C): Map<N, C>
    fun initialNodes(f: Formula): Map<N, C>
    fun addFormula(n: N, f: Formula, p: C): Boolean
    fun validColorsFor(n: N, f: Formula): C
    fun purge(formula: Formula)

}