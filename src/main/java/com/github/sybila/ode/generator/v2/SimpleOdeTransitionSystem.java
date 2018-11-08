package com.github.sybila.ode.generator.v2;

import com.github.sybila.ode.generator.NodeEncoder;
import com.github.sybila.ode.model.Evaluable;
import com.github.sybila.ode.model.ModelApproximationKt;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.OdeModel.Variable;
import com.github.sybila.ode.model.Parser;
import com.github.sybila.ode.model.Summand;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SimpleOdeTransitionSystem implements TransitionSystem<Integer, Boolean> {

    private final OdeModel model;
    private final NodeEncoder encoder;
    private final Integer dimensions;
    private Integer stateCount;
    private List<Integer> facetColors;
    private List<List<Summand>> equations = new ArrayList<>();
    private List<List<Double>> thresholds = new ArrayList<>();

    private Integer PositiveIn = 0;
    private Integer PositiveOut = 1;
    private Integer NegativeIn = 2;
    private Integer NegativeOut = 3;

    public SimpleOdeTransitionSystem(OdeModel model) {
        this.model = model;
        this.encoder = new NodeEncoder(model);
        this.dimensions = model.getVariables().size();
        this.stateCount = getStateCount();
        this.facetColors = new ArrayList<>();
        for (int i = 0; i < stateCount * dimensions * 4) {
            this.facetColors.add(null);
        }

        for (Variable var: model.getVariables()) {
            this.equations.add(var.getEquation());
            this.thresholds.add(var.getThresholds());
        }



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
        List<Integer> result = new ArrayList<>();
        for (int dim = 0; dim < model.getVariables().size(); dim++) {
            String dimName = model.getVariables().get(dim).getName();
            Boolean timeFlow = true;


            List<Integer> positiveOut = getFacetColors(from, dim, timeFlow ? PositiveOut : PositiveIn);
            List<Integer> negativeOut = getFacetColors(from, dim, timeFlow ? NegativeOut : NegativeIn);


            Integer higherNode = encoder.higherNode(from, dim);
            if (higherNode != null) {
                List<Integer> colors = positiveOut;
                /*if (colors.) { emptiness check?
                    result.add(higherNode);
                }*/
            }

            Integer lowerNode = encoder.lowerNode(from, dim);
            if (lowerNode != null) {
                List<Integer> colors = negativeOut;
                /*if (colors.) { emptiness check?
                    result.add(higherNode);
                }*/
            }

        }
        return result;
    }

    @NotNull
    @Override
    public List<Integer> predecessors(@NotNull Integer from) {
        List<Integer> result = new ArrayList<>();
        return result;
    }


    private Integer facetIndex(Integer from, Integer dimension, Integer orientation) {
        return from + (stateCount * dimension) + (stateCount * dimensions * orientation);
    }

    private List<Integer> getFacetColors(Integer from, Integer dimension, Integer orientation) {
        Integer index = facetIndex(from, dimension, orientation);
        Integer value = facetColors.get(index) != null ? facetColors.get(index) :


    }

    private Boolean getVertexColor(Integer vertex, Integer dimension, Boolean positive) {
        Integer dim = dimension;

        Double derivationValue = 0.0;

        for (Summand summand : equations.get(dim)) {
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
        OdeModel model = modelParser.parse(new File("/home/jakub/Desktop/SBAPR/ode-generator/src/main/java/com/github/sybila/ode/generator/v2/model.txt"));
        OdeModel modelWithThresholds = ModelApproximationKt.computeApproximation(model, false, true);
    }

}
