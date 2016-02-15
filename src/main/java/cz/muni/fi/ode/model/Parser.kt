package cz.muni.fi.ode.model

import cz.muni.fi.ctl.antlr.ODEBaseListener
import cz.muni.fi.ctl.antlr.ODEParser
import cz.muni.fi.ctl.set
import org.antlr.v4.runtime.tree.ParseTreeProperty
import java.util.*

private class ModelReader : ODEBaseListener() {

    private val equations = HashMap<String, Evaluable>()
    private val thresholds = HashMap<String, List<Double>>()
    private val constants = HashMap<String, Double>()
    private val initial = HashMap<String, Pair<Double, Double>>()
    private val varPoints = HashMap<String, Pair<Int, Int>>()
    private var parameters = ArrayList<Model.Parameter>()    //preserve ordering
    private var parameterNames = ArrayList<String>()    //preserve ordering
    private var variables = ArrayList<String>()

    private val expressionTree = ParseTreeProperty<Evaluable>()

    /**
     * Conversion to standard model, with various integrity checks.
     */
    fun toModel(): Model {
        assert(parameters.map { it.name } == parameterNames)

        //basic integrity checks
        if (variables.isEmpty()) {
            throw IllegalStateException("Model has no variables!")
        }
        for (v in variables) {
            if (v !in thresholds) {
                throw IllegalStateException("No thresholds for variable $v")
            }
            if (v !in equations) {
                throw IllegalStateException("No equation for variable $v")
            }
        }
        for (t in thresholds.entries) {
            if (t.value.size < 2) {
                throw IllegalStateException("You must provide at least two thresholds for each variable")
            }
        }

        fun checkExtra(name: String, data: Iterable<String>) {
            val extra = data - variables
            if (extra.isNotEmpty()) {
                throw IllegalStateException("$name for non existing variables: $extra")
            }
        }
        checkExtra("Equations", equations.keys)
        checkExtra("Thresholds", thresholds.keys)
        checkExtra("Var points", varPoints.keys)
        checkExtra("Initial", initial.keys)

        val nameClash1 = constants.keys intersect variables
        if (nameClash1.isNotEmpty()) {
            throw IllegalStateException("$nameClash1 can't be a constant and a variable at the same time")
        }
        val nameClash2 = constants.keys intersect parameters.map { it.name }
        if (nameClash2.isNotEmpty()) {
            throw IllegalStateException("$nameClash2 can't be a constant and a parameter at the same time")
        }
        val nameClash3 = variables intersect parameters.map { it.name }
        if (nameClash3.isNotEmpty()) {
            throw IllegalStateException("$nameClash2 can't be a variable and a parameter at the same time")
        }

        return Model(
                variables.map { vName ->
                    Model.Variable(
                            name = vName,
                            range = Pair(thresholds[vName]!!.first(), thresholds[vName]!!.last()),
                            thresholds = thresholds[vName]!!,
                            varPoints = varPoints[vName],
                            equation = flattenAndResolve(equations[vName]!!)
                    )
                },
                parameters
        )
    }

    private fun flattenAndResolve(target: Evaluable): List<Summand> = when (target) {
        is Reference.Number -> listOf(Summand(constant = target.value))
        is Reference.Name -> when (target.value) {
            in variables -> listOf(Summand(variableIndices = listOf(variables.indexOf(target.value))))
            in parameterNames -> listOf(Summand(paramIndex = parameterNames.indexOf(target.value)))
            in constants -> listOf(Summand(constant = constants[target.value]!!))
            else -> throw IllegalStateException("Undefined reference: ${target.value}")
        }
        is AbstractHill -> listOf(Summand(evaluable = listOf(target.toHill(this))))
        is AbstractSigmoid -> listOf(Summand(evaluable = listOf(target.toSigmoid(this))))
        is AbstractRamp -> listOf(Summand(evaluable = listOf(target.toRamp(this))))
        is AbstractStep -> listOf(Summand(evaluable = listOf(target.toStep(this))))
        is Plus -> flattenAndResolve(target.e1) + flattenAndResolve(target.e2)
        is Negation -> flattenAndResolve(target.expr).map { it.copy(constant = -1.0 * it.constant) }
        is Times -> {
            val s1 = flattenAndResolve(target.e1)
            val s2 = flattenAndResolve(target.e2)
            s1.flatMap { i1 -> s2.map { i2 -> i1 * i2 } }
        }
        else -> throw IllegalStateException("Can't flatten or resolve: $target")
    }

    internal fun resolveArgument(arg: Reference): Double = when(arg) {
        is Reference.Name -> {
            when {
                arg.value in variables -> throw IllegalStateException("Variable can't be an argument to function: ${arg.value}")
                arg.value in parameterNames -> throw IllegalStateException("Parameter can't be an argument to function: ${arg.value}")
                arg.value in constants -> constants[arg.value]!!
                else -> throw IllegalStateException("Undefined reference: ${arg.value}")
            }
        }
        is Reference.Number -> arg.value
    }

    internal fun resolveVarName(name: String): Int {
        val index = variables.indexOf(name)
        if (index < 0) throw IllegalStateException("Undefined variable: $name")
        return index
    }

    /*
        Parsing basic model properties: thresholds, constants, initial states, var points, parameters, variable names...
     */

    override fun exitVarName(ctx: ODEParser.VarNameContext) {
        variables.add(ctx.NAME().text!!)
    }

    override fun exitParamInterval(ctx: ODEParser.ParamIntervalContext) {
        parameters.add(Model.Parameter(ctx.NAME().text!!,
                Pair(ctx.NUMBER(0).text.toDouble(), ctx.NUMBER(1).text.toDouble())
        ))
        parameterNames.add(ctx.NAME().text!!)
    }

    override fun exitConstant(ctx: ODEParser.ConstantContext) {
        constants.put(ctx.NAME().text!!, ctx.NUMBER().text.toDouble())
    }

    override fun exitInitInterval(ctx: ODEParser.InitIntervalContext) {
        initial.put(ctx.NAME().text!!,
                Pair(ctx.NUMBER(0).text.toDouble(), ctx.NUMBER(1).text.toDouble())
        )
    }

    override fun exitVarInterval(ctx: ODEParser.VarIntervalContext) {
        varPoints.put(ctx.NAME().text!!,
                Pair(ctx.NUMBER(0).text.toInt(), ctx.NUMBER(1).text.toInt())
        )
    }

    override fun exitThresholds(ctx: ODEParser.ThresholdsContext) {
        thresholds.put(ctx.NAME().text!!, ctx.NUMBER().map { it.text!!.toDouble() })
    }

    /*
        Equation parsing
     */

    override fun exitEquation(ctx: ODEParser.EquationContext) {
        equations.put(ctx.NAME().text!!, expressionTree[ctx.expr()])
    }

    override fun exitNumberEval(ctx: ODEParser.NumberEvalContext) {
        expressionTree[ctx] = Reference.Number(ctx.NUMBER().text.toDouble())
    }

    override fun exitNameEval(ctx: ODEParser.NameEvalContext) {
        expressionTree[ctx] = Reference.Name(ctx.NAME().text)
    }

    override fun exitRampEval(ctx: ODEParser.RampEvalContext) {
        expressionTree[ctx] = AbstractRamp(
                ctx.ramp().NAME().text!!,
                ctx.ramp().arg(0).toReference(),
                ctx.ramp().arg(1).toReference(),
                ctx.ramp().arg(2).toReference(),
                ctx.ramp().arg(3).toReference(),
                ctx.ramp().RAMP().text[1] == 'p',
                ctx.ramp().RAMP().text.length > 2
        )
    }

    override fun exitSigmoidEval(ctx: ODEParser.SigmoidEvalContext) {
        expressionTree[ctx] = AbstractSigmoid(
                ctx.sigm().NAME().text!!,
                ctx.sigm().arg(0).toReference(),
                ctx.sigm().arg(1).toReference(),
                ctx.sigm().arg(2).toReference(),
                ctx.sigm().arg(3).toReference(),
                ctx.sigm().SIGM().text[1] == 'p',
                ctx.sigm().SIGM().text.length > 2
        )
    }

    override fun exitStepEval(ctx: ODEParser.StepEvalContext) {
        expressionTree[ctx] = AbstractStep(
                ctx.step().NAME().text!!,
                ctx.step().arg(0).toReference(),
                ctx.step().arg(1).toReference(),
                ctx.step().arg(2).toReference(),
                ctx.step().STEP().text[1] == 'p'
        )
    }

    override fun exitHillEval(ctx: ODEParser.HillEvalContext) {
        expressionTree[ctx] = AbstractHill(
                ctx.hill().NAME().text!!,
                ctx.hill().arg(0).toReference(),
                ctx.hill().arg(1).toReference(),
                ctx.hill().arg(2).toReference(),
                ctx.hill().arg(3).toReference(),
                ctx.hill().HILL().text.endsWith("p")
        )
    }

    override fun exitNegativeEvaluable(ctx: ODEParser.NegativeEvaluableContext) {
        expressionTree[ctx] = Negation(expressionTree[ctx.eval()])
    }

    override fun exitParenthesis(ctx: ODEParser.ParenthesisContext) {
        expressionTree[ctx] = expressionTree[ctx.expr()]
    }

