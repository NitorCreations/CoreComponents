package com.nitorcreations.junit.runners.parameterized;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_5;

import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.nitorcreations.junit.runners.parameterized.ParameterizedSuiteBuilder.TestInstantiatorBuilder;

/**
 * This runner supports wrapping most regular junit runners while adding the
 * logical functionality of {@link Parameterized}.
 * <p>
 * Example usage:
 * 
 * <pre>
 * &#64;RunWith(WrappingParameterizedRunner.class)
 * &#64;WrappedRunWith(SpringJUnit4ClassRunner.class)
 * &#64;ContextConfiguration(...) // used by SpringJUnit4ClassRunner
 * public class MyTest {
 *   &#64;ParameterizedSuite
 *   public static void suite(ParameterizedSuiteBuilder builder) {
 *     builder.add().withConstructor("foo").withDescription("try foo");
 *     builder.add().withConstructor("bar").withDescription("try bar");
 *   }
 *   
 *   private String str;
 *   
 *   public MyTest(String str) {
 *     this.str = str;
 *   }
 *   
 *   &#64;Test
 *   public void testSomething( ) {
 *     ...
 *   }
 *   &#64;Test
 *   public void testSomethingElse() {
 *     ...
 *   }
 * }
 * </pre>
 * 
 * The withConstructor() calls validates constructor parameters in-place so you
 * get immediate feedback if you supply the wrong parameters.
 * <p>
 * The withDescription() calls provides a description of the test set, seen in
 * the test tree:
 * <ul>
 * <li>MyTest
 * <ul>
 * <li>try foo
 * <ul>
 * <li>testSomething()</li>
 * <li>testSomethingElse()</li>
 * </ul>
 * </li>
 * <li>try bar
 * <ul>
 * <li>testSomething()</li>
 * <li>testSomethingElse()</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 */
public class WrappingParameterizedRunner extends Runner {

	private static final class FailingRunner extends Runner {
		private final Description description;
		private final RuntimeException exception;

		FailingRunner(Description description,
				RuntimeException exception) {
			this.description = description;
			this.exception = exception;
		}

		@Override
		public void run(RunNotifier notifier) {
			notifier.fireTestFailure(new Failure(getDescription(), exception));
		}

		@Override
		public Description getDescription() {
			return description;
		}
	}

	private final Class<?> testClass;
	private final DescriptionMapper descriptionMapper;
	private final Constructor<? extends Runner> wrappedRunnerConstructor;
	private Description description;
	private Runner[] runners;
	private String[] testDescriptions;
	private Exception totalFailure;

	public WrappingParameterizedRunner(Class<?> testClass)
			throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			ClassNotFoundException {
		if (Modifier.isFinal(testClass.getModifiers())) {
			throw new IllegalArgumentException(
					"Cannot operate on test classes marked final: " + testClass);
		}
		this.testClass = testClass;
		descriptionMapper = new DescriptionMapper(testClass);
		Class<? extends Runner> wrappedRunnerClass = JUnit4.class;
		WrappedRunWith wrappedRunWith = testClass
				.getAnnotation(WrappedRunWith.class);
		if (wrappedRunWith != null) {
			wrappedRunnerClass = wrappedRunWith.value();
		}
		this.wrappedRunnerConstructor = wrappedRunnerClass
				.getConstructor(Class.class);
	}

	/**
	 * Initialize the runner so it can provide a description and run the
	 * contained tests.
	 */
	private void ensureSetup() {
		if (runners != null) {
			return;
		}
		try {
			List<TestInstantiatorBuilder> tests = fetchTests();

			runners = new Runner[tests.size()];
			testDescriptions = new String[runners.length];
			int i = 0;
			for (TestInstantiatorBuilder test : tests) {
				String errorMsg = "Failed to get test descriptions";
				try {
					testDescriptions[i] = test.getDescription();
					errorMsg = "Failed to create test class";
					Class<?> parameterizedTestClass = createParameterizedTestClass(i, test);
					errorMsg = "Failed to create runner for test";
					runners[i] = wrappedRunnerConstructor
							.newInstance(parameterizedTestClass);
				} catch (ParameterizationStrategyNotAvailableException e) {
					throw e;
				} catch (Exception e) {
					final RuntimeException exception = new RuntimeException(errorMsg, e);
					if (testDescriptions[i] == null) {
						testDescriptions[i] = "Test " +i;
					}
					final Description dummyDescription = Description.createSuiteDescription("Dummy  " + (System.identityHashCode(this) + i));
					runners[i] = new FailingRunner(dummyDescription, exception);
				}
				++i;
			}
		} catch (RuntimeException e) {
			totalFailure = e;
			runners = null;
			testDescriptions = null;
		}
	}

	/**
	 * Fetch the tests to run by calling the method annotated with @
	 * {@link ParameterizedSuite}.
	 * 
	 * @return list of tests to run
	 */
	private List<TestInstantiatorBuilder> fetchTests() {
		Method foundMethod = getParameterizedSuiteMethod();
		ParameterizedSuiteBuilder builder = new ParameterizedSuiteBuilder(
				testClass);
		try {
			foundMethod.invoke(null, builder);
		} catch (Throwable t) {
			throw new RuntimeException(
					"Failed to build parameterized suite for "
							+ testClass.getName(), t);
		}
		List<TestInstantiatorBuilder> tests = builder.getTests();
		return tests;
	}

