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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
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
		@WrappedRunWith(PowerMockRunner.class)
		@PrepareForTest(using_PowerMockRunner.help.class)
		public static class using_PowerMockRunner {
			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
				builder.constructWith("foo");
				builder.constructWith("bar");
			}

			private String s;

			public using_PowerMockRunner(String s) {
				this.s = s;
			}

			public static class help {
				/**
				 * A final method that we override with powermock
				 */
				public final String get() {
					return "should be overridden";
				}
			}

			@Test
			public void test1() {
				LoggerFactory.getLogger(getClass()).info("test() s=" + s);
				help mock = PowerMockito.mock(help.class);
				PowerMockito.when(mock.get()).thenReturn("yeah");
				assertThat(mock.get(), is("yeah"));
			}

			@Test
			public void test2() {
				LoggerFactory.getLogger(getClass()).info("test() s=" + s);
				help mock = PowerMockito.mock(help.class);
				PowerMockito.when(mock.get()).thenReturn("joo");
				assertThat(mock.get(), is("joo"));
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
	
	public static class verify_successfully_constructed_suite_operation {
		@RunWith(WrappingParameterizedRunner.class)
		public static class SampleTestClass {
			@ParameterizedSuite
			public static void suite(ParameterizedSuiteBuilder builder) {
				builder.constructWith(2);
				builder.constructWith(3).named("hello");
			}
			private final long d;
			public SampleTestClass(long d) {
				this.d = d;
			}
			@Test
			public void test1() {
				assertEquals(2L, d);
			}
			@Test
			public void test2() {
				assertEquals(1L, d);
			}
		}

		@Test
		public void ensure_proper_runner_operation() throws Exception {
			WrappingParameterizedRunner runner = new WrappingParameterizedRunner(SampleTestClass.class);
			
			Description runnerDescription  = runner.getDescription();

			final String testClassName = SampleTestClass.class.getName();

			verifyDesc(runnerDescription , testClassName, testClassName, true);

			List<Description> subRunners  = runnerDescription.getChildren();
			assertEquals(2, subRunners.size());
			verifyDesc(subRunners.get(0), "0. 2", testClassName, true);
			verifyDesc(subRunners.get(1), "1. hello", testClassName, true);

			List<Description> tests1 = verifyTestDescs(testClassName, subRunners.get(0).getChildren());
			List<Description> tests2 = verifyTestDescs(testClassName, subRunners.get(1).getChildren());

			RunNotifier notifier = new RunNotifier();
			RunListener mockListener = mock(RunListener.class);
			notifier.addListener(mockListener);
			runner.run(notifier);
			
			ArgumentCaptor<Description> startCaptor = forClass(Description.class);
			ArgumentCaptor<Failure> failureCaptor  = forClass(Failure.class);
			ArgumentCaptor<Description> finishCaptor = forClass(Description.class);
			verify(mockListener, times(4)).testStarted(startCaptor .capture());
			verify(mockListener, times(3)).testFailure(failureCaptor.capture());
			verify(mockListener, times(4)).testFinished(finishCaptor .capture());
			verifyNoMoreInteractions(mockListener);
			
			List<Description> starts = startCaptor.getAllValues();
			List<Failure> failures = failureCaptor.getAllValues();
			List<Description> finishes = finishCaptor.getAllValues();

			assertEquals(Arrays.asList(tests1.get(0), tests1.get(1), tests2.get(0), tests2.get(1)), starts);
			assertEquals(tests1.get(1), failures.get(0).getDescription());
			assertEquals(tests2.get(0), failures.get(1).getDescription());
			assertEquals(tests2.get(1), failures.get(2).getDescription());
			assertEquals(Arrays.asList(tests1.get(0), tests1.get(1), tests2.get(0), tests2.get(1)), finishes);
		}

		private List<Description> verifyTestDescs(final String testClassName, List<Description> tests) {
			assertEquals(2, tests.size());
			verifyDesc(tests.get(0), "test1(" + testClassName + ")", testClassName, false);
			verifyDesc(tests.get(1), "test2(" + testClassName + ")", testClassName, false);
			return tests;
		}

		private void verifyDesc(Description desc, String displayName, String className, boolean isSuite) {
			assertEquals(displayName, desc.getDisplayName());
			assertEquals(className, desc.getClassName());
			assertEquals(isSuite, desc.isSuite());
		}
	}

	@RunWith(WrappingParameterizedRunner.class)
	public static class verify_suite_construction_failure_reporting { 
		
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

		public  verify_suite_construction_failure_reporting(Class<?> testClass) throws Exception {
			this(testClass,  (String) testClass.getDeclaredField("EXCEPTION_MESSAGE").get(null));
		}

		public  verify_suite_construction_failure_reporting(Class<?> testClass, String exceptionMessage) throws Exception {
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
