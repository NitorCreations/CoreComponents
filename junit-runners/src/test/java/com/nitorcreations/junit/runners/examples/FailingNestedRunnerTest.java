package com.nitorcreations.junit.runners.examples;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import com.nitorcreations.junit.runners.NestedRunner;

public class FailingNestedRunnerTest {

	@Test
	public void ShouldFail() {
		Result result = JUnitCore.runClasses(Parent.class);
		assertThat(result.getFailureCount(), is(1));
		assertTrue(result.getFailures().get(0).getException().getClass() == RuntimeException.class);
	}

	@RunWith(NestedRunner.class)
	public static class Parent {
		public class Failing {
			public Failing() {
				throw new RuntimeException();
			}

			@Before
			public void setup() {
			}

			@Test
			public void isNeverExecuted() {
			}

		}
	}

}
