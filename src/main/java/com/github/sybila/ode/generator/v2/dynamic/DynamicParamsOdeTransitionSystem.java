package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.checker.Solver;
import com.github.sybila.ode.generator.NodeEncoder;
import com.github.sybila.ode.generator.rect.Rectangle;
import com.github.sybila.ode.generator.rect.RectangleSolver;
import com.github.sybila.ode.generator.v2.ParamsOdeTransitionSystem;
import com.github.sybila.ode.generator.v2.TransitionSystem;
import com.github.sybila.ode.model.Evaluable;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.OdeModel.Variable;
import com.github.sybila.ode.model.Parser;
import com.github.sybila.ode.model.Summand;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;


@SuppressWarnings("Duplicates")
public class DynamicParamsOdeTransitionSystem implements TransitionSystem<Integer, Set<Rectangle>> {

    private final OdeModel model;
    private final NodeEncoder encoder;
    private final Integer dimensions;
    public Integer stateCount;
    private Boolean createSelfLoops;
    private List<Set<Rectangle>> facetColors;
    private Map<Variable, List<Integer>> masks = new HashMap<>();
    private Map<Variable, Integer> dependenceCheckMasks = new HashMap<>();
    private double[] boundsRect;
    public Solver<Set<Rectangle>> solver;
    private OnTheFlyColorComputer<Set<Rectangle>> colorComputer;
    private Path sourceCodePath;
    private Path project;

    private Integer PositiveIn = 0;
    private Integer PositiveOut = 1;
    private Integer NegativeIn = 2;
    private Integer NegativeOut = 3;

    private final String FULL_CLASS_PATH;

    /*
    private static final String CLASS_CODE = "import com.github.sybila.checker.Solver;\n" +
            "import com.github.sybila.ode.generator.rect.Rectangle;\n" +
            "import com.github.sybila.ode.model.OdeModel;\n" +
            "import com.github.sybila.ode.generator.v2.dynamic.OnTheFlyColorComputer;\n" +
            "import kotlin.Pair;\n" +
            "\n" +
            "import java.util.HashSet;\n" +
            "import java.util.Set;\n" +
            "\n" +
            "public class TestClass implements OnTheFlyColorComputer<Set<Rectangle>> {\n" +
            "    private OdeModel model;\n" +
            "    private Solver<Set<Rectangle>> solver;\n" +
            "    private double[] boundsRect;\n" +
            "\n" +
            "    @Override\n" +
            "    public void initialize(OdeModel model, Solver<Set<Rectangle>> solver) {\n" +
            "        System.out.println(\"Initializing...\");\n" +
            "        this.model = model;\n" +
            "        this.solver = solver;\n" +
            "        boundsRect = new double[2 * model.getParameters().size()];\n" +
            "        for (int i = 0; i < model.getParameters().size(); i++) {\n" +
            "            boundsRect[2 * i] = model.getParameters().get(i).getRange().getFirst();\n" +
            "            boundsRect[2 * i + 1] = model.getParameters().get(i).getRange().getSecond();\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public Set<Rectangle> getVertexColor(int vertex, int dimension, boolean positive) {\n" +
            "        System.out.println(\"Compute vertex color!\");\n" +
            "        Set<Rectangle> result = new HashSet<>();\n" +
            "        double derivationValue = 0.0;\n" +
            "        double denominator = 0.0;\n" +
            "        int parameterIndex = -1;\n" +
            "        if (parameterIndex == -1 || denominator == 0.0) {\n" +
            "            if ((positive && derivationValue > 0) || (!positive && derivationValue < 0)) {\n" +
            "                return solver.getTt();\n" +
            "            } else {\n" +
            "                return solver.getFf();\n" +
            "            }\n" +
            "        } else {\n" +
            "            // division by negative number flips the condition\n" +
            "            boolean newPositive = (denominator > 0) == positive;\n" +
            "            Pair<Double, Double> range = model.getParameters().get(parameterIndex).getRange();\n" +
            "            double split = Math.min(range.getSecond(), Math.max(range.getFirst(), -derivationValue / denominator));\n" +
            "            double newLow = newPositive ? split : range.getFirst();\n" +
            "            double newHigh = newPositive ? range.getSecond() : split;\n" +
            "\n" +
            "            if (newLow >= newHigh) {\n" +
            "                return null;\n" +
            "            } else {\n" +
            "                double[] r = boundsRect.clone();\n" +
            "                r[2 * parameterIndex] = newLow;\n" +
            "                r[2 * parameterIndex + 1] = newHigh;\n" +
            "                result.add(new Rectangle(r));\n" +
            "            }\n" +
            "        }\n" +
            "        return result;\n" +
            "    }\n" +
            "    \n" +
            "}";*/

