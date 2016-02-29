package cz.muni.fi.ode.model

import com.github.sybila.ctl.set
import cz.muni.fi.ode.antlr.ODEBaseListener
import cz.muni.fi.ode.antlr.ODELexer
import cz.muni.fi.ode.antlr.ODEParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File
import java.util.*

class Parser {

    fun parse(input: String): Model
            = processStream(ANTLRInputStream(input.toCharArray(), input.length))

    fun parse(input: File): Model
            = input.inputStream().use { processStream(ANTLRInputStream(it)) }

    private fun processStream(input: ANTLRInputStream): Model {
        val root = ODEParser(CommonTokenStream(ODELexer(input))).root()
        val reader = ModelReader()
        ParseTreeWalker().walk(reader, root)
        return reader.toModel()
    }

}

private class ModelReader : ODEBaseListener() {

    private val equations = HashMap<String, Resolvable>()
    private val thresholds = HashMap<String, List<Double>>()
    private val constants = HashMap<String, Double>()
    private val initial = HashMap<String, Pair<Double, Double>>()
    private val varPoints = HashMap<String, Pair<Int, Int>>()
    private var parameters = ArrayList<Model.Parameter>()    //preserve ordering
    private var parameterNames = ArrayList<String>()    //preserve ordering
    private var variables = ArrayList<String>()

    private val expressionTree = ParseTreeProperty<Resolvable>()

