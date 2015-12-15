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
    fun invertNodeSet(nodes: Map<N, C>): Map<N, C>

}

interface StateSpacePartitioner<N: Node> {

    fun getMyId(): Int

    fun getNodeOwner(node: CoordinateNode): Int

}

abstract class BlockingTaskMessenger<N: Node, C: ColorSet> {
    abstract fun sendTask(destinationNode: Int, internal: CoordinateNode, external: CoordinateNode, colors: TreeColorSet)
    abstract fun blockingReceiveTask(taskListener: OnTaskListener<CoordinateNode, TreeColorSet>): Boolean
    abstract fun finishSelf()

}

interface OnTaskListener<N: Node, C: ColorSet> {
    fun onTask(s: Int, t: N, d: N, c: C)
}