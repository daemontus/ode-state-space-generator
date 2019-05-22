package com.github.sybila.ode.generator.v2.benchmark;

import com.github.sybila.ode.generator.rect.RectangleOdeModel;
import com.github.sybila.ode.generator.v2.KotlinParamsOdeTransitionSystem;
import com.github.sybila.ode.generator.v2.ParamsOdeTransitionSystem;
import com.github.sybila.ode.generator.v2.dynamic.DynamicParamsOdeTransitionSystem;
import com.github.sybila.ode.generator.v2.dynamic.KotlinDynamicParamsOdeTransitionSystem;
import com.github.sybila.ode.model.ModelApproximationKt;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.Parser;

import java.io.File;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Duplicates")
public class Benchmark {

    //private static final String CLASS_PATH = "C:\\Users\\Jakub\\Desktop\\ode-generator\\build\\libs\\ode-generator-1.3.3-2-all.jar";
    private static final String CLASS_PATH = "/home/xracek6/ode-generator/build/libs/ode-generator-1.3.3-2-all.jar";

    public static void main(String[] args) {
        Parser modelParser = new Parser();
        OdeModel model = ModelApproximationKt.computeApproximation(modelParser.parse(new File("models/tcbb.bio")), false, false);
        //originalBenchmark(model, 3);
        //paramsOdeBenchmark(model, 3);
        kotlinParamsOdeBenchmark(model, 3);
        //dynamicParamsOdeBenchmark(model, 3);
        //kotlinDynamicParamsOdeBenchmark(model, 10);
    }


    private static void originalBenchmark(OdeModel model, int numOfRuns) {

        long[] times = new long[numOfRuns];

        for (int i = 0; i < numOfRuns; i++) {

            long startTime = System.currentTimeMillis();
            RectangleOdeModel rectangleModel = new RectangleOdeModel(model, true);
            for (int state = 0; state < rectangleModel.getStateCount(); state++) {
                if (state % 50000 == 0) System.out.println("Progress "+state+"/"+rectangleModel.getStateCount());
                rectangleModel.successors(state, true);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("Run "+i+" elapsed in: "+elapsedTime);
            times[i] = elapsedTime;

        }

        System.out.println("Original Benchmark");
        System.out.println("Average time: " + getAverage(times) + "ms");

    }


    private static void paramsOdeBenchmark(OdeModel model, int numOfRuns) {

        long[] times = new long[numOfRuns];

        for (int i = 0; i < numOfRuns; i++) {

            long startTime = System.currentTimeMillis();
            ParamsOdeTransitionSystem rectangleModel = new ParamsOdeTransitionSystem(model);
            for (int state = 0; state < rectangleModel.stateCount; state++) {
                if (state % 50000 == 0) System.out.println("Progress "+state+"/"+rectangleModel.stateCount);
                rectangleModel.successors(state);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("Run "+i+" elapsed in: "+elapsedTime);
            times[i] = elapsedTime;
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
            System.out.println("Run "+i+" elapsed in: "+elapsedTimeMs);
            times[i] = elapsedTimeMs;
        }

        System.out.println("Kotlin improved benchmark");
        System.out.println("Average time: " + getAverage(times) + "ms");
    }

    private static void dynamicParamsOdeBenchmark(OdeModel model, int numOfRuns) {
        long[] times = new long[numOfRuns];

        for (int i = 0; i < numOfRuns; i++) {
            long startTime = System.currentTimeMillis();
            DynamicParamsOdeTransitionSystem rectangleModel = new DynamicParamsOdeTransitionSystem(model, CLASS_PATH);
            for (int state = 0; state < rectangleModel.stateCount; state++) {
                if (state % 50000 == 0) System.out.println("Progress "+state+"/"+rectangleModel.stateCount);
                rectangleModel.successors(state);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("Run "+i+" elapsed in "+elapsedTime+" for "+rectangleModel.stateCount+" states.");
            times[i] = elapsedTime;
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

    private static double getAverage(long[] times) {
        double sum = 0;
        for (int i = 0; i < times.length; i++) {
            if (i == 0) continue; // skipping first measured time due to JVM reasons
            sum += times[i];
        }
        return sum / (times.length - 1);
    }

}