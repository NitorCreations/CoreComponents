package com.nitorcreations.junit.runners.parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.Serializable;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(Enclosed.class)
public class WrappingParameterizedRunnerTest {

	@RunWith(Enclosed.class)
	public static class different_runners {
		/**
		 * JUnit4 is the default JUnit runner, so this should behave exactly as
		 * the normal Parameterized runner.
		 */
		@RunWith(WrappingParameterizedRunner.class)
		@WrappedRunWith(JUnit4.class)
		public static class using_JUnit4 {
			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
				builder.constructWith("foo");
				builder.constructWith("bar");
			}

			private String s;

			public using_JUnit4(String s) {
				this.s = s;
			}

			@Test
			public void test1() {
				LoggerFactory.getLogger(getClass()).info("test1() s=" + s);
			}

			@Test
			public void test2() {
				LoggerFactory.getLogger(getClass()).info("test2() s=" + s);
			}
		}

		/**
		 * Test using default runner, which is JUnit4.
		 */
		@RunWith(WrappingParameterizedRunner.class)
		public static class using_default_runner {
			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
				builder.constructWith("foo");
			}

			private String s;

			public using_default_runner(String s) {
				this.s = s;
			}

			@Test
			public void test() {
				LoggerFactory.getLogger(getClass()).info("test() s=" + s);
			}
		}

		@RunWith(WrappingParameterizedRunner.class)
		@WrappedRunWith(MockitoJUnitRunner.class)
		public static class using_MockitoJUnitRunner {
			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
				builder.constructWith("foo");
				builder.constructWith("bar");
			}

			private String s;

			@Mock
			private Runnable mock;

			public using_MockitoJUnitRunner(String s) {
				this.s = s;
			}

			@Test
			public void test1() {
				LoggerFactory.getLogger(getClass()).info("test1() s=" + s);
			}

			@Test
			public void testMockExists() {
				assertNotNull(mock);
			}

			@Test
			public void test2() {
				LoggerFactory.getLogger(getClass()).info("test2() s=" + s);
			}
		}

		@RunWith(WrappingParameterizedRunner.class)
		@WrappedRunWith(SpringJUnit4ClassRunner.class)
		@ContextConfiguration(classes = { using_SpringJUnit4ClassRunner.TestConfig.class })
		public static class using_SpringJUnit4ClassRunner {

			public interface MyInterface {
				String transform(String s);
			}

			@Configuration
			public static class TestConfig {
				@Bean
				MyInterface myInterface() {
					return new MyInterface() {
						public String transform(String s) {
							return s.toLowerCase() + s.toUpperCase();
						}
					};
				}
			}

			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
				builder.constructWith("foo");
				builder.constructWith("Bar");
			}

			@Autowired
			private MyInterface myInterface;

			private String s;

			public using_SpringJUnit4ClassRunner(String s) {
				this.s = s;
			}

			@Test
			public void test1() {
				LoggerFactory.getLogger(getClass()).info(
						"test1() s=" + s + " transformed="
								+ myInterface.transform(s));
			}

			@Test
			public void test2() {
				LoggerFactory.getLogger(getClass()).info(
						"test2() s="
								+ s
								+ " transformed²="
								+ myInterface.transform(myInterface
										.transform(s)));
			}
		}
	}

	@RunWith(WrappingParameterizedRunner.class)
	@WrappedRunWith(JUnit4.class)
	public static class features {
		private String s;
		private final Integer io;
		private final double f;
		private final Object x;
		private final byte b;

		@ParameterizedSuite
		public static void suite(ParameterizedSuiteBuilder builder) {
			builder.constructWith("foo", 2, 3.2f, new Object(), (byte) 200)
					.named("Hello 1");
			builder.constructWith("bar", 4, 5.0f, "kala", (byte) 5);
			builder.constructWith(null, null, 5.0f, null, (byte) 7)
					.named("Yoo");
		}

		public features(String s, Integer io, float f, Object x, byte b) {
			this.s = s;
			this.f = f;
			this.io = io;
			this.x = x;
			this.b = b;
		}

		@Test
		public void test() {
			LoggerFactory.getLogger(getClass()).info(
					"test() s=" + s + " " + io + " " + f + " " + x + " " + b);
		}

		@Test
		public void test2() {
			LoggerFactory.getLogger(getClass()).info("test2() " + s);
		}
	}

	@RunWith(WrappingParameterizedRunner.class)
	@WrappedRunWith(JUnit4.class)
	public static class constructor_variants {

		@ParameterizedSuite
		public static void suite(ParameterizedSuiteBuilder builder) {
			builder.constructWith("foo", "");
			builder.constructWith("foo", new Object());
			builder.constructWith(new Serializable() {
				private static final long serialVersionUID = -2542951035340069284L;
			}, "");
			builder.constructWith(new Serializable() {
				private static final long serialVersionUID = -3956496975645452784L;
			}, new Object());
			builder.constructWith(new Object(), "x");
		}

		public constructor_variants(Object s, Object o) {
			LoggerFactory.getLogger(getClass()).info("wrong");
		}

		public constructor_variants(Serializable s, String o) {
			LoggerFactory.getLogger(getClass()).info("success2");
		}

		public constructor_variants(String s, String o) {
			LoggerFactory.getLogger(getClass()).info("success0");
		}

		public constructor_variants(Serializable s, Object o) {
			LoggerFactory.getLogger(getClass()).info("success3");
		}

		public constructor_variants(String s, Object o) {
			LoggerFactory.getLogger(getClass()).info("success1");
		}

		public constructor_variants(Object s, CharSequence o) {
			LoggerFactory.getLogger(getClass()).info("success4");
		}

		@Test
		public void test() {
		}
	}

	@RunWith(WrappingParameterizedRunner.class)
	public static class setup_failures { 
		
		@RunWith(WrappingParameterizedRunner.class)
		public static class constructor_failure {
			static final String EXCEPTION_MESSAGE = "A01";
			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
				throw new RuntimeException(EXCEPTION_MESSAGE);
			}
		}

		static final String STATIC_EXCEPTION_MESSAGE = "456";
		@RunWith(WrappingParameterizedRunner.class)
		public static class static_block_failure {
			static {
				if (true) {
					throw new RuntimeException(STATIC_EXCEPTION_MESSAGE);
				}
			}
			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
			}
		}

		@RunWith(WrappingParameterizedRunner.class)
		public static class constructor_parameter_toString_failure {
			static final String EXCEPTION_MESSAGE = "123";
			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
				builder.constructWith(new Object() {
					@Override
					public String toString() {
						throw new RuntimeException(EXCEPTION_MESSAGE);
					}
				});
			}
			public constructor_parameter_toString_failure(Object object) { }
		}

		@ParameterizedSuite
		public static void suite(ParameterizedSuiteBuilder builder) {
			builder.constructWith(constructor_failure.class);
			builder.constructWith(static_block_failure.class, STATIC_EXCEPTION_MESSAGE);
			builder.constructWith(constructor_parameter_toString_failure.class);
		}

		private final Class<?> testClass;
		private final String exceptionMessage;

		public  setup_failures(Class<?> testClass) throws Exception {
			this(testClass,  (String) testClass.getDeclaredField("EXCEPTION_MESSAGE").get(null));
		}

		public  setup_failures(Class<?> testClass, String exceptionMessage) throws Exception {
			this.testClass = testClass;
			this.exceptionMessage = exceptionMessage;
		}

		@Test
		public void ensure_failure() throws Exception {
			WrappingParameterizedRunner runner = new WrappingParameterizedRunner(testClass);
			RunNotifier notifier = new RunNotifier();
			RunListener mockListener = mock(RunListener.class);
			notifier.addListener(mockListener );
			runner.run(notifier);
			ArgumentCaptor<Failure> failureCaptor  = forClass(Failure.class);
			verify(mockListener).testFailure(failureCaptor.capture());
			verifyNoMoreInteractions(mockListener);
			Failure failure = failureCaptor.getValue();
			Throwable e = failure.getException();
			while (e.getCause() != null) {
				e = e.getCause();
			}
			assertEquals(RuntimeException.class, e.getClass());
			assertEquals(exceptionMessage, e.getMessage());
		}
	}
}
