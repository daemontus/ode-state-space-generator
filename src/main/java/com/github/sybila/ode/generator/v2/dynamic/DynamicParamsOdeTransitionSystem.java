package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.checker.Solver;
import com.github.sybila.ode.generator.NodeEncoder;
import com.github.sybila.ode.generator.rect.Rectangle;
import com.github.sybila.ode.generator.rect.RectangleSolver;
import com.github.sybila.ode.generator.v2.TransitionSystem;
import com.github.sybila.ode.model.Evaluable;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.OdeModel.Variable;
import com.github.sybila.ode.model.Summand;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    private Integer PositiveIn = 0;
    private Integer PositiveOut = 1;
    private Integer NegativeIn = 2;
    private Integer NegativeOut = 3;

    public DynamicParamsOdeTransitionSystem(OdeModel model) {
        this.model = model;
        encoder = new NodeEncoder(model);
        dimensions = model.getVariables().size();
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

            ColorComputer computer = null;
            try {
                ClassLoader loader = this.getClass().getClassLoader();
                Class colorComputer = loader.loadClass("com.github.sybila.ode.generator.v2.dynamic.ColorComputer");
                Constructor constructor = colorComputer.getConstructor(
                        OdeModel.class,
                        Solver.class,
                        NodeEncoder.class,
                        double[].class
                );

                computer = (ColorComputer) constructor.newInstance(new Object[] {model, solver, encoder, boundsRect});

            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

            Set<Rectangle> vertexColor = computer.getVertexColor(vertex, dimension, positiveDerivation);
            if (vertexColor != null) {
                colors = solver.or(colors, vertexColor);
            }

            //colors = colors | vertexColor;
        }

        solver.minimize(colors);
        facetColors.set(facetIndex, colors);
        return colors;
    }

    @NotNull
    @Override
    public Set<Rectangle> transitionParameters(@NotNull Integer source, @NotNull Integer target) {
        return edgeColours.getOrDefault(new Pair<>(source, target), solver.getFf());
    }
}
