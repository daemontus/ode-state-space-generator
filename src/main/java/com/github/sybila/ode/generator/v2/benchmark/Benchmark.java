package com.github.sybila.ode.generator.v2.benchmark;

import com.github.sybila.ode.generator.rect.RectangleOdeModel;
import com.github.sybila.ode.generator.v2.ParamsOdeTransitionSystem;
import com.github.sybila.ode.generator.v2.Test;
import com.github.sybila.ode.generator.v2.dynamic.DynamicParamsOdeTransitionSystem;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.Parser;

import java.io.File;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Duplicates")
public class Benchmark {
    public static void main(String[] args) {
        Parser modelParser = new Parser();
        OdeModel model = modelParser.parse(new File("models/tcbb.bio"));
        //originalBenchmark(model);
        //improvedBenchmark(model);
        //dynamicBenchmark(model);
        testBenchmark(model);
    }

    public static void originalBenchmark(OdeModel model) {
        RectangleOdeModel rectangleModel = new RectangleOdeModel(model, true);

        long startTime = System.nanoTime();
        for (int state = 0; state < rectangleModel.getStateCount(); state++) {
            rectangleModel.successors(state, true);
        }
        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Original benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }

    public static void improvedBenchmark(OdeModel model) {
        ParamsOdeTransitionSystem rectangleModel = new ParamsOdeTransitionSystem(model);

        long startTime = System.nanoTime();
        for (int state = 0; state < rectangleModel.stateCount; state++) {
            rectangleModel.successors(state);
        }
        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Improved benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }

    public static void dynamicBenchmark(OdeModel model) {
        DynamicParamsOdeTransitionSystem rectangleModel = new DynamicParamsOdeTransitionSystem(model, "/home/xracek6/ode-generator/build/libs/ode-generator-1.3.3-2-all.jar");

        long startTime = System.nanoTime();
        for (int state = 0; state < rectangleModel.stateCount; state++) {
            rectangleModel.successors(state);
        }
        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Dynamic benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }

    public static void testBenchmark(OdeModel model) {
        Test testModel = new Test(model, true);
        long startTime = System.nanoTime();
        for (int state = 0; state < testModel.getStateCount(); state++) {
            testModel.successors(state);
        }
        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Test benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }
}