    public DynamicParamsOdeTransitionSystem(OdeModel model, String fullClassPath) {
        this.model = model;
        encoder = new NodeEncoder(model);
        dimensions = model.getVariables().size();
        FULL_CLASS_PATH = fullClassPath;
        stateCount = getStateCount();
        createSelfLoops = true;

        facetColors = new ArrayList<>();
        for (int i = 0; i < stateCount * dimensions * 4; i++) {
            facetColors.add(null);
        }

        for (Variable var: model.getVariables()) {
            masks.put(var, new ArrayList<>());
            dependenceCheckMasks.put(var, getDependenceCheckMask(var));
        }

        //Iterates through all possible masks and all variables, filters out masks which are valid and saves them.
        for (int mask = 0; mask < Math.pow(2, dimensions); mask++) {
            for (Variable var: model.getVariables()) {
                if (checkMask(var, mask)) {
                    masks.get(var).add(mask);
                }
            }
        }

        boundsRect = new double[2 * model.getParameters().size()];
        for (int i = 0; i < model.getParameters().size(); i++) {
            boundsRect[2 * i] = model.getParameters().get(i).getRange().getFirst();
            boundsRect[2 * i + 1] = model.getParameters().get(i).getRange().getSecond();
        }

        solver = new RectangleSolver(new Rectangle(boundsRect));



        try {
            project = Files.createTempDirectory("on-the-fly");
            sourceCodePath = project.resolve("ColorComputer.java");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // delete .class file afterwards!
        }

    }

    /**
     * Calculates a set of variable indices which represents variables dependent on the input variable.
     * Then constructs a binary number, where 1 represents independent variable and 0 represents dependent variable,
     * indexed from the right, e.g., 100 means vars on indices 0 and 1 are dependent on the input var, whereas
     * var on index 2 is independent from the input var. Finally, returns this number as integer.
     *
     * @param var Variable
     * @return dependence-check mask as integer
     */
    private Integer getDependenceCheckMask(Variable var) {
        Set<Integer> dependentOn = new HashSet<>();
        for (Summand summand: var.getEquation()) {
            dependentOn.addAll(summand.getVariableIndices());
            for (Evaluable e: summand.getEvaluable()) {
                dependentOn.add(e.getVarIndex());
            }
        }

        BitSet result = new BitSet(model.getVariables().size());
        result.set(0, model.getVariables().size());
        for (Integer index: dependentOn) {
            result.clear(index);
        }

        int integerResult = 0;
        for(int i = 0 ; i < 32; i++)
            if (result.get(i)) {
                integerResult |= (1 << i);
            }
        return integerResult;
    }


    /**
     * Checks if the mask is valid for the given var, in other words, checks if the mask has zeroes on indices/bits
     * corresponding to variables which are independent from the given var.
     * Mask is valid <=> dependence-check mask (for the given var) & mask == 0.
     *
     * @param var variable to be checked
     * @param mask mask to be checked
     * @return true if mask is valid for the var, false otherwise
     */
    private boolean checkMask(Variable var, int mask) {
        return (dependenceCheckMasks.get(var) & mask) == 0;
    }

    private Integer getStateCount() {
        int result = 1;
        for (Variable var: model.getVariables()) {
            result = result * (var.getThresholds().size() - 1);
        }
        return result;
    }
    

    private Map<Integer, List<Integer>> successors = new HashMap<>();
    private Map<Integer, List<Integer>> predecessors= new HashMap<>();

