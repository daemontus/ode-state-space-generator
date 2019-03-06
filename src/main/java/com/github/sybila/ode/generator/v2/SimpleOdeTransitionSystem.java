package com.github.sybila.ode.generator.v2;

import com.github.sybila.ode.generator.NodeEncoder;
import com.github.sybila.ode.model.Evaluable;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.OdeModel.Variable;
import com.github.sybila.ode.model.Parser;
import com.github.sybila.ode.model.Summand;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SimpleOdeTransitionSystem implements TransitionSystem<Integer, Boolean> {

    private final OdeModel model;
    private final NodeEncoder encoder;
    private final Integer dimensions;
    private Integer stateCount;
    private List<Boolean> facetColors;
    private List<List<Summand>> equations = new ArrayList<>();
    private List<List<Double>> thresholds = new ArrayList<>();
    private Map<Variable, List<Integer>> masks = new HashMap<>();
    private Map<Variable, Integer> dependenceCheckMasks = new HashMap<>();

    private Integer PositiveIn = 0;
    private Integer PositiveOut = 1;
    private Integer NegativeIn = 2;
    private Integer NegativeOut = 3;

    private Boolean createSelfLoops;

    public SimpleOdeTransitionSystem(OdeModel model) {
        this.model = model;
        this.encoder = new NodeEncoder(model);
        this.dimensions = model.getVariables().size();
        this.stateCount = getStateCount();
        this.facetColors = new ArrayList<>();

        for (int i = 0; i < stateCount * dimensions * 4; i++) {
            this.facetColors.add(null);
        }

        for (Variable var: model.getVariables()) {
            this.equations.add(var.getEquation());
            this.thresholds.add(var.getThresholds());
            this.masks.put(var, new ArrayList<>());
            this.dependenceCheckMasks.put(var, getDependenceCheckMask(var));
        }

        createSelfLoops = true;

        for (int mask = 0; mask < Math.pow(2, dimensions); mask++) {
            for (Variable var: model.getVariables()) {
                if (checkMask(var, mask)) {
                    masks.get(var).add(mask);
                }
            }
        }
    }

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
            result.clear(model.getVariables().size() - 1 - index);
        }

        int integerResult = 0;
        for(int i = 0 ; i < 32; i++)
            if (result.get(i)) {
                integerResult |= (1 << i);
            }
        return integerResult;
    }

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

    @NotNull
    @Override
    public List<Integer> successors(@NotNull Integer from) {
        return getStep(from, true);
    }

    @NotNull
    @Override
    public List<Integer> predecessors(@NotNull Integer from) {
       return getStep(from, false);
    }


    private List<Integer> getStep(int from, Boolean successors) {
        List<Integer> result = new ArrayList<>();
        boolean selfLoop = true;
        for (int dim = 0; dim < model.getVariables().size(); dim++) {

            boolean positiveIn = getFacetColors(from, dim, PositiveIn);
            boolean negativeIn = getFacetColors(from, dim, NegativeIn);
            boolean positiveOut = getFacetColors(from, dim, PositiveOut);
            boolean negativeOut = getFacetColors(from, dim, NegativeOut);

            Integer higherNode = encoder.higherNode(from, dim);
            if (higherNode != null) {
                boolean colors = successors ? positiveOut : positiveIn;
                if (colors) {
                    result.add(higherNode);
                }

                if (createSelfLoops) {
                    boolean positiveFlow = negativeIn && positiveOut && !(negativeOut || positiveIn);
                    selfLoop = selfLoop && !positiveFlow;
                }
            }

            Integer lowerNode = encoder.lowerNode(from, dim);
            if (lowerNode != null) {
                boolean colors = successors ? negativeOut : negativeIn;
                if (colors) {
                    result.add(lowerNode);
                }

                if (createSelfLoops) {
                    boolean negativeFlow = negativeOut && positiveIn && !(negativeIn || positiveOut);
                    selfLoop = selfLoop && !negativeFlow;
                }
            }
        }

        if (selfLoop) {
            result.add(from);
        }

        return result;
    }


    private int facetIndex(int from, int dimension, int orientation) {
        return from + (stateCount * dimension) + (stateCount * dimensions * orientation);
    }

    private boolean getFacetColors(int from, int dimension, int orientation) {
        int facetIndex = facetIndex(from, dimension, orientation);
        Boolean currentValue = facetColors.get(facetIndex);

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
        boolean colors = false;

        /*
         * Iterate over all vertex masks. Vertex mask is a binary number which encodes a vertex of a state.
         * Specifically, every bit in the mask describes whether the vertex contains the lower/upper
         * threshold of the specified state. The smallest dimension corresponds to the least significant
         * bit of the mask.
         * So for a simple 3D cube [[1,2], [3, 5], [-1, 1]], we have 8 masks:
         * 000 - [1,3,-1]
         * 001 - [2,3,-1]
         * 010 - [1,5,-1]
         * 011 - [2,5,-1]
         * 100 - [1,3,1]
         * 101 - [2,3,1]
         * 110 - [1,5,1]
         * 111 - [2,5,1]
         */
        for (int mask = 0; mask < Math.pow(2, dimensions); mask++) {
            /*
                We want to evaluate half of the vertices. Which half is indicated by the positiveFacet variable.
                If positiveFacet is true, we want to evaluate vertices where the dimension bit is set to 1,
                if positiveFacet is false, the we want the dimension bit to be set to false.
             */
            if (((mask >> dimension) & 1) != positiveFacet) {
                continue;
            }
            int vertex = encoder.nodeVertex(from, mask);
            boolean vertexColor = getVertexColor(vertex, dimension, positiveDerivation);
            colors = colors | vertexColor;
        }

        facetColors.set(facetIndex, colors);
        return colors;


    }

    private boolean getVertexColor(int vertex, int dimension, boolean positive) {
        double derivationValue = 0.0;

        for (Summand summand : equations.get(dimension)) {
            double partialSum = summand.getConstant();
            for (Integer v : summand.getVariableIndices()) {
                partialSum *= thresholds.get(v).get(encoder.vertexCoordinate(vertex, v));
            }

            if (partialSum != 0.0) {
                for (Evaluable function : summand.getEvaluable()) {
                    int index = function.getVarIndex();
                    partialSum *= function.invoke(thresholds.get(index).get(encoder.vertexCoordinate(vertex, index)));
                }
            }
            derivationValue += partialSum;
        }

        return positive ? derivationValue > 0 : derivationValue < 0;
    }



    @NotNull
    @Override
    public Boolean transitionParameters(@NotNull Integer source, @NotNull Integer target) {
        return null;
    }

    public static void main(String[] args) {
        //Parser modelParser = new Parser();
        //OdeModel model = modelParser.parse(new File("model.txt"));
        //OdeModel modelWithThresholds = ModelApproximationKt.computeApproximation(model, false, true);
        //SimpleOdeTransitionSystem simpleOdeTransitionSystem = new SimpleOdeTransitionSystem(modelWithThresholds);
    }

}
