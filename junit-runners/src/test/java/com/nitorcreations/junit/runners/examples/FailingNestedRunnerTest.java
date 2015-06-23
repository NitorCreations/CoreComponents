/**
 * Copyright 2013 Nitor Creations Oy
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
