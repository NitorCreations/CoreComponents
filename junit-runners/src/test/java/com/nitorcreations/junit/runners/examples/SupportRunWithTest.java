package com.nitorcreations.junit.runners.examples;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.builders.JUnit3Builder;
import org.junit.matchers.JUnitMatchers;
import org.junit.runner.JUnitCore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import com.nitorcreations.junit.runners.NestedRunner;

public class SupportRunWithTest {
	private boolean testRunnerWasUsed = false;

	@Test
	public void InnerTestShouldBeRunWithTestRunner() {
		Result result = JUnitCore.runClasses(Parent.class);
		assertTrue(result.wasSuccessful());
		assertThat(result.getRunCount(), is(1));
		assertTrue(testRunnerWasUsed);
	}

	public class TestRunner extends Runner {
		public TestRunner(Class<?> klazz) {
		}

		@Override
		public void run(RunNotifier notifier) {
			testRunnerWasUsed = true;
		}

		@Override
		public Description getDescription() {
			return Description.createSuiteDescription("test");
		}
	}

	@RunWith(NestedRunner.class)
	public static class Parent {
		@RunWith(TestRunner.class)
		public class Inner {
			@Test
			public void isRunWithBlockJUnit4ClassRunner() {
			}

		}
	}
}
