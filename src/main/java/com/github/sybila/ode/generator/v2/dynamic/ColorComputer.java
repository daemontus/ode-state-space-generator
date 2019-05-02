/*package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.checker.Solver;
import com.github.sybila.ode.generator.NodeEncoder;
import com.github.sybila.ode.generator.rect.Rectangle;
import com.github.sybila.ode.model.Evaluable;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.Summand;
import kotlin.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("Duplicates")
public class ColorComputer {
    private OdeModel model;
    private Solver<Set<Rectangle>> solver;
    private NodeEncoder encoder;
    private double[] boundsRect;

    public ColorComputer(OdeModel model, Solver<Set<Rectangle>> solver, NodeEncoder encoder, double[] boundsRect) {
        this.model = model;
        this.solver = solver;
        this.encoder = encoder;
        this.boundsRect = boundsRect;
    }

    public Set<Rectangle> getVertexColor(int vertex, int dimension, boolean positive) {
        // generated...
        if (dimension == 0) {
            return getVertexColor0(vertex, positive)
        } else if (dimension == 1) {
            return getVertexColor1(vertex, positive)
        } else {

        }
    }

    private Set<Rectangle> getVertexColor0(int vertex, boolean positive) {
        Set<Rectangle> result = new HashSet<>();
        List<Summand> equation = model.getVariables().get(dimension).getEquation();
        double derivationValue = 0.0;
        double denominator = 0.0;
        int parameterIndex = -1;

        for (Summand summand: equation) {
            double partialSum = summand.getConstant();
            for (Integer v: summand.getVariableIndices()) {
                partialSum *= varValue(vertex, v);
            }
            if (partialSum != 0.0) {
                for (Evaluable function: summand.getEvaluable()) {
                    int index = function.getVarIndex();
                    partialSum *= function.invoke(model.getVariables().get(index).getThresholds()
                            .get(encoder.vertexCoordinate(vertex, index)));
                }
            }

            if (summand.hasParam()) {
                parameterIndex = summand.getParamIndex();
                denominator += partialSum;
            } else {
                derivationValue += partialSum;
            }
        }

        if (parameterIndex == -1 || denominator == 0.0) {
            if ((positive && derivationValue > 0) || (!positive && derivationValue < 0)) {
                return solver.getTt();
            } else {
                return solver.getFf();
            }
        } else {
            // division by negative number flips the condition
            boolean newPositive = (denominator > 0) == positive;
            Pair<Double, Double> range = model.getParameters().get(parameterIndex).getRange();
            double split = Math.min(range.getSecond(), Math.max(range.getFirst(), -derivationValue / denominator));
            double newLow = newPositive ? split : range.getFirst();
            double newHigh = newPositive ? range.getSecond() : split;

            if (newLow >= newHigh) {
                return null;
            } else {
                double[] r = boundsRect.clone();
                r[2 * parameterIndex] = newLow;
                r[2 * parameterIndex + 1] = newHigh;
                result.add(new Rectangle(r));
            }
        }
        return result;
    }

    private double varValue(int vertex, int var) {
        return model.getVariables().get(var).getThresholds().get(encoder.vertexCoordinate(vertex, var));
    }


}
*/
