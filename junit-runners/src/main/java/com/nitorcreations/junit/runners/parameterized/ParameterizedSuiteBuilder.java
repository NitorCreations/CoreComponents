package com.nitorcreations.junit.runners.parameterized;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is a helper class for {@link WrappingParameterizedRunner}. It acts as a
 * suite builder, providing an {@link #add()} method to add new tests to the
 * suite. The add method returns a {@link TestInstantiatorBuilder} instance
 * which can then be used to configure the test.
 */
public class ParameterizedSuiteBuilder {
	private final Class<?> testClass;
	private final List<TestInstantiatorBuilder> tests = new ArrayList<TestInstantiatorBuilder>();

	public ParameterizedSuiteBuilder(Class<?> testClass) {
		this.testClass = testClass;
	}

	/**
	 * Add a new test to the suite, to be constructed with the given arguments.
	 * <p>
	 * In case there are multiple constructors available, please note that the
	 * constructor is chosen based on the runtime types of the arguments, which
	 * might be different from constructing the class directly.
	 * 
	 * @param constructorArgs
	 *            the arguments to give to the constructor
	 * @return a builder for further configuration of the test.
	 */
	public TestInstantiatorBuilder constructWith(Object... constructorArgs) {
		TestInstantiatorBuilder testBuilder = new TestInstantiatorBuilder(
				constructorArgs);
		tests.add(testBuilder);
		return testBuilder;
	}

	List<TestInstantiatorBuilder> getTests() {
		return tests;
	}

	static final Map<Class<?>, Class<?>> UNBOXED_TO_BOXED = new HashMap<Class<?>, Class<?>>();
	static {
		UNBOXED_TO_BOXED.put(Integer.TYPE, Integer.class);
		UNBOXED_TO_BOXED.put(Byte.TYPE, Byte.class);
		UNBOXED_TO_BOXED.put(Character.TYPE, Character.class);
		UNBOXED_TO_BOXED.put(Long.TYPE, Long.class);
		UNBOXED_TO_BOXED.put(Double.TYPE, Double.class);
		UNBOXED_TO_BOXED.put(Float.TYPE, Float.class);
		UNBOXED_TO_BOXED.put(Boolean.TYPE, Boolean.class);
		UNBOXED_TO_BOXED.put(Short.TYPE, Short.class);
	}

	/**
	 * Builder for configuring a single test in a parameterized suite. The
	 * with*() methods are used to set up the test. The builder actively
	 * validates the calls, immediately giving feedback on improper usage.
	 */
	public class TestInstantiatorBuilder {
		private Constructor<?> constructor;
		private Object[] constructorArgs;
		private String description;

		TestInstantiatorBuilder(Object... constructorArgs) {
			if (constructorArgs == null) {
				throw new IllegalArgumentException(
						"constructor argument list null");
			}
			this.constructor = findBestConstructorMatch(constructorArgs);
			this.constructorArgs = Arrays.copyOf(constructorArgs,
					constructorArgs.length);
		}

		public Constructor<?> getConstructor() {
			return constructor;
		}

		public Object[] getConstructorArgs() {
			return constructorArgs;
		}

		public String getDescription() {
			if (description == null) {
				StringBuilder sb = new StringBuilder();
				boolean first = true;
				for (Object parameter : constructorArgs) {
					if (first) {
						first = false;
					} else {
						sb.append(", ");
					}
					if (parameter instanceof String) {
						parameter = '"' + (String) parameter + '"';
					}
					sb.append(parameter);
				}
				description = sb.toString();
			}
			return description;
		}

		private Constructor<?> findBestConstructorMatch(
				Object... constructorArgs) {
			Constructor<?>[] constructors = (Constructor<?>[]) testClass
					.getConstructors();
			Constructor<?> c;
			if (constructors.length > 1) {
				List<Constructor<?>> cs = new ArrayList<Constructor<?>>(
						Arrays.asList(constructors));
				Iterator<Constructor<?>> ics = cs.iterator();
				ics: while (ics.hasNext()) {
					Class<?>[] parameterTypes = ics.next().getParameterTypes();
					if (parameterTypes.length != constructorArgs.length) {
						ics.remove();
						continue;
					}
					for (int i = 0; i < constructorArgs.length; ++i) {
						Class<?> boxedType = boxedTypeFor(parameterTypes[i]);
						if (!boxedType.isInstance(constructorArgs[i])) {
							ics.remove();
							continue ics;
						}
					}
				}
				if (cs.isEmpty()) {
					throw new IllegalArgumentException(
							"No constructor(s) found matching given argument types");
				}
				if (cs.size() > 1) {
					filterRedundantConstructors(cs);
					if (cs.size() > 1) {
						throw new IllegalStateException(
								buildAmbiguousConstructorsExceptionString(cs));
					}
				}
				c = cs.get(0);
			} else {
				c = constructors[0];
			}
			validateConstructorArguments(c, constructorArgs);
			return c;
		}

		private void filterRedundantConstructors(List<Constructor<?>> cs) {
			for (int i1 = 0; i1 < cs.size() - 1; ++i1) {
				for (int i2 = i1 + 1; i2 < cs.size(); ++i2) {
					Constructor<?> c1 = cs.get(i1);
					Constructor<?> c2 = cs.get(i2);
					if (isFirstStrictlyLessSpecificThanSecond(c1, c2)) {
						cs.remove(i1);
						i2 = i1;
					} else if (isFirstStrictlyLessSpecificThanSecond(c2, c1)) {
						cs.remove(i2);
						--i2;
					}
				}
			}
		}

		/**
		 * Determines whether all arguments of the first constructor are either
		 * as specific or less specific as the respective arguments of the
		 * second constructor. So when called with foo(Object, Object) and
		 * foo(String, String) it will return true, whereas foo(Object, String)
		 * and foo(String, Object) will return false.
		 */
		private boolean isFirstStrictlyLessSpecificThanSecond(
				Constructor<?> c1, Constructor<?> c2) {
			Class<?>[] ps1 = c1.getParameterTypes();
			Class<?>[] ps2 = c2.getParameterTypes();
			assert ps1.length == ps2.length;
			for (int i = 0; i < ps1.length; ++i) {
				if (!ps1[i].isAssignableFrom(ps2[i])) {
					return false;
				}
			}
			return true;
		}

		private String buildAmbiguousConstructorsExceptionString(
				List<Constructor<?>> cs) {
			StringBuilder sb = new StringBuilder();
			sb.append("Found ")
					.append(cs.size())
					.append(" ambiguous constructors matching given arguments:");
			for (Constructor<?> c2 : cs) {
				String sep = "\n\t- (";
				for (Class<?> pt : c2.getParameterTypes()) {
					String name = pt.getName();
					sb.append(sep).append(
							name.startsWith("java.lang.") ? name.substring(10)
									: name);
					sep = ", ";
				}
				sb.append(')');
			}
			return sb.toString();
		}

		/**
		 * Set the test description. This appears as the test subtree name in
		 * the test report.
		 * 
		 * @return the same test builder instance, for chaining.
		 */

		public TestInstantiatorBuilder named(String description) {
			if (description == null) {
				description = "(null)";
			}
			this.description = description;
			return this;
		}

		private void validateConstructorArguments(Constructor<?> c,
				Object[] args) {
			Class<?>[] parameterTypes = c.getParameterTypes();
			if (parameterTypes.length != args.length) {
				throw new IllegalArgumentException(
						"Constructor argument count mismatch: expected "
								+ parameterTypes.length + ", got "
								+ args.length);
			}
			for (int i = 0; i < args.length; ++i) {
				Class<?> boxedType = boxedTypeFor(parameterTypes[i]);
				if (args[i] == null) {
					if (parameterTypes[i].isPrimitive()) {
						throw new IllegalArgumentException(
								"Constructor argument "
										+ i
										+ " expects primitive type "
										+ parameterTypes[i].getName()
										+ " but null value was given for constructor "
										+ c + " with proposed arguments "
										+ Arrays.toString(args));
					}
				} else if (!boxedType.isInstance(args[i])) {
					if (!parameterTypes[i].isPrimitive() || !canBeCastToPrimitiveType(args[i], parameterTypes[i])) {
						throw new IllegalArgumentException("Constructor argument "
								+ i + " expected type " + boxedType.getName()
								+ " but got " + args[i].getClass().getName()
								+ " for constructor " + c
								+ " with proposed arguments "
								+ Arrays.toString(args));
					}
				}
			}
		}

		private boolean canBeCastToPrimitiveType(Object arg, Class<?> primitiveType) {
			if (primitiveType == Boolean.TYPE){
				return false;
			}
			final boolean isChar = arg instanceof Character;
			final boolean isDouble = arg instanceof Double;
			final boolean isFloat = arg instanceof Float;
			final boolean isLong = arg instanceof Long;
			final boolean isInteger = arg instanceof Integer;
			final boolean isShort = arg instanceof Short;
			final boolean isByte = arg instanceof Byte;
			if (!isDouble && !isFloat && !isLong && !isInteger && !isShort && !isChar && !isByte) {
				return false;
			}
			if (primitiveType == Double.TYPE) {
				return true;
			}
			if (isDouble) {
				return false;
			}
			if (primitiveType == Float.TYPE) {
				return true;
			}
			if (isFloat) {
				return false;
			}
			if (primitiveType == Long.TYPE) {
				return true;
			}
			if (isLong) {
				return false;
			}
			if (primitiveType == Integer.TYPE) {
				return true;
			}
			int v = isChar ? ((Character) arg).charValue() : ((Number)arg).intValue();
			if (primitiveType == Character.TYPE) {
				return v >= Character.MIN_VALUE && v <= Character.MAX_VALUE;
			}
			if (primitiveType == Short.TYPE) {
				return v >= Short.MIN_VALUE&& v <= Short.MAX_VALUE;
			}
			if (primitiveType == Byte.TYPE) {
				return v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE;
			}
			return false;
		}

		private Class<?> boxedTypeFor(Class<?> potentiallyUnboxed) {
			Class<?> boxed = UNBOXED_TO_BOXED.get(potentiallyUnboxed);
			return boxed != null ? boxed : potentiallyUnboxed;
		}
	}
}
