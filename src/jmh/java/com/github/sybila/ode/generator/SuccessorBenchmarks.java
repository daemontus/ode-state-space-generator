package com.github.sybila.ode.generator;
/*
import com.github.sybila.ode.model.ModelApproximationKt;
import cz.muni.fi.checker.IDNode;
import cz.muni.fi.checker.Nodes;
import cz.muni.fi.checker.PartitionFunction;
import cz.muni.fi.checker.UniformPartitionFunction;
import com.github.sybila.ode.model.Parser;
import kotlin.collections.CollectionsKt;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.util.Map;

public class SuccessorBenchmarks {

    @State(Scope.Thread)
    public static class MyState {
        RectangleOdeModel TCBBModel;
        Nodes<IDNode, RectangleColors> TCBBNodes;


        @Setup(Level.Trial)
        public void setup() {
            PartitionFunction<IDNode> partition = new UniformPartitionFunction<>();
            TCBBModel = new RectangleOdeModel(
                    ModelApproximationKt.computeApproximation(
                            new Parser().parse(new File("models/tcbb.bio")), true, false)
                    , partition);
            TCBBNodes = TCBBModel.allNodes();
        }
    }


    @Benchmark
    public int TCBBSuccessorBenchmark(MyState state) {
        int count = 0;
        for (Map.Entry<IDNode, RectangleColors> node : state.TCBBNodes.getEntries()) {
            count += CollectionsKt.count(state.TCBBModel.getSuccessors().invoke(node.getKey()).getEntries());
        }
        return count;
    }
}*/
