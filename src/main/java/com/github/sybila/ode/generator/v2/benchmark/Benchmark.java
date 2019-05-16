package com.github.sybila.ode.generator.v2.benchmark;

import com.github.sybila.ode.generator.rect.RectangleOdeModel;
import com.github.sybila.ode.generator.v2.KotlinParamsOdeTransitionSystem;
import com.github.sybila.ode.generator.v2.ParamsOdeTransitionSystem;
import com.github.sybila.ode.generator.v2.dynamic.DynamicParamsOdeTransitionSystem;
import com.github.sybila.ode.generator.v2.dynamic.KotlinDynamicParamsOdeTransitionSystem;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.Parser;

import java.io.File;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Duplicates")
public class Benchmark {
    public static void main(String[] args) {
        Parser modelParser = new Parser();
        OdeModel model = modelParser.parse(new File("models/model_31_reduced.bio"));
        originalBenchmark(model);
        //paramsOdeBenchmark(model);
        //kotlinParamsOdeBenchmark(model);
        //dynamicParamsOdeBenchmark(model);
        //kotlinDynamicParamsOdeBenchmark(model);
    }


    private static void originalBenchmark(OdeModel model) {
        long startTime = System.nanoTime();
        RectangleOdeModel rectangleModel = new RectangleOdeModel(model, true);
        for (int state = 0; state < rectangleModel.getStateCount(); state++) {
            rectangleModel.successors(state, true);
        }
        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Original benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }

    private static void paramsOdeBenchmark(OdeModel model) {
        long startTime = System.nanoTime();
        ParamsOdeTransitionSystem rectangleModel = new ParamsOdeTransitionSystem(model);
        for (int state = 0; state < rectangleModel.stateCount; state++) {
            rectangleModel.successors(state);
        }
        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Improved benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }

    private static void kotlinParamsOdeBenchmark(OdeModel model) {
        long startTime = System.nanoTime();
        KotlinParamsOdeTransitionSystem rectangleModel = new KotlinParamsOdeTransitionSystem(model, true);

        for (int state = 0; state < rectangleModel.getStateCount(); state++) {
            rectangleModel.successors(state);
        }

        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Test benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }

    private static void dynamicParamsOdeBenchmark(OdeModel model) {
        long startTime = System.nanoTime();
        RectangleOdeModel rectangleModel = new RectangleOdeModel(model, true);
        for (int state = 0; state < rectangleModel.getStateCount(); state++) {
            rectangleModel.successors(state, true);
        }

        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Dynamic benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }

    private static void kotlinDynamicParamsOdeBenchmark(OdeModel model) {
        long startTime = System.nanoTime();

        RectangleOdeModel rectangleModel = new RectangleOdeModel(model, true);
        for (int state = 0; state < rectangleModel.getStateCount(); state++) {
            rectangleModel.successors(state, true);
        }

        long elapsedTime = System.nanoTime() - startTime;

        double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("Test benchmark");
        System.out.println("Elapsed time: " + elapsedTimeMs + " ms");
    }

}