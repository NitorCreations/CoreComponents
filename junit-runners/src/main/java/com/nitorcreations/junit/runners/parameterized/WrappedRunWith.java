package com.nitorcreations.junit.runners.parameterized;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.runner.Runner;

/**
 * This annotation used to indicate which runner is to be wrapped when a
 * wrapping runner is given in the &#64;RunWith(...) annotation.
 * 
 * @see WrappingParameterizedRunner
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface WrappedRunWith {
	Class<? extends Runner> value();
}
