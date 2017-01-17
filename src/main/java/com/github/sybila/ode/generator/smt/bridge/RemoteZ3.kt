package com.github.sybila.ode.generator.smt.bridge

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
            "model=false",
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

    val bounds: String = params.flatMap {
        listOf("(< ${it.name} ${it.range.second.safeString()})", "(> ${it.name} ${it.range.first.safeString()})")
    }.joinToString(separator = " ", prefix = "(and ", postfix = ")")

    val activeAssertions = HashSet<String>()

    init {
        //setup solver
        "(set-logic $logic)".execute()

        //add parameter bounds and declarations
        params.forEach {
            "(declare-const ${it.name} Real)".execute()
        }
        "(assert $bounds)".execute()

        "(apply ctx-solver-simplify)".execute()
        if ("(goals" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")
        if ("(goal" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")
        var line = readResult()
        while (!line.trim().startsWith(":precision")) {
            activeAssertions.add(line.filter { it != '?' })
            line = readResult()
        }
        if (")" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")

        /*println()
        val check = measureTimeMillis {
            repeat (1000) {
                minimize("(and (>= y_pRB 0.0005412787) (not (<= y_pRB 0.0005378015)))")
            }
        }
        println("elapsed $check" )*/
    }

    fun checkSat(formula: String): Boolean {
        if (formula.length > size) {
            size = formula.length
            println("Send: $size")
        }
        "(push)".execute()
        "(assert $formula)".execute()
        "(check-sat-using (using-params qflra :logic QF_LRA))".execute()
        val result = readResult()
        "(pop)".execute()
        return result == "sat"
    }

    var size = 0

    fun minimize(formula: String): String {
        if (formula.length > size) {
            size = formula.length
            println("Send: $size")
        }
        "(push)".execute()
        "(assert $formula)".execute()
        "(apply (repeat ctx-solver-simplify))".execute()
        //println("Minimize: $formula")
        if ("(goals" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")
        if ("(goal" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")
        val assertions = ArrayList<String>()
        var line = readResult()
        while (!line.startsWith(":precision")) {
            //println("assertion: $line")
            if (line !in activeAssertions) assertions.add(line)
            line = readResult()
        }
        if (")" != readResult()) throw IllegalStateException("Unexpected z3 output when minimizing")
        //println("To: $assertions")
        "(pop)".execute()
        return when {
            assertions.isEmpty() -> "true"
            assertions.size == 1 -> assertions.first()
            else -> assertions.joinToString(prefix = "(and ", postfix = ")", separator = " ")
        }
    }

    private fun String.execute() {
        if (verbose) println(this)
        commands.write(this+"\n")
        commands.flush()
    }

    private fun readResult(): String {
        val r = results.next().filter { it != '?' }.trim()
        if (verbose) println(r)
        return r
    }

    override fun close() {
        if (process.isAlive) {
            process.destroyForcibly()
        }
    }

}