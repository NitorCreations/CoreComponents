package com.nitorcreations.junit.runners.parameterized;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used by {@link WrappingParameterizedRunner} to detect the
 * method which configures the parameters with which the suite is to be run. The
 * method signature is expected to be (the name doesn't matter):
 * 
 * <pre>
 * public static void suite(ParameterizedSuiteBuilder builder) { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ParameterizedSuite {
}
