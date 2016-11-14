package com.github.sybila.ode.exe

import com.github.sybila.ctl.*
import com.github.sybila.ode.model.*
import com.google.gson.*
import java.io.File

fun main(args: Array<String>) {
    try {
        if (args.isEmpty()) throw IllegalArgumentException("Missing argument: .bio file")
        if (args.size < 2) throw IllegalArgumentException("Mussing argument: .ctl file")
        val propertyFile = File(args[1])
        val modelFile = File(args[0])
        val formulas = CTLParser().parse(propertyFile).entries
                .filter { it.key.endsWith("?") }
                .map { it.key to it.value.normalize().optimize() }

        val model = Parser().parse(modelFile)

        println(serialize(formulas, model))
    } catch (e: Exception) {
        System.err.println("${e.message} (${e.javaClass.name})")
    }
}

fun serialize(formula: List<Pair<String, Formula>>, model: Model): String {
    val gson = GsonBuilder()
            .registerTypeHierarchyAdapter(Formula::class.java, JsonSerializer<Formula> { src, typeOfSrc, context ->
                when {
                    src == True || src is TT -> JsonObject().apply {
                        addProperty("operator", "Atom")
                        addProperty("inner", "True")
                    }
                    src == False || src is FF -> JsonObject().apply {
                        addProperty("operator", "Atom")
                        addProperty("inner", "False")
                    }
                    src is FloatProposition -> {
                        JsonObject().apply {
                            addProperty("operator", "Atom")
                            add("inner", model.serializeProposition(src.left, src.compareOp, src.right))
                        }
                    }
                    src.operator == Op.EXISTS_NEXT -> {
                        JsonObject().apply {
                            addProperty("operator", "EX")
                            add("inner", context.serialize(src[0]))
                        }
                    }
                    src.operator == Op.NEGATION -> {
                        JsonObject().apply {
                            addProperty("operator", "Not")
                            add("inner", context.serialize(src[0]))
                        }
                    }
                    src.operator == Op.AND -> {
                        JsonObject().apply {
                            addProperty("operator", "And")
                            add("left", context.serialize(src[0]))
                            add("right", context.serialize(src[1]))
                        }
                    }
                    src.operator == Op.EXISTS_UNTIL -> {
                        JsonObject().apply {
                            addProperty("operator", "EU")
                            add("path", context.serialize(src[0]))
                            add("reach", context.serialize(src[1]))
                        }
                    }
                    src.operator == Op.ALL_UNTIL -> {
                        JsonObject().apply {
                            addProperty("operator", "AU")
                            add("path", context.serialize(src[0]))
                            add("reach", context.serialize(src[1]))
                        }
                    }
                    else -> throw IllegalArgumentException("Unsupported formula: $src")
                }
            })
            .registerTypeAdapter(Model::class.java, JsonSerializer<Model> { src, typeOfSrc, context ->
                JsonObject().apply {
                    addProperty("name", "unknown")
                    add("variables", context.serialize(src.variables))
                    add("parameters", context.serialize(src.parameters))
                }
            })
            .registerTypeAdapter(Model.Variable::class.java, JsonSerializer<Model.Variable> { src, typeOfSrc, context ->
                JsonObject().apply {
                    addProperty("name", src.name)
                    add("range", JsonObject().apply {
                        addProperty("min", src.range.first)
                        addProperty("max", src.range.second)
                    })
                    add("thresholds", context.serialize(src.thresholds))
                    add("equation", context.serialize(src.equation))
                }
            })
            .registerTypeAdapter(Summand::class.java, JsonSerializer<Summand> { src, typeOfSrc, context ->
                JsonObject().apply {
                    addProperty("constant", src.constant)
                    add("parameterIndices", JsonArray().apply {
                        if (src.paramIndex >= 0) {
                            add(src.paramIndex)
                        }
                    })
                    add("variableIndices", context.serialize(src.variableIndices))
                    add("evaluables", context.serialize(src.evaluable))
                }
            })
            .registerTypeHierarchyAdapter(Evaluable::class.java, JsonSerializer<Evaluable> { src, typeOfSrc, context ->
                when (src) {
                    is RampApproximation -> {
                        JsonObject().apply {
                            addProperty("type", "ramp_approximation")
                            addProperty("variableIndex", src.varIndex)
                            add("approximation", context.serialize(src.thresholds.zip(src.values).map {
                                Point(it.first, it.second)
                            }))
                        }
                    }
                    else -> throw IllegalArgumentException("Unsupported evaluable $src")
                }
            })
            .registerTypeAdapter(Model.Parameter::class.java, JsonSerializer<Model.Parameter> { src, typeOfSrc, context ->
                JsonObject().apply {
                    addProperty("name", src.name)
                    add("range", JsonObject().apply {
                        addProperty("min", src.range.first)
                        addProperty("max", src.range.second)
                    })
                }
            })
            .setPrettyPrinting().create()
    return gson.toJson(Config(model, formula.map { it.copy(second = it.second.transform()) }))
}

class TT : Formula {
    override val operator: Op = Op.ATOM
    override val subFormulas: List<Formula> = listOf()
}

class FF : Formula {
    override val operator: Op = Op.ATOM
    override val subFormulas: List<Formula> = listOf()
}

private fun Formula.transform(): Formula {
    return when {
        this == True -> TT()
        this == False -> FF()
        this.operator.cardinality == 0 -> this
        else -> {
            FormulaImpl(this.operator, this.subFormulas.map { it.transform() })
        }
    }
}

class Point(val threshold: Double, val value: Double)
class Config(val model: Model, val formulas: List<Pair<String, Formula>>)


fun Model.serializeProposition(left: Expression, compareOp: CompareOp, right: Expression): JsonObject {
    return when {
        left is Variable && right is Constant -> JsonObject().apply {
            val i = this@serializeProposition.findVariableIndex(left.name)
            val t = this@serializeProposition.findThresholdIndex(i, right.value)
            addProperty("variableIndex", i)
            addProperty("thresholdIndex", t)
            addProperty("compareOp", when (compareOp) {
                CompareOp.LT, CompareOp.LT_EQ -> "LT"
                CompareOp.GT, CompareOp.GT_EQ -> "GT"
                else -> throw IllegalArgumentException("Operator $compareOp is not supported!")
            })
        }
        left is Constant && right is Variable -> JsonObject().apply {
            val i = this@serializeProposition.findVariableIndex(right.name)
            val t = this@serializeProposition.findThresholdIndex(i, left.value)
            addProperty("variableIndex", i)
            addProperty("thresholdIndex", t)
            addProperty("compareOp", when (compareOp) {
                CompareOp.LT, CompareOp.LT_EQ -> "GT"
                CompareOp.GT, CompareOp.GT_EQ -> "LT"
                else -> throw IllegalArgumentException("Operator $compareOp is not supported!")
            })
        }
        else -> throw IllegalArgumentException("Proposition is too complex: $left $compareOp $right")
    }
}

fun Model.findVariableIndex(name: String): Int {
    val i = this.variables.indexOfFirst { it.name == name }
    if (i < 0) {
        throw IllegalArgumentException("Model does not contain variable $name")
    } else {
        return i
    }
}

fun Model.findThresholdIndex(variable: Int, value: Double): Int {
    val i = this.variables[variable].thresholds.indexOfFirst { it == value }
    if (i < 0) {
        throw IllegalArgumentException("Model does not contain threshold $value for ${
            this.variables[variable].name
        }")
    } else {
        return i
    }
}