    private Map<Pair<Integer, Integer>, Set<Rectangle>> edgeColours = new HashMap<>();

    @NotNull
    @Override
    public List<Integer> successors(@NotNull Integer from) {
        return successors.computeIfAbsent(from, f -> getStep(f, true));
    }

    @NotNull
    @Override
    public List<Integer> predecessors(@NotNull Integer from) {
       return predecessors.computeIfAbsent(from, f -> getStep(f, false));
    }


    private List<Integer> getStep(int from, Boolean successors) {
        List<Integer> result = new ArrayList<>();
        Set<Rectangle> selfLoop = solver.getTt();
        for (int dim = 0; dim < model.getVariables().size(); dim++) {

            //String dimName = model.getVariables().get(dim).getName();
            Set<Rectangle> positiveIn = getFacetColors(from, dim, PositiveIn);
            Set<Rectangle> positiveOut = getFacetColors(from, dim, PositiveOut);
            Set<Rectangle> negativeIn = getFacetColors(from, dim, NegativeIn);
            Set<Rectangle> negativeOut = getFacetColors(from, dim, NegativeOut);

            Integer higherNode = encoder.higherNode(from, dim);
            if (higherNode != null) {
                Set<Rectangle> colors = successors ? positiveOut : positiveIn;
                if (solver.isSat(colors)) {
                    result.add(higherNode);
                    if (successors) {
                        edgeColours.putIfAbsent(new Pair<>(from, higherNode), colors); // putIfAbsent?
                    } else {
                        edgeColours.putIfAbsent(new Pair<>(higherNode, from), colors);
                    }
                }

                if (createSelfLoops) {
                    Set<Rectangle> positiveFlow = solver.and(solver.and(negativeIn, positiveOut),
                            solver.not(solver.or(negativeOut, positiveIn)));
                    selfLoop = solver.and(selfLoop, solver.not(positiveFlow));

                    //boolean positiveFlow = negativeIn && positiveOut && !(negativeOut || positiveIn);
                    //selfLoop = selfLoop && !positiveFlow;
                }
            }

            Integer lowerNode = encoder.lowerNode(from, dim);
            if (lowerNode != null) {
                Set<Rectangle> colors = successors ? negativeOut : negativeIn;
                if (solver.isSat(colors)) {
                    result.add(lowerNode);
                    if (successors) {
                        edgeColours.putIfAbsent(new Pair<>(from, lowerNode), colors); // putIfAbsent?
                    } else {
                        edgeColours.putIfAbsent(new Pair<>(lowerNode, from), colors);
                    }
                }

                if (createSelfLoops) {
                    Set<Rectangle> negativeFlow = solver.and(solver.and(negativeOut, positiveIn),
                            solver.not(solver.or(negativeIn, positiveOut)));
                    selfLoop = solver.and(selfLoop, solver.not(negativeFlow));

                    //boolean negativeFlow = negativeOut && positiveIn && !(negativeIn || positiveOut);
                    //selfLoop = selfLoop && !negativeFlow;
                }
            }
        }

        if (solver.isSat(selfLoop)) {
            result.add(from);
            solver.minimize(selfLoop);
            edgeColours.putIfAbsent(new Pair<>(from, from), selfLoop);
        }

        return result;
    }


    private int facetIndex(int from, int dimension, int orientation) {
        return from + (stateCount * dimension) + (stateCount * dimensions * orientation);
    }

