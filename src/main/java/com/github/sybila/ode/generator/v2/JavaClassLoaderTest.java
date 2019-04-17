package com.github.sybila.ode.generator.v2;

public class JavaClassLoaderTest /*extends JavaClassLoader*/ {
    public static void main(String[] args) {
        JavaClassLoader loader = new JavaClassLoader();
        loader.invokeClassMethod("com.github.sybila.ode.generator.v2.TestClass", "sayHelloWorld");
    }

}
