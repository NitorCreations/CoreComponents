/**
 * Copyright 2014 Nitor Creations Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nitorcreations.junit.runners.parameterized;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.runner.Description;

/**
 * This is a helper class for {@link WrappingParameterizedRunner}. It rewrites
 * descriptions such that they use the original test class instead of the
 * dynamically created (synthesized) extending test class so that test reports
 * refer back to the original test class. It also remembers the mappings done so
 * they can be re-applied later when the actual test runs report success or
 * failure. The {@link Description#equals(Object)} and
 * {@link Description#hashCode()} implementations suitably support using the
 * Description objects themselves directly as map keys.
 */
public class DescriptionMapper {
	private final Map<Description, Description> originalToRewrittenDescriptionMap = new HashMap<Description, Description>();
	private final Class<?> testClass;
	private final Constructor<Description> descConstr;
	private final Field uniqueIdField;
	private final Field testClassField;

	public DescriptionMapper(Class<?> testClass) {
		this.testClass = testClass;

		// The Description class is mutation-hostile, so just pull the rug and
		// go all reflective on it.
		try {
			descConstr = Description.class.getDeclaredConstructor(Class.class,
					String.class, Serializable.class, Annotation[].class);
			uniqueIdField = Description.class.getDeclaredField("fUniqueId");
			testClassField = Description.class.getDeclaredField("fTestClass");
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		descConstr.setAccessible(true);
		uniqueIdField.setAccessible(true);
		testClassField.setAccessible(true);
	}

	/**
	 * Rewrites the description of a first-level runner child of the
	 * WrappingParameterizedRunner runner, and recursively its children.
	 * Replaces the original description with the given description identifying
	 * the child and the test class with the one given to this class'
	 * constructor. The child's children descriptions are just rewritten to use
	 * the test class given in constructor instead of direct subclass, where
	 * applicable.
	 * 
	 * @param description
	 *            the new description text to use for this child
	 * @param orig
	 *            the original description object provided by the child runner.
	 * @return the rewritten description object
	 */
	public Description rewriteDescription(String description, Description orig) {
		return createDescription(testClass, description, orig);
	}

	private Description rewriteIndirectChildDescription(Description orig) {
		Class<?> childTestClass = getTestClassFromDescription(orig);
		String displayName = orig.getDisplayName();

		if (childTestClass.getSuperclass() == testClass) {
			displayName = displayName.replace(childTestClass.getName(),
					testClass.getName());
			childTestClass = testClass;
		}

		return createDescription(childTestClass, displayName, orig);
	}

	private Description createDescription(Class<?> testClass,
			String displayName, Description orig) {
		Serializable uniqueId = getUniqueIdFromDescription(orig);
		Annotation[] annotations = orig.getAnnotations().toArray(
				new Annotation[0]);

		Description desc;
		try {
			desc = descConstr.newInstance(testClass, displayName, uniqueId,
					annotations);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}

		for (Description child : orig.getChildren()) {
			desc.addChild(rewriteIndirectChildDescription(child));
		}

		originalToRewrittenDescriptionMap.put(orig, desc);
		return desc;
	}

	private Class<?> getTestClassFromDescription(Description orig) {
		Class<?> origTestClass;
		try {
			origTestClass = (Class<?>) testClassField.get(orig);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return origTestClass;
	}

	private Serializable getUniqueIdFromDescription(Description desc) {
		try {
			return (Serializable) uniqueIdField.get(desc);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This is used to map "original" descriptions again in the same manner as
	 * when they were rewritten earlier using
	 * {@link #rewriteDescription(String, Description)}. Any description object
	 * given to the {@link #rewriteDescription(String, Description)} or any
	 * child, child's child etc inside it can be supplied to this method.
	 * 
	 * @param orig
	 *            the original description object
	 * @return the associated description as it was rewritten when calling
	 *         {@link #rewriteDescription(String, Description)}.
	 */
	public Description map(Description orig) {
		Description desc = originalToRewrittenDescriptionMap.get(orig);
		assert desc != null : 
			"WrappingParameterizedRunner: Internal error";
		return desc;
	}
}