    private Set<Rectangle> getFacetColors(int from, int dimension, int orientation) {
        int facetIndex = facetIndex(from, dimension, orientation);
        Set<Rectangle> currentValue = facetColors.get(facetIndex);

        if (currentValue != null) {
            return currentValue;
        }

        // Indicates that we want to compute colors for the facet where the dimension threshold
        // is set to the "upper" value of this state.
        int positiveFacet = (orientation == PositiveIn || orientation == PositiveOut) ? 1 : 0;

        // Indicates that the derivative should be positive - entering from the bottom or exiting on the top.
        // Otherwise, the derivative has to be negative.
        boolean positiveDerivation = orientation == PositiveOut || orientation == NegativeIn;

        // Compute value
        Set<Rectangle> colors = solver.getFf();

        int dependencyMask = dependenceCheckMasks.get(model.getVariables().get(dimension));
        // if self dependent, dependency mask has 0 at "dimension" position
        boolean selfDependent = ((dependencyMask >> dimension) & 1) == 0;

        for (Integer mask: masks.get(model.getVariables().get(dimension))) {
            if (selfDependent && ((mask >> dimension) & 1) != positiveFacet) {
                continue;
            }

            int vertex = encoder.nodeVertex(from, mask);

            try {
                BufferedWriter writer = Files.newBufferedWriter(sourceCodePath);
                writer.write(generateFullClassCode(model.getVariables().get(dimension).getEquation()));
                writer.close();

                System.out.println("Temp file created: " + sourceCodePath);
                Process compiler = Runtime.getRuntime().exec(new String[]{ "javac", "-cp", FULL_CLASS_PATH, sourceCodePath.toAbsolutePath().toString() });
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(compiler.getErrorStream()));
                errorReader.lines().forEach(s -> System.err.println("CP: " + s));
                int resultCode = compiler.waitFor();
                System.out.println("Temp file compiled: " + resultCode);

                URL classUrl = project.toUri().toURL();
                System.out.println("Load dynamic from: " + classUrl);
                ClassLoader loader = new URLClassLoader(new URL[]{ classUrl });

                Class<?> dynamicClass = loader.loadClass("ColorComputer");

                colorComputer = (OnTheFlyColorComputer<Set<Rectangle>>) dynamicClass.getConstructor().newInstance();
                //colorComputer = (OnTheFlyColorComputer<Set<Rectangle>>) dynamicClass.newInstance();
                colorComputer.initialize(model, solver);

            /*OdeModel model = new Parser().parse(new File("models/tcbb.bio"));

            List<Summand> equation = model.getVariables().get(0).getEquation();
            System.out.println(prepareSummands(equation));
            for (int i=0; i < equation.size(); i++) {
                System.out.println("Summand "+i+": "+compileSummand(equation.get(i), i));
            }*/

            } catch (IOException | IllegalAccessException | InstantiationException | ClassNotFoundException | InterruptedException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            } finally {
                // delete .class file afterwards!
            }



            Set<Rectangle> vertexColor = colorComputer.getVertexColor(vertex, dimension, positiveDerivation);
            if (vertexColor != null) {
                colors = solver.or(colors, vertexColor);
            }

            //colors = colors | vertexColor;
        }

        solver.minimize(colors);
        facetColors.set(facetIndex, colors);

        if (orientation == PositiveIn || orientation == PositiveOut) {
            Integer higherNode = encoder.higherNode(from, dimension);
            if (higherNode != null) {
                int dual = orientation == PositiveIn ? NegativeOut : NegativeIn;
                facetColors.set(facetIndex(higherNode, dimension, dual), colors);
            }
        } else {
            Integer lowerNode = encoder.lowerNode(from, dimension);
            if (lowerNode != null) {
                int dual = orientation == NegativeIn ? PositiveOut : PositiveIn;
                facetColors.set(facetIndex(lowerNode, dimension, dual), colors);
            }
        }

