/**
 * Copyright 2012 Nitor Creations Oy
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.junit.runners.parameterized.ParameterizedSuite;
import com.nitorcreations.junit.runners.parameterized.ParameterizedSuiteBuilder;
import com.nitorcreations.junit.runners.parameterized.WrappedRunWith;
import com.nitorcreations.junit.runners.parameterized.WrappingParameterizedRunner;

@RunWith(WrappingParameterizedRunner.class)
@WrappedRunWith(MockitoJUnitRunner.class)
public class WrappingParameterizedRunnerTest {
	private final String str;
	private final int id;

	@Mock
	private Runnable mock;

	@ParameterizedSuite
	public static void suite(ParameterizedSuiteBuilder builder) {
		builder.add().withConstructor("kala", 1);
		builder.add().withConstructor("foo", 42);
	}

	public WrappingParameterizedRunnerTest(String str, int id) {
		this.str = str;
		this.id = id;
	}

	@Test
	public void ensureStringNotNull() throws Exception {
		assertNotNull(str);
	}

	@Test
	public void ensureMockAvailable() throws Exception {
		assertNotNull(mock);
	}

	@Test
	@Ignore
	public void ensureIdAtleast15() throws Exception {
		assertTrue("Id should be at least 15, but was " + id, id >= 15);
	}
}
