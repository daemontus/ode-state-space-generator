package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.checker.Solver;
import com.github.sybila.ode.model.OdeModel;

/**
 * Generic interface which provides functionality for computing parameters for a vertex.
 *
 * @param <T> type representation of parameters
 */
public interface OnTheFlyColorComputer<T> {

    /**
     * Initializes the color computer with the given ODE model and a solver.
     *
     * @param model ODE model
     * @param solver Solver
     */
    void initialize(OdeModel model, Solver<T> solver);

    /**
     * Computes and returns parameters for which derivation in the given dimension is positive/negative
     * at the given vertex.
     *
     * @param vertex int representation of vertex
     * @param dimension dimension
     * @param positive boolean value indicating if the derivative should be positive
     * @return parameters
     */
    T getVertexColor(int vertex, int dimension, boolean positive);

}