        return colors;
    }

    @NotNull
    @Override
    public Set<Rectangle> transitionParameters(@NotNull Integer source, @NotNull Integer target) {
        return edgeColours.getOrDefault(new Pair<>(source, target), solver.getFf());
    }

    private static String generateFullClassCode(List<Summand> equation) {
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
                "        System.out.println(\"Initializing...\");\n" +
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
                "    @Override\n" +
                "    public Set<Rectangle> getVertexColor(int vertex, int dimension, boolean positive) {\n" +
                "        System.out.println(\"Compute vertex color!\");\n" +
                "        Set<Rectangle> result = new HashSet<>();\n" +
                "        List<Summand> equation = model.getVariables().get(dimension).getEquation();\n" +
                "        double derivationValue = 0.0;\n" +
                "        double denominator = 0.0;\n" +
                "        int parameterIndex = -1;\n" +
                prepareSummands(equation) +
                compileDerivationValueAndDenominator(equation) +
                "        if (parameterIndex == -1 || denominator == 0.0) {\n" +
                "            if ((positive && derivationValue > 0) || (!positive && derivationValue < 0)) {\n" +
                "                return solver.getTt();\n" +
                "            } else {\n" +
                "                return solver.getFf();\n" +
                "            }\n" +
                "        } else {\n" +
                "            // division by negative number flips the condition\n" +
                "            boolean newPositive = (denominator > 0) == positive;\n" +
                "            Pair<Double, Double> range = model.getParameters().get(parameterIndex).getRange();\n" +
                "            double split = Math.min(range.getSecond(), Math.max(range.getFirst(), -derivationValue / denominator));\n" +
                "            double newLow = newPositive ? split : range.getFirst();\n" +
                "            double newHigh = newPositive ? range.getSecond() : split;\n" +
                "\n" +
                "            if (newLow >= newHigh) {\n" +
                "                return null;\n" +
                "            } else {\n" +
                "                double[] r = boundsRect.clone();\n" +
                "                r[2 * parameterIndex] = newLow;\n" +
                "                r[2 * parameterIndex + 1] = newHigh;\n" +
                "                result.add(new Rectangle(r));\n" +
                "            }\n" +
                "        }\n" +
                "        return result;\n" +
                "    }\n" +
                "    \n" +
                "   private double varValue(int vertex, int var) {\n" +
                "       return model.getVariables().get(var).getThresholds().get(encoder.vertexCoordinate(vertex, var));\n" +
                "   }\n" +
                "}";
    }

    private static String prepareSummands(List<Summand> equation) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i<equation.size(); i++) {
            result.append("Summand summand")
                    .append(i)
                    .append(" = equation.get(")
                    .append(i)
                    .append(");\n");
        }
        return result.toString();
    }

    private static String compileSummand(Summand summand, int summandIndex) {
        StringBuilder result = new StringBuilder();
        for (int v : summand.getVariableIndices()) {
            result.append("varValue(vertex, ")
                    .append(v)
                    .append(") * ");
        }

        List<Evaluable> evaluable = summand.getEvaluable();
        for (int i = 0; i < evaluable.size(); i++) {
            Evaluable eval = evaluable.get(i);
            result.append("summand")
                    .append(summandIndex)
                    .append(".getEvaluable(")
                    .append(i)
                    .append(").invoke(getValue(vertex, ")
                    .append(eval.getVarIndex())
                    .append(")) * ");
        }

        result.append(summand.getConstant());
        return result.toString();
    }

    private static String compileDerivationValueAndDenominator(List<Summand> equation) {
        StringBuilder result = new StringBuilder();
        StringBuilder derivationValue = new StringBuilder();
        StringBuilder denominator = new StringBuilder();

        int parameterIndex = -1;

        for (int i = 0; i < equation.size(); i++) {
            if (equation.get(i).hasParam()) {
                parameterIndex = equation.get(i).getParamIndex();
                denominator.append(compileSummand(equation.get(i), i))
                        .append(" + ");
            } else {
                derivationValue.append(compileSummand(equation.get(i), i))
                        .append(" + ");
            }
        }

        if (derivationValue.length() > 0) {
            // getting rid of " + " at the end
            derivationValue.setLength(derivationValue.length() - 3);
            result.append("derivationValue = ")
                    .append(derivationValue)
                    .append(";\n");
        }

        if (denominator.length() > 0) {
            // getting rid of " + " at the end
            denominator.setLength(denominator.length() - 3);
            result.append("denominator = ")
                    .append(denominator)
                    .append(";\n")
                    .append("parameterIndex = ")
                    .append(parameterIndex)
                    .append(";\n");
        }

        return result.toString();
    }

    public static void main(String[] args) {
        /*ParamsOdeTransitionSystem transitionSystem = new ParamsOdeTransitionSystem(new Parser()
                .parse(new File("models/tcbb.bio")));
        System.out.println(generateFullClassCode(transitionSystem.model.getVariables().get(0).getEquation()));
        */
    }
}
