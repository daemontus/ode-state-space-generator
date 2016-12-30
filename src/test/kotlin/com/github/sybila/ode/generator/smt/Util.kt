package com.github.sybila.ode.generator.smt

/*
fun Nodes<IDNode, SMTColors>.normalize(): List<Pair<IDNode, SMTColors>>
    = this.entries.map { Pair(it.key, it.value) }.sortedBy { it.first.id }

//semantically compare the formulas
fun assertEquals(a: List<Pair<IDNode, SMTColors>>, b: List<Pair<IDNode, SMTColors>>) {
    if (a.size != b.size) {
        error("Expected $a, got $b")
    } else {
        a.zip(b).forEach {
            val (left, right) = it
            if (left.first != right.first) {
                error("Expected $left, got $right in $a != $b")
            } else {
                if (!left.second.normalize().solverEquals(right.second.normalize())) {
                    error("Expected ${left.second.normalize()}, got ${right.second.normalize()} in $a != $b")
                }
            }
        }
    }
}*/