	/**
	 * Find the single public static method in test class that is annotated with @
	 * {@link ParameterizedSuite}.
	 */
	private Method getParameterizedSuiteMethod() {
		Method foundMethod = null;
		for (Method method : testClass.getMethods()) {
			if (null == method.getAnnotation(ParameterizedSuite.class)) {
				continue;
			}
			if (foundMethod == null) {
				foundMethod = method;
			} else {
				throw new IllegalArgumentException("Class "
						+ testClass.getName()
						+ " contains multiple methods annotated with @"
						+ ParameterizedSuite.class.getSimpleName()
						+ ", expected exactly one");
			}
		}
		if (foundMethod == null) {
			throw new IllegalArgumentException("Class " + testClass.getName()
					+ " must have a static method annotated with @"
					+ ParameterizedSuite.class.getSimpleName());
		}
		if (!Modifier.isStatic(foundMethod.getModifiers())) {
			throw new IllegalArgumentException("Class " + testClass.getName()
					+ " method " + foundMethod + " must be static");
		}
		Class<?>[] parameterTypes = foundMethod.getParameterTypes();
		if (parameterTypes.length != 1
				|| parameterTypes[0] != ParameterizedSuiteBuilder.class) {
			throw new IllegalArgumentException("Class " + testClass.getName()
					+ " method " + foundMethod
					+ " must take exactly one argument of type "
					+ ParameterizedSuiteBuilder.class.getSimpleName());
		}
		return foundMethod;
	}

	/**
	 * Create new class that extends the test class and has a single public
	 * no-argument constructor that calls the wanted superclass (= test class)
	 * constructor with wanted arguments. This fulfills the "normal" fingerprint
	 * expected by regular test runners.
	 * 
	 * @param testIdx
	 *            the test index (in the parameterized set)
	 * @param test
	 *            the test instantiator instance which contains the constructor
	 *            to use and arguments to call it with.
	 * @return newly created class extending the test class.
	 */
	private Class<?> createParameterizedTestClass(int testIdx,
			TestInstantiatorBuilder test) {
		ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		String testClassNameRaw = Type.getInternalName(testClass);
		String extendingTestClassName = testClass.getName() + '_' + testIdx;
		String extendingTestClassNameRaw = testClassNameRaw + '_' + testIdx;

		ParameterizationStrategy strategy = chooseParameterizationStrategy();

		// create a new class that extends the test class
		cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, extendingTestClassNameRaw, null,
				testClassNameRaw, null);

		// let the strategy create any fields and annotations necessary
		strategy.classCreationInProgress(extendingTestClassNameRaw, cw);

		// create no-arg constructor expected by "normal" runners
		// calls the superclass (e.g. original test class) constructor with the
		// parameters for this test)
		MethodVisitor mvo = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null,
				null);
		GeneratorAdapter mv = new GeneratorAdapter(mvo, ACC_PUBLIC, "<init>",
				"()V");
		mv.visitCode();
		Label label = new Label();
		mv.visitLabel(label);
		mv.visitLineNumber(1, label);
		// ask strategy to emit the bytecode to load the Object[] containing the
		// constructor args for this test
		strategy.loadConstructorArgs(mv);
		mv.visitVarInsn(ASTORE, 1);
		mv.loadThis();
		Type constructorType = Type.getType(test.getConstructor());
		Type[] argumentTypes = constructorType.getArgumentTypes();
		for (int arg = 0; arg < test.getConstructorArgs().length; ++arg) {
			Type argType = argumentTypes[arg];
			mv.visitVarInsn(ALOAD, 1);
			mv.push(arg);
			mv.visitInsn(AALOAD);
			mv.unbox(argType);
		}
		String constructorArgsRaw = constructorType.getDescriptor();
		mv.visitMethodInsn(INVOKESPECIAL, testClassNameRaw, "<init>",
				constructorArgsRaw);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0); // dummy values
		mv.visitEnd();

		// done, convert class to byte array
		cw.visitEnd();
		byte[] clazzBytes = cw.toByteArray();

		/* ** DEBUG export class to /tmp/x.class ** */
		try {
			FileOutputStream fos = new FileOutputStream("/tmp/x.class");
			fos.write(clazzBytes);
			fos.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		/* ** /DEBUG ** */

		// inject the new class into the current classloader
		Class<?> clazz = ClassLoaderInjector.injectClass(
				extendingTestClassName, clazzBytes);
		// let the strategy complete initialization of the newly created class
		strategy.classConstructed(clazz, clazzBytes, test.getConstructorArgs());
		return clazz;
	}

	private ParameterizationStrategy chooseParameterizationStrategy() {
		if (wrappedRunnerConstructor.getDeclaringClass().getPackage().getName()
				.contains("powermock")) {
			return new PowermockParameterizationStrategy();
		}
		return new DefaultParameterizationStrategy();
	}

	@Override
	public synchronized Description getDescription() {
		if (description == null) {
			ensureSetup();
			Description d = Description.createSuiteDescription(testClass);
			if (totalFailure == null) {
				for (int i = 0; i < runners.length; ++i) {
					String testDescription = i + ". " + testDescriptions[i];
					Runner runner = runners[i];
					d.addChild(descriptionMapper.rewriteDescription(
							testDescription, runner.getDescription()));
				}
			}
			description = d;
		}
		return description;
	}

	@Override
	public synchronized void run(RunNotifier notifier) {
		getDescription();
		if (totalFailure != null) {
			notifier.fireTestFailure(new Failure(getDescription(), totalFailure));
		} else {
			notifier = new DescriptionMappingRunNotifierProxy(notifier,
					descriptionMapper);
			for (Runner runner : runners) {
				runner.run(notifier);
			}
		}
	}
}
