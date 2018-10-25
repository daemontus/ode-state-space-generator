package com.github.sybila.ode.generator.v2;

import com.github.sybila.ode.generator.NodeEncoder;
import com.github.sybila.ode.model.ModelApproximationKt;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.Parser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class SimpleOdeTransitionSystem implements TransitionSystem<Integer, Boolean> {

    private final OdeModel model;
    private final NodeEncoder encoder;

    public SimpleOdeTransitionSystem(OdeModel model) {
        this.model = model;
        this.encoder = new NodeEncoder(model);
    }

    @NotNull
    @Override
    public List<Integer> successors(@NotNull Integer $receiver) {
        return null;
    }

    @NotNull
    @Override
    public List<Integer> predecessors(@NotNull Integer $receiver) {
        return null;
    }

    @NotNull
    @Override
    public Boolean transitionParameters(@NotNull Integer source, @NotNull Integer target) {
        return null;
    }

    public static void main(String[] args) {
        Parser modelParser = new Parser();
        OdeModel model = modelParser.parse(new File("path"));
        OdeModel modelWithThresholds = ModelApproximationKt.computeApproximation(model, false, true);
    }

}
