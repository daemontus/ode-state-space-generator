package com.github.sybila.ode.generator.v2.dynamic
import com.github.sybila.checker.Solver
import com.github.sybila.checker.Transition
import com.github.sybila.checker.decreaseProp
import com.github.sybila.checker.increaseProp
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.ode.generator.NodeEncoder
import com.github.sybila.ode.generator.rect.Rectangle
import com.github.sybila.ode.generator.rect.RectangleSolver
import com.github.sybila.ode.generator.v2.TransitionSystem
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.OdeModel.*
import com.github.sybila.ode.model.Summand
import com.sun.org.apache.xpath.internal.operations.Bool
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.pow


class KotlinDynamicParamsOdeTransitionSystem(
        protected val model: OdeModel,
        private val createSelfLoops: Boolean,
        private val fullClassPath: String
) : TransitionSystem<Int, MutableSet<Rectangle>>,
        Solver<MutableSet<Rectangle>> by RectangleSolver(Rectangle(model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray())) {

    protected val encoder = NodeEncoder(model)
    protected val dimensions = model.variables.size
    private var colorComputer : OnTheFlyColorComputer<MutableSet<Rectangle>>? = null
    private var solver : Solver<MutableSet<Rectangle>>? = null


    private val masks = HashMap<Variable, MutableList<Int>>()
    private val dependenceCheckMasks = HashMap<Variable, Int>()

    val stateCount: Int = model.variables.fold(1) { a, v ->
        a * (v.thresholds.size - 1)
    }

    init {
        for (v in model.variables) {
            masks[v] = mutableListOf()
            dependenceCheckMasks[v] = getDependenceCheckMask(v)
        }

        for (mask in 0 until 2.toDouble().pow(dimensions).toInt()) {
            for (v in model.variables) {
                if (checkMask(v, mask)) {
                    masks[v]?.add(mask)
                }
            }
        }

        solver = RectangleSolver(Rectangle(model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray()))
        compileAndLoadClass(generateFullClassCode())
    }

    private fun generateFullClassCode(): String {
        return "import com.github.sybila.checker.Solver;\n" +
                "import com.github.sybila.ode.generator.rect.Rectangle;\n" +
                "import com.github.sybila.ode.generator.NodeEncoder;\n" +
                "import com.github.sybila.ode.generator.v2.dynamic.OnTheFlyColorComputer;\n" +
                "import com.github.sybila.ode.model.OdeModel;\n" +
                "import com.github.sybila.ode.model.Summand;\n" +
                "import kotlin.Pair;\n" +
                "\n" +
                "import java.util.HashSet;\n" +
                "import java.util.Set;\n" +
                "import java.util.List;\n" +
                "\n" +
                "public class ColorComputer implements OnTheFlyColorComputer<Set<Rectangle>> {\n" +
                "    private OdeModel model;\n" +
                "    private Solver<Set<Rectangle>> solver;\n" +
                "    private double[] boundsRect;\n" +
                "    private NodeEncoder encoder;\n" +
                "\n" +
                "    @Override\n" +
                "    public void initialize(OdeModel model, Solver<Set<Rectangle>> solver) {\n" +
                "        this.model = model;\n" +
                "        this.solver = solver;\n" +
                "        this.encoder = new NodeEncoder(model);\n" +
                "        boundsRect = new double[2 * model.getParameters().size()];\n" +
                "        for (int i = 0; i < model.getParameters().size(); i++) {\n" +
                "            boundsRect[2 * i] = model.getParameters().get(i).getRange().getFirst();\n" +
                "            boundsRect[2 * i + 1] = model.getParameters().get(i).getRange().getSecond();\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                generateMainGetVertexColor() +
                generateGetVertexColor() +
                generateGetResult() +
                "   private double varValue(int vertex, int var) {\n" +
                "       return model.getVariables().get(var).getThresholds().get(encoder.vertexCoordinate(vertex, var));\n" +
                "   }\n" +
                "}"
    }

    private fun generateGetResult(): String {
        val result = StringBuilder()
        result.append("private Set<Rectangle> getResult(double derivationValue, double denominator, int parameterIndex, boolean positive) {\n")
                .append("Set<Rectangle> result = new HashSet<>();\n")
                .append("if (parameterIndex == -1 || denominator == 0.0) {\n")
                .append("            if ((positive && derivationValue > 0) || (!positive && derivationValue < 0)) {\n")
                .append("                return solver.getTt();\n")
                .append("            } else {\n")
                .append("                return solver.getFf();\n")
                .append("            }\n")
                .append("        } else {\n")
                .append("            boolean newPositive = (denominator > 0) == positive;\n")
                .append("            Pair<Double, Double> range = model.getParameters().get(parameterIndex).getRange();\n")
                .append("            double split = Math.min(range.getSecond(), Math.max(range.getFirst(), -derivationValue / denominator));\n")
                .append("            double newLow = newPositive ? split : range.getFirst();\n")
                .append("            double newHigh = newPositive ? range.getSecond() : split;\n")
                .append("            if (newLow >= newHigh) {\n")
                .append("                return null;\n")
                .append("            } else {\n")
                .append("                double[] r = boundsRect.clone();\n")
                .append("                r[2 * parameterIndex] = newLow;\n")
                .append("                r[2 * parameterIndex + 1] = newHigh;\n")
                .append("                result.add(new Rectangle(r));\n")
                .append("            }\n")
                .append("        }\n")
                .append("        return result;\n")
                .append("    }\n")

        return result.toString()
    }

    private fun generateGetVertexColor(): String {
        val result = StringBuilder()

        for (i in 0 until dimensions) {
            result.append("private Set<Rectangle> getVertexColor")
                    .append(i)
                    .append("(int vertex, boolean positive) {\n")
                    .append("List<Summand> equation = model.getVariables().get(\n")
                    .append(i)
                    .append(").getEquation();\n")
                    .append("double derivationValue = 0.0;\n")
                    .append("double denominator = 0.0;\n")
                    .append("int parameterIndex = -1;\n")
                    .append(prepareSummands(model.variables[i].equation))
                    .append(compileDerivationValueAndDenominator(model.variables[i].equation))
                    .append("return getResult(derivationValue, denominator, parameterIndex, positive);\n")
                    .append("    }\n")
        }

        return result.toString()
    }

    private fun compileDerivationValueAndDenominator(equation: List<Summand>): String {
        val result = StringBuilder()
        val derivationValue = StringBuilder()
        val denominator = StringBuilder()

        var parameterIndex = -1

        for (i in equation.indices) {
            if (equation[i].hasParam()) {
                parameterIndex = equation[i].paramIndex
                denominator.append(compileSummand(equation[i], i))
                        .append(" + ")
            } else {
                derivationValue.append(compileSummand(equation[i], i))
                        .append(" + ")
            }
        }

        if (derivationValue.length > 0) {
            derivationValue.setLength(derivationValue.length - 3) // getting rid of " + " at the end
            result.append("derivationValue = ")
                    .append(derivationValue)
                    .append(";\n")
        }

        if (denominator.length > 0) {
            denominator.setLength(denominator.length - 3) // getting rid of " + " at the end
            result.append("denominator = ")
                    .append(denominator)
                    .append(";\n")
                    .append("parameterIndex = ")
                    .append(parameterIndex)
                    .append(";\n")
        }

        return result.toString()
    }

    private fun compileSummand(summand: Summand, summandIndex: Int): String {
        val result = StringBuilder()
        for (v in summand.variableIndices) {
            result.append("varValue(vertex, ")
                    .append(v)
                    .append(") * ")
        }

        val evaluable = summand.evaluable
        for (i in evaluable.indices) {
            val eval = evaluable[i]
            result.append("summand")
                    .append(summandIndex)
                    .append(".getEvaluable(")
                    .append(i)
                    .append(").invoke(varValue(vertex, ") // getValue typo??
                    .append(eval.varIndex)
                    .append(")) * ")
        }

        result.append(summand.constant)
        return result.toString()
    }

    private fun prepareSummands(equation: List<Summand>): String {
        val result = StringBuilder()
        for (i in equation.indices) {
            result.append("Summand summand")
                    .append(i)
                    .append(" = equation.get(")
                    .append(i)
                    .append(");\n")
        }
        return result.toString()
    }

    private fun generateMainGetVertexColor(): String {
        val result = StringBuilder()
        result.append("@Override\n")
                .append("public Set<Rectangle> getVertexColor(int vertex, int dimension, boolean positive) {\n")
        for (i in 0 until dimensions) {
            result.append("if (dimension == ")
                    .append(i)
                    .append(") return getVertexColor")
                    .append(i)
                    .append("(vertex, positive);\n")
        }
        result.append("return new HashSet<Rectangle>();\n") // should never happen
                .append("}\n")

        return result.toString()
    }

    private fun compileAndLoadClass(fullClassCode: String) {
        try {
            val project = Files.createTempDirectory("on-the-fly")
            val sourceCodePath = project.resolve("ColorComputer.java")
            val writer = Files.newBufferedWriter(sourceCodePath)
            writer.write(fullClassCode)
            writer.close()

            val compiler = Runtime.getRuntime().exec(arrayOf("javac", "-cp", fullClassPath, sourceCodePath.toAbsolutePath().toString()))
            val resultCode = compiler.waitFor()
            val classUrl = project.toUri().toURL()

            val loader = URLClassLoader(arrayOf(classUrl))
            val dynamicClass = loader.loadClass("ColorComputer")
            colorComputer = dynamicClass.getConstructor().newInstance() as OnTheFlyColorComputer<MutableSet<Rectangle>>?
            colorComputer?.initialize(model, solver)

        } finally {

        }
    }

    private fun getDependenceCheckMask(v: Variable) : Int {
        val dependentOn = mutableSetOf<Int>()
        for (summand in v.equation) {
            dependentOn.addAll(summand.variableIndices)
            for (evaluable in summand.evaluable) {
                dependentOn.add(evaluable.varIndex)
            }
        }

        val result = BitSet(model.variables.size)
        result.set(0, model.variables.size)
        for (index in dependentOn) {
            result.clear(index)
        }


        var intResult = 0
        for (i in 0 until 32) {
            if (result.get(i)) {
                intResult = intResult or (1 shl i)
            }
        }
        return intResult
    }

    private fun checkMask(v: Variable, mask: Int) : Boolean {
        return (dependenceCheckMasks[v]?.and(mask)) == 0
    }

    private val facetColors = arrayOfNulls<Any>(stateCount * dimensions * 4)

    private val PositiveIn = 0
    private val PositiveOut = 1
    private val NegativeIn = 2
    private val NegativeOut = 3


    private fun facetIndex(from: Int, dimension: Int, orientation: Int)
            = from + (stateCount * dimension) + (stateCount * dimensions * orientation)

    private fun getFacetColors(from: Int, dimension: Int, orientation: Int): MutableSet<Rectangle> {
        val index = facetIndex(from, dimension, orientation)
        val value = facetColors[index] ?: run {
            //iterate over vertices
            val positiveFacet = if (orientation == PositiveIn || orientation == PositiveOut) 1 else 0
            val positiveDerivation = orientation == PositiveOut || orientation == NegativeIn


            val dependencyMask = dependenceCheckMasks[model.variables[dimension]]
            val selfDependent = ((dependencyMask?.shr(dimension))?.and(1)) == 0

            val vertexMasks = masks[model.variables[dimension]]


            val colors = vertexMasks
                    ?.filter { !selfDependent || it.shr(dimension).and(1) == positiveFacet }
                    ?.fold(ff) { a, mask ->
                        val vertex = encoder.nodeVertex(from, mask)
                        colorComputer?.getVertexColor(vertex, dimension, positiveDerivation)?.let { a or it } ?: a
                    }


            colors?.minimize()

            facetColors[index] = colors

            //also update dual facet
            if (orientation == PositiveIn || orientation == PositiveOut) {
                encoder.higherNode(from, dimension)?.let { higher ->
                    val dual = if (orientation == PositiveIn) {
                        NegativeOut
                    } else { NegativeIn }
                    facetColors[facetIndex(higher, dimension, dual)] = colors
                }
            } else {
                encoder.lowerNode(from, dimension)?.let { lower ->
                    val dual = if (orientation == NegativeIn) {
                        PositiveOut
                    } else {
                        PositiveIn
                    }
                    facetColors[facetIndex(lower, dimension, dual)] = colors
                }
            }

            colors
        }

        return value as MutableSet<Rectangle>
    }


    private val successorCache = HashMap<Int, List<Int>>(stateCount)
    private val predecessorCache = HashMap<Int, List<Int>>(stateCount)

    override fun Int.predecessors(): List<Int>
            = getStep(this, false)

    override fun Int.successors(): List<Int>
            = getStep(this, true)

    private fun getStep(from: Int, successors: Boolean): List<Int> {
        return (when {
            successors -> successorCache
            else -> predecessorCache
        }).computeIfAbsent(from) {
            val result = ArrayList<Int>()
            //selfLoop <=> !positiveFlow && !negativeFlow <=> !(positiveFlow || negativeFlow)
            //positiveFlow = (-in && +out) && !(-out || +In) <=> -in && +out && !-out && !+In
            var selfloop = tt
            for (dim in model.variables.indices) {

                val positiveOut = getFacetColors(from, dim, PositiveOut)
                val positiveIn = getFacetColors(from, dim, PositiveIn)
                val negativeOut = getFacetColors(from, dim, NegativeOut)
                val negativeIn = getFacetColors(from, dim, NegativeIn)

                encoder.higherNode(from, dim)?.let { higher ->
                    val colors = (if (successors) positiveOut else positiveIn)
                    if (colors.isSat()) {
                        result.add(higher)
                        if (successors) edgeColours.putIfAbsent(Pair(from, higher), colors)
                        else edgeColours.putIfAbsent(Pair(higher, from), colors)
                    }

                    if (createSelfLoops) {
                        val positiveFlow = negativeIn and positiveOut and (negativeOut or positiveIn).not()
                        selfloop = selfloop and positiveFlow.not()
                    }
                }

                encoder.lowerNode(from, dim)?.let { lower ->
                    val colors = (if (successors) negativeOut else negativeIn)
                    if (colors.isSat()) {
                        result.add(lower)
                        if (successors) edgeColours.putIfAbsent(Pair(from, lower), colors)
                        else edgeColours.putIfAbsent(Pair(lower, from), colors)
                    }

                    if (createSelfLoops) {
                        val negativeFlow = negativeOut and positiveIn and (negativeIn or positiveOut).not()
                        selfloop = selfloop and negativeFlow.not()
                    }
                }

            }

            if (selfloop.isSat()) {
                selfloop.minimize()
                result.add(from)
                edgeColours.putIfAbsent(Pair(from, from), selfloop)
            }
            result
        }
    }

    private val edgeColours: HashMap<Pair<Int, Int>, MutableSet<Rectangle>> = hashMapOf()

    override fun transitionParameters(source: Int, target: Int): MutableSet<Rectangle> {
        return edgeColours.getOrDefault(Pair(source, target), ff)
    }

}