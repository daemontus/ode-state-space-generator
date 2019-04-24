package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.checker.Solver;
import com.github.sybila.ode.generator.rect.Rectangle;
import com.github.sybila.ode.model.OdeModel;

import java.util.Set;

public class TestClass implements OnTheFlyColorComputer<Set<Rectangle>> {

    @Override
    public void initialize(OdeModel model, Solver<Set<Rectangle>> solver) {
        System.out.println("Initializing...");
    }

    @Override
    public Set<Rectangle> getVertexColor(int vertex, int dimension, boolean positive) {
        System.out.println("Compute vertex color!");
        return null;
    }

}
