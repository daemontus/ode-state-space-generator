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
        //originalBenchmark(model, 10);
        //paramsOdeBenchmark(model, 10);
        //kotlinParamsOdeBenchmark(model, 10);
        //dynamicParamsOdeBenchmark(model, 1);
        //kotlinDynamicParamsOdeBenchmark(model, 10);
    }


    private static void originalBenchmark(OdeModel model, int numOfRuns) {

        double[] times = new double[numOfRuns];

        for (int i = 0; i < numOfRuns; i++) {

            long startTime = System.nanoTime();
            RectangleOdeModel rectangleModel = new RectangleOdeModel(model, true);
            for (int state = 0; state < rectangleModel.getStateCount(); state++) {
                rectangleModel.successors(state, true);
            }

            long elapsedTime = System.nanoTime() - startTime;
            double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
            times[i] = elapsedTimeMs;

        }

        System.out.println("Original Benchmark");
        System.out.println("Average time: " + getAverage(times) + "ms");

    }


    private static void paramsOdeBenchmark(OdeModel model, int numOfRuns) {

        double[] times = new double[numOfRuns];

        for (int i = 0; i < numOfRuns; i++) {

            long startTime = System.nanoTime();
            ParamsOdeTransitionSystem rectangleModel = new ParamsOdeTransitionSystem(model);
            for (int state = 0; state < rectangleModel.stateCount; state++) {
                rectangleModel.successors(state);
            }

            long elapsedTime = System.nanoTime() - startTime;
            double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
            times[i] = elapsedTimeMs;
        }


        System.out.println("Improved benchmark");
        System.out.println("Average time: " + getAverage(times) + "ms");
    }

    private static void kotlinParamsOdeBenchmark(OdeModel model, int numOfRuns) {

        double[] times = new double[numOfRuns];

        for (int i = 0; i < numOfRuns; i++) {
            long startTime = System.nanoTime();
            KotlinParamsOdeTransitionSystem rectangleModel = new KotlinParamsOdeTransitionSystem(model, true);

            for (int state = 0; state < rectangleModel.getStateCount(); state++) {
                rectangleModel.successors(state);
            }

            long elapsedTime = System.nanoTime() - startTime;
            double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
            times[i] = elapsedTimeMs;
        }

        System.out.println("Kotlin improved benchmark");
        System.out.println("Average time: " + getAverage(times) + "ms");
    }

    private static void dynamicParamsOdeBenchmark(OdeModel model, int numOfRuns) {
        double[] times = new double[numOfRuns];

        for (int i = 0; i < numOfRuns; i++) {
            long startTime = System.nanoTime();
            DynamicParamsOdeTransitionSystem rectangleModel = new DynamicParamsOdeTransitionSystem(model, "C:\\Users\\Jakub\\Desktop\\ode-generator\\build\\libs\\ode-generator-1.3.3-2-all.jar");
            for (int state = 0; state < rectangleModel.stateCount; state++) {
                rectangleModel.successors(state);
            }

            long elapsedTime = System.nanoTime() - startTime;
            double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
            times[i] = elapsedTimeMs;
        }


        System.out.println("Dynamic benchmark");
        System.out.println("Average time: " + getAverage(times) + " ms");
    }

    private static void kotlinDynamicParamsOdeBenchmark(OdeModel model, int numOfRuns) {
        double[] times = new double[numOfRuns];

        for (int i = 0; i < numOfRuns; i++) {
            long startTime = System.nanoTime();
            KotlinDynamicParamsOdeTransitionSystem rectangleModel = new KotlinDynamicParamsOdeTransitionSystem(model, true, "C:\\Users\\Jakub\\Desktop\\ode-generator\\build\\libs\\ode-generator-1.3.3-2-all.jar");
            for (int state = 0; state < rectangleModel.getStateCount(); state++) {
                rectangleModel.successors(state);
            }

            long elapsedTime = System.nanoTime() - startTime;
            double elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
            times[i] = elapsedTimeMs;
        }


        System.out.println("Kotlin dynamic benchmark");
        System.out.println("Average time: " + getAverage(times) + " ms");
    }

    private static double getAverage(double[] times) {
        double sum = 0;
        for (int i = 0; i < times.length; i++) {
            if (i == 0) continue; // skipping first measured time due to JVM reasons
            sum += times[i];
        }
        return sum / (times.length - 1);
    }

}