    override fun exitAddition(ctx: ODEParser.AdditionContext) {
        expressionTree[ctx] = Plus(expressionTree[ctx.expr(0)], expressionTree[ctx.expr(1)])
    }

    override fun exitSubtraction(ctx: ODEParser.SubtractionContext) {
        expressionTree[ctx] = Minus(expressionTree[ctx.expr(0)], expressionTree[ctx.expr(1)])
    }

    override fun exitMultiplication(ctx: ODEParser.MultiplicationContext) {
        expressionTree[ctx] = Times(expressionTree[ctx.expr(0)], expressionTree[ctx.expr(1)])
    }

    override fun exitEvaluable(ctx: ODEParser.EvaluableContext) {
        expressionTree[ctx] = expressionTree[ctx.eval()]
    }

}

// Helper classes that preserve important parts of parse tree between parsing and validation/conversion

private fun ODEParser.ArgContext.toReference(): Reference {
    if (NAME() != null) return Reference.Name(NAME().text!!)
    else return Reference.Number(NUMBER().text.toDouble())
}

private data class Negation(
        val expr: Evaluable
) : Evaluable {
    override fun eval(value: Double): Double = -1 * expr.eval(value)
}

private data class Plus(
        val e1: Evaluable,
        val e2: Evaluable
) : Evaluable {
    override fun eval(value: Double): Double = e1.eval(value) + e2.eval(value)
}

private data class Minus(
        val e1: Evaluable,
        val e2: Evaluable
) : Evaluable {
    override fun eval(value: Double): Double = e1.eval(value) - e2.eval(value)
}

private data class Times(
        val e1: Evaluable,
        val e2: Evaluable
) : Evaluable {
    override fun eval(value: Double): Double = e1.eval(value) * e2.eval(value)
}

private sealed class Reference : Evaluable {
    internal class Name(
            val value: String
    ) : Reference() {
        override fun eval(value: Double): Double = throw UnsupportedOperationException()
    }
    internal class Number(
            val value: Double
    ) : Reference() {
        override fun eval(value: Double): Double = value
    }
}

private abstract class AbstractFunction : Evaluable {
    override fun eval(value: Double): Double = throw UnsupportedOperationException()
}

private class AbstractHill(
        val name: String,
        val theta: Reference, val n: Reference, val a: Reference, val b: Reference,
        val positive: Boolean
) : AbstractFunction() {
    fun toHill(reader: ModelReader): Hill = Hill(
            varIndex = reader.resolveVarName(name),
            theta = reader.resolveArgument(theta),
            n = reader.resolveArgument(n),
            a = reader.resolveArgument(a),
            b = reader.resolveArgument(b),
            positive = positive
    )
}

private class AbstractRamp(
        val name: String,
        val lowThreshold: Reference, val highThreshold: Reference, val a: Reference, val b: Reference,
        val positive: Boolean,
        val coor: Boolean
) : AbstractFunction() {
    fun toRamp(reader: ModelReader): Ramp = Ramp(
            varIndex = reader.resolveVarName(name),
            lowThreshold = reader.resolveArgument(lowThreshold),
            highThreshold = reader.resolveArgument(highThreshold),
            a = if (coor) reader.resolveArgument(a) else reader.resolveArgument(lowThreshold) * reader.resolveArgument(a) + reader.resolveArgument(b),
            b = if (coor) reader.resolveArgument(b) else reader.resolveArgument(highThreshold) * reader.resolveArgument(a) + reader.resolveArgument(b),
            positive = positive
    )
}

private class AbstractSigmoid(
        val name: String,
        val k: Reference, val theta: Reference, val a: Reference, val b: Reference,
        val positive: Boolean,
        val inverse: Boolean
) : AbstractFunction() {
    fun toSigmoid(reader: ModelReader): Sigmoid = Sigmoid(
            varIndex = reader.resolveVarName(name),
            k = reader.resolveArgument(k),
            theta = if (!inverse)
                        reader.resolveArgument(theta)
                    else
                        reader.resolveArgument(theta) + Math.log(reader.resolveArgument(a)/reader.resolveArgument(b)) / 2*reader.resolveArgument(k),
            a = if (!inverse) reader.resolveArgument(a) else 1.0/reader.resolveArgument(a),
            b = if (!inverse) reader.resolveArgument(b) else 1.0/reader.resolveArgument(b),
            positive = positive
    )
}

private class AbstractStep(
        val name: String,
        val theta: Reference, val a: Reference, val b: Reference,
        val positive: Boolean
) : AbstractFunction() {
    fun toStep(reader: ModelReader): Step = Step(
            varIndex = reader.resolveVarName(name),
            theta = reader.resolveArgument(theta),
            a = reader.resolveArgument(a),
            b = reader.resolveArgument(b),
            positive = positive
    )
}
