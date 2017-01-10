package com.github.sybila.ode.generator.smt.bridge

import com.github.sybila.ode.generator.smt.Z3Formula
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.safeString
import java.io.Closeable
import java.util.*


class RemoteZ3(
        params: List<OdeModel.Parameter>,
        private val verbose: Boolean = false,
        logic: String = "QF_LRA",
        z3Executable: String = "z3"
) : Closeable {

    val parameters =  listOf(
            "pp.single_line=true",
            "pp.fixed_indent=true",
            "pp.flat_assoc=true",
            "pp.decimal=true",
            "pp.min_alias_size=${Integer.MAX_VALUE}"    //this should prevent usage of let
    )

    val process = kotlin.run {
        val command = (listOf(z3Executable, "-in") + parameters)
        Runtime.getRuntime().exec(command.toTypedArray())
    }!!

    val results = process.inputStream.bufferedReader().lineSequence().iterator()
    val commands = process.outputStream.bufferedWriter()

    val bounds: Z3Formula = params.flatMap {
        listOf("(< ${it.name} ${it.range.second.safeString()})", "(> ${it.name} ${it.range.first.safeString()})")
    }.joinToString(separator = " ", prefix = "(and ", postfix = ")").readSMT().asZ3Formula()

    init {
        //setup solver
        "(set-logic $logic)".execute()

        //add parameter bounds and declarations
        params.forEach {
            "(declare-const ${it.name} Real)".execute()
        }
        "(assert ${bounds.asCommand()})".execute()
    }

    fun checkSat(formula: Z3Formula): Boolean {
        "(push)".execute()
        val command = formula.asCommand()
        "(assert $command)".execute()
        "(check-sat)".execute()
        val r = readResult()
        "(pop)".execute()
        return r == "sat"
    }

    fun minimize(formula: Z3Formula): Z3Formula {
        "(push)".execute()
        val command = formula.asCommand()
        "(assert $command)".execute()
        "(apply ctx-solver-simplify)".execute()
        if ("(goals" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")
        if ("(goal" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")
        val assertions = ArrayList<Z3Formula>()
        var line = readResult()
        while (!line.trim().startsWith(":precision")) {
            val smt = line.readSMT().asZ3Formula()
            println("Read: $smt")
            assertions.add(smt)
            line = readResult()
        }
        if (")" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")
        "(pop)".execute()
        return if (assertions.size == 1) assertions.first() else Z3Formula.And(assertions)
    }

    private fun String.execute() {
        if (verbose) println(this)
        commands.write(this+"\n")
        commands.flush()
    }

    private fun readResult(): String {
        val r = results.next()
        if (verbose) println(r)
        return r
    }

    override fun close() {
        if (process.isAlive) {
            process.destroyForcibly()
        }
    }

}
/*
fun main(args: Array<String>) {

    val params = listOf(
            OdeModel.Parameter("p1", 0.0 to 2.0),
            OdeModel.Parameter("p2", 0.0 to 2.0)
    )
    val z3 = RemoteZ3(params, verbose = true)

    val elapsed = measureTimeMillis {
        val f = Z3Formula.Compare(Z3Formula.Plus(listOf(
                Z3Formula.Value("-1.0"),
                Z3Formula.Times(listOf(
                        Z3Formula.Value("1.0"),
                        Z3Formula.Value("p1")
                )),
                Z3Formula.Times(listOf(
                        Z3Formula.Value("1.0"),
                        Z3Formula.Value("p2")
                ))
        )), true, Z3Formula.Value("0.0"))
        val min = z3.minimize(f)
        println("Minimal ${min.asCommand(params)} ${z3.minimize(min).asCommand(params)}")


    }

    println("Elapsed: $elapsed")

    z3.close()
}*/