    /**
     * Conversion to standard model, with various integrity checks.
     */
    fun toModel(): Model {
        assert(parameters.map { it.name } == parameterNames)

        //basic integrity checks
        if (variables.isEmpty()) {
            throw IllegalStateException("Model has no variables!")
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
                    val threshold = thresholds[vName] ?: throw IllegalStateException("No thresholds for variable $vName")
                    val equation = equations[vName] ?: throw IllegalStateException("No equation for variable $vName")
                    Model.Variable(
                            name = vName,
                            range = Pair(threshold.first(), threshold.last()),
                            thresholds = threshold,
                            varPoints = varPoints[vName],
                            equation = flattenAndResolve(equation)
                    )
                },
                parameters
        )
    }

    private fun flattenAndResolve(target: Resolvable): List<Summand> = when (target) {
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
        is Plus -> simplifySummands(flattenAndResolve(target.e1) + flattenAndResolve(target.e2))
        is Negation -> simplifySummands(flattenAndResolve(target.expr).map { it.copy(constant = -1.0 * it.constant) })
        is Times -> {
            val s1 = flattenAndResolve(target.e1)
            val s2 = flattenAndResolve(target.e2)
            simplifySummands(s1.flatMap { i1 -> s2.map { i2 -> i1 * i2 } })
        }
        is Minus -> simplifySummands(flattenAndResolve(target.e1) + flattenAndResolve(Negation(target.e2)))
        else -> throw IllegalStateException("Can't flatten or resolve: $target")    //can't happen
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
        if (index < 0) {
            throw IllegalStateException("Undefined variable: $name")
        }
        return index
    }

    internal fun simplifySummands(source: List<Summand>): List<Summand> {

        val list = source.toMutableList()

        fun findAndMerge(): Boolean {
            for (fst in list.indices) {
                for (snd in list.indices) {
                    if (fst == snd) continue
                    val sum = list[fst] + list[snd]
                    if (sum != null) {
                        list.removeAll(listOf(fst, snd).map { list[it] })
                        list.add(sum)
                        return true
                    }
                }
            }
            return false
        }

        while (findAndMerge()) {}

        return list.toList()
    }

    /*
        Parsing basic model properties: thresholds, constants, initial states, var points, parameters, variable names...
     */

    override fun exitVarName(ctx: ODEParser.VarNameContext) {
        val value = ctx.NAME().text
        if (value in variables) {
            throw IllegalStateException("Redefinition of variable $value")
        }
        variables.add(value)
    }

    override fun exitParamInterval(ctx: ODEParser.ParamIntervalContext) {
        val name = ctx.NAME().text
        if (name in parameterNames) {
            throw IllegalStateException("Redefinition of parameter $name")
        }
        parameters.add(Model.Parameter(name,
                Pair(ctx.NUMBER(0).text.toDouble(), ctx.NUMBER(1).text.toDouble())
        ))
        parameterNames.add(name)
    }

    override fun exitConstant(ctx: ODEParser.ConstantContext) {
        if (constants.put(ctx.NAME().text, ctx.NUMBER().text.toDouble()) != null) {
            throw IllegalStateException("Redefinition of constant ${ctx.NAME().text}")
        }
    }

    override fun exitInitInterval(ctx: ODEParser.InitIntervalContext) {
        if (initial.put(ctx.NAME().text, Pair(ctx.NUMBER(0).text.toDouble(), ctx.NUMBER(1).text.toDouble())) != null) {
            throw IllegalStateException("Redefinition of initial interval for ${ctx.NAME().text}")
        }
    }

    override fun exitVarInterval(ctx: ODEParser.VarIntervalContext) {
        if (varPoints.put(ctx.NAME().text, Pair(ctx.NUMBER(0).text.toInt(), ctx.NUMBER(1).text.toInt())) != null) {
            throw IllegalStateException("Redefinition of var points for ${ctx.NAME().text}")
        }
    }

    override fun exitThresholds(ctx: ODEParser.ThresholdsContext) {
        if (thresholds.put(ctx.NAME().text, ctx.NUMBER().map { it.text.toDouble() }) != null) {
            throw IllegalStateException("Redefinition of thresholds for ${ctx.NAME().text}")
        }
    }

    /*
        Equation parsing
     */

    override fun exitEquation(ctx: ODEParser.EquationContext) {
        if (equations.put(ctx.NAME().text, expressionTree[ctx.expr()]) != null) {
            throw IllegalStateException("Redefinition of equation for ${ctx.NAME().text}")
        }
    }

    override fun exitNumberEval(ctx: ODEParser.NumberEvalContext) {
        expressionTree[ctx] = Reference.Number(ctx.NUMBER().text.toDouble())
    }

    override fun exitNameEval(ctx: ODEParser.NameEvalContext) {
        expressionTree[ctx] = Reference.Name(ctx.NAME().text)
    }

    override fun exitRampEval(ctx: ODEParser.RampEvalContext) {
        expressionTree[ctx] = AbstractRamp(
                ctx.ramp().NAME().text,
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
                ctx.sigm().NAME().text,
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
                ctx.step().NAME().text,
                ctx.step().arg(0).toReference(),
                ctx.step().arg(1).toReference(),
                ctx.step().arg(2).toReference(),
                ctx.step().STEP().text[1] == 'p'
        )
    }

    override fun exitHillEval(ctx: ODEParser.HillEvalContext) {
        expressionTree[ctx] = AbstractHill(
                ctx.hill().NAME().text,
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
    if (NAME() != null) return Reference.Name(NAME().text)
    else return Reference.Number(NUMBER().text.toDouble())
}

//Represents one part of expression that needs to be processed before it becomes evaluable
private interface Resolvable

private class Negation(
        val expr: Resolvable
) : Resolvable

private class Plus(
        val e1: Resolvable,
        val e2: Resolvable
) : Resolvable

private class Minus(
        val e1: Resolvable,
        val e2: Resolvable
) : Resolvable

private class Times(
        val e1: Resolvable,
        val e2: Resolvable
) : Resolvable

private sealed class Reference : Resolvable {
    internal class Name(
            val value: String
    ) : Reference()
    internal class Number(
            val value: Double
    ) : Reference()
}

private class AbstractHill(
        private val name: String,
        private val theta: Reference,
        private val n: Reference,
        private val a: Reference,
        private val b: Reference,
        private val positive: Boolean
) : Resolvable {
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
        private val name: String,
        private val lowThreshold: Reference,
        private val highThreshold: Reference,
        private val a: Reference,
        private val b: Reference,
        private val positive: Boolean,
        private val coordinate: Boolean
) : Resolvable {
    fun toRamp(reader: ModelReader): Ramp {
        val a = reader.resolveArgument(a)
        val b = reader.resolveArgument(b)
        val lowThreshold = reader.resolveArgument(lowThreshold)
        val highThreshold = reader.resolveArgument(highThreshold)
        return when {
            positive && coordinate -> Ramp.positiveCoordinate(reader.resolveVarName(name), lowThreshold, highThreshold, a, b)
            !positive && coordinate -> Ramp.negativeCoordinate(reader.resolveVarName(name), lowThreshold, highThreshold, a, b)
            positive && !coordinate -> Ramp.positive(reader.resolveVarName(name), lowThreshold, highThreshold, a, b)
            else -> Ramp.negative(reader.resolveVarName(name), lowThreshold, highThreshold, a, b)
        }
    }
}

private class AbstractSigmoid(
        private val name: String,
        private val k: Reference,
        private val theta: Reference,
        private val a: Reference,
        private val b: Reference,
        private val positive: Boolean,
        private val inverse: Boolean
) : Resolvable {
    fun toSigmoid(reader: ModelReader): Sigmoid {
        val a = reader.resolveArgument(a)
        val b = reader.resolveArgument(b)
        val k = reader.resolveArgument(k)
        val theta = reader.resolveArgument(theta)
        return when {
            positive && inverse -> Sigmoid.positiveInverse(reader.resolveVarName(name), k, theta, a, b)
            !positive && inverse -> Sigmoid.negativeInverse(reader.resolveVarName(name), k, theta, a, b)
            positive && !inverse -> Sigmoid.positive(reader.resolveVarName(name), k, theta, a, b)
            else -> Sigmoid.negative(reader.resolveVarName(name), k, theta, a, b)
        }
    }
}

private class AbstractStep(
        private val name: String,
        private val theta: Reference,
        private val a: Reference,
        private val b: Reference,
        private val positive: Boolean
) : Resolvable {
    fun toStep(reader: ModelReader): Step = Step(
            varIndex = reader.resolveVarName(name),
            theta = reader.resolveArgument(theta),
            a = reader.resolveArgument(a),
            b = reader.resolveArgument(b),
            positive = positive
    )
}
