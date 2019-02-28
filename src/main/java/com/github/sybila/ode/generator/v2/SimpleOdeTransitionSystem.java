package com.github.sybila.ode.generator.v2;

import com.github.sybila.ode.generator.NodeEncoder;
import com.github.sybila.ode.model.Evaluable;
import com.github.sybila.ode.model.ModelApproximationKt;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.OdeModel.Variable;
import com.github.sybila.ode.model.Parser;
import com.github.sybila.ode.model.Summand;
import com.sun.org.apache.xpath.internal.operations.Neg;
import kotlin.collections.CollectionsKt;
import kotlin.collections.IntIterator;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SimpleOdeTransitionSystem implements TransitionSystem<Integer, Boolean> {

    private final OdeModel model;
    private final NodeEncoder encoder;
    private final Integer dimensions;
    private Integer stateCount;
    private List<Boolean> facetColors;
    private List<List<Summand>> equations = new ArrayList<>();
    private List<List<Double>> thresholds = new ArrayList<>();

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
        }

        createSelfLoops = true;
    }

    private Integer getStateCount() {
        Integer result = 1;
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


    private List<Integer> getStep(Integer from, Boolean successors) {
        List<Integer> result = new ArrayList<>();
        Boolean selfloop = true;
        for (int dim = 0; dim < model.getVariables().size(); dim++) {

            Boolean positiveIn = getFacetColors(from, dim, PositiveIn);
            Boolean negativeIn = getFacetColors(from, dim, NegativeIn);
            Boolean positiveOut = getFacetColors(from, dim, PositiveOut);
            Boolean negativeOut = getFacetColors(from, dim, NegativeOut);

            Integer higherNode = encoder.higherNode(from, dim);
            if (higherNode != null) {
                Boolean colors = successors ? positiveOut : positiveIn;
                if (colors) {
                    result.add(higherNode);
                }

                if (createSelfLoops) {
                    Boolean positiveFlow = negativeIn && positiveOut && !(negativeOut || positiveIn);
                    selfloop = !positiveFlow;
                }
            }

            Integer lowerNode = encoder.lowerNode(from, dim);
            if (lowerNode != null) {
                Boolean colors = successors ? negativeOut : negativeIn;
                if (colors) {
                    result.add(lowerNode);
                }

                if (createSelfLoops) {
                    Boolean negativeFlow = negativeOut && positiveIn && !(negativeIn || positiveOut);
                    selfloop = !negativeFlow;
                }
            }
        }

        if (selfloop) {
            result.add(from);
        }

        return result;
    }


    private Integer facetIndex(Integer from, Integer dimension, Integer orientation) {
        return from + (stateCount * dimension) + (stateCount * dimensions * orientation);
    }

    private Boolean getFacetColors(Integer from, Integer dimension, Integer orientation) {
        Integer facetIndex = facetIndex(from, dimension, orientation);
        Boolean facetColor = facetColors.get(facetIndex);

        /*
        if (facetColor != null) {
            return facetColor;
        } else {
            for (int i = 0; i < Math.pow(2, dimension); i++) {

                if (((i >> dimension) & 1) != positiveFacet) {
                    continue;
                }

                Integer vertex = encoder.nodeVertex(from, i);
                Boolean vertexColor = getVertexColor(vertex, dimension, positiveDerivation);
                if (vertexColor != null && vertexColor) {
                    facetColors.set(facetIndex, true);
                    return true;
                }
            }

            facetColors.set(facetIndex, false);
            return false;
        }
        */

        if (facetColor != null) {
            return facetColor;
        }

        Integer positiveFacet = (orientation.equals(PositiveIn) || orientation.equals(PositiveOut)) ? 1 : 0;
        Boolean positiveDerivation = orientation.equals(PositiveOut) || orientation.equals(NegativeIn);
        Boolean colors = false;

        for (int i = 0; i < Math.pow(2, dimension); i++) {
            Integer helper = (i >> dimension) & 1;
            if (!helper.equals(positiveFacet)) {
                continue;
            }
            Integer vertex = encoder.nodeVertex(from, i);
            Boolean vertexColor = getVertexColor(vertex, dimension, positiveDerivation);
            if (vertexColor != null && vertexColor) {
                colors = true;
            }
        }

        facetColors.set(facetIndex, colors);

        if (orientation.equals(PositiveIn) || orientation.equals(PositiveOut)) {
            Integer higherNode = encoder.higherNode(from, dimension);
            if (higherNode != null) {
                Integer dual = orientation.equals(PositiveIn) ? NegativeOut : NegativeIn;
                facetColors.set(facetIndex(higherNode,dimension, dual), colors);
            }

        } else {
            Integer lowerNode = encoder.lowerNode(from, dimension);
            if (lowerNode != null) {
                Integer dual = orientation.equals(NegativeIn) ? PositiveOut : PositiveIn;
                facetColors.set(facetIndex(lowerNode, dimension, dual), colors);
            }
        }

        return facetColors.get(facetIndex);


    }

    private Boolean getVertexColor(Integer vertex, Integer dimension, Boolean positive) {
        Double derivationValue = 0.0;

        for (Summand summand : equations.get(dimension)) {
            double partialSum = summand.getConstant();
            for (Integer v : summand.getVariableIndices()) {
                partialSum *= thresholds.get(v).get(encoder.vertexCoordinate(vertex, v));
            }

            if (partialSum != 0.0) {
                for (Evaluable function : summand.getEvaluable()) {
                    Integer index = function.getVarIndex();
                    partialSum *= function.invoke(thresholds.get(index).get(encoder.vertexCoordinate(vertex, index)));
                }
            }
            derivationValue += partialSum;
        }

        return derivationValue == 0.0 ? null : derivationValue > 0 == positive;
    }



    @NotNull
    @Override
    public Boolean transitionParameters(@NotNull Integer source, @NotNull Integer target) {
        return null;
    }

    public static void main(String[] args) {
        Parser modelParser = new Parser();
        //OdeModel model = modelParser.parse(new File("model.txt"));
        //OdeModel modelWithThresholds = ModelApproximationKt.computeApproximation(model, false, true);
        //SimpleOdeTransitionSystem simpleOdeTransitionSystem = new SimpleOdeTransitionSystem(modelWithThresholds);
    }

}
