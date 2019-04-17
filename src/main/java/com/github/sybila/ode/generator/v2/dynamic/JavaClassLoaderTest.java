package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.ode.generator.v2.dynamic.JavaClassLoader;

public class JavaClassLoaderTest /*extends JavaClassLoader*/ {
    public static void main(String[] args) {
        JavaClassLoader loader = new JavaClassLoader();
        loader.invokeClassMethod("com.github.sybila.ode.generator.v2.dynamic.TestClass", "sayHelloWorld");
    }

}
