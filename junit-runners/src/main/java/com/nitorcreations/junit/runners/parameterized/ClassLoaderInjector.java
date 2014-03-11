package com.nitorcreations.junit.runners.parameterized;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ClassLoaderInjector {

	private static final Method DEFINE_CLASS_METHOD;
	static {
		DEFINE_CLASS_METHOD = getDefineClassMethod();
	}

	private static Method getDefineClassMethod() {
		try {
			Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
					"defineClass", new Class[] { String.class, byte[].class,
							int.class, int.class });
			defineClassMethod.setAccessible(true);
			return defineClassMethod;
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Inject a new class into the current context classloader.
	 * 
	 * @param name
	 *            the name of the class "com.foo.Bar"
	 * @param clazzBytes
	 *            the class in byte array format
	 * @return a newly loaded class
	 */
	public static Class<?> injectClass(String name, byte[] clazzBytes) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		return injectClass(name, clazzBytes, cl);
	}

	/**
	 * Inject a new class into a classloader.
	 * 
	 * @param name
	 *            the name of the class "com.foo.Bar"
	 * @param clazzBytes
	 *            the class in byte array format
	 * @param classLoader
	 *            the classloader to inject the class into
	 * @return a newly loaded class
	 */
	public static Class<?> injectClass(String name, byte[] clazzBytes,
			ClassLoader classLoader) {
		try {
			return (Class<?>) DEFINE_CLASS_METHOD.invoke(classLoader, name,
					clazzBytes, 0, clazzBytes.length);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
