package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.checker.Solver;
import com.github.sybila.ode.generator.rect.Rectangle;
import com.github.sybila.ode.model.OdeModel;
import kotlin.Pair;

import java.util.HashSet;
import java.util.Set;

public class TestClass implements OnTheFlyColorComputer<Set<Rectangle>> {
    private OdeModel model;
    private Solver<Set<Rectangle>> solver;
    private double[] boundsRect;

    @Override
    public void initialize(OdeModel model, Solver<Set<Rectangle>> solver) {
        System.out.println("Initializing...");
        this.model = model;
        this.solver = solver;
        boundsRect = new double[2 * model.getParameters().size()];
        for (int i = 0; i < model.getParameters().size(); i++) {
            boundsRect[2 * i] = model.getParameters().get(i).getRange().getFirst();
            boundsRect[2 * i + 1] = model.getParameters().get(i).getRange().getSecond();
        }
    }

    @Override
    public Set<Rectangle> getVertexColor(int vertex, int dimension, boolean positive) {
        System.out.println("Compute vertex color!");
        Set<Rectangle> result = new HashSet<>();
        double derivationValue = 0.0;
        double denominator = 0.0;
        int parameterIndex = -1;


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

}
