package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.checker.Solver;
import com.github.sybila.ode.model.OdeModel;

public interface OnTheFlyColorComputer<T> {

    void initialize(OdeModel model, Solver<T> solver);

    T getVertexColor(int vertex, int dimension, boolean positive);

}
