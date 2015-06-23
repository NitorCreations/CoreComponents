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

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

/**
 * This is a helper class for {@link WrappingParameterizedRunner}. It rewrites
 * descriptions objects given to this instance and passes the rewritten
 * description objects on to the given targetNotifier instance.
 */
class DescriptionMappingRunNotifierProxy extends RunNotifier {
	DescriptionMappingRunNotifierProxy(final RunNotifier targetNotifier,
			final DescriptionMapper descriptionMapper) {
		addListener(new RunListener() {
			@Override
			public void testAssumptionFailure(Failure failure) {
				targetNotifier.fireTestAssumptionFailed(mapFailure(failure));
			}

			@Override
			public void testFailure(Failure failure) throws Exception {
				targetNotifier.fireTestFailure(mapFailure(failure));
			}

			@Override
			public void testFinished(Description description) throws Exception {
				targetNotifier.fireTestFinished(mapDescription(description));
			}

			@Override
			public void testIgnored(Description description) throws Exception {
				targetNotifier.fireTestIgnored(mapDescription(description));
			}

			@Override
			public void testStarted(Description description) throws Exception {
				targetNotifier.fireTestStarted(mapDescription(description));
			}

			private Failure mapFailure(Failure failure) {
				return new Failure(mapDescription(failure.getDescription()),
						failure.getException());
			}

			private Description mapDescription(Description orig) {
				return descriptionMapper.map(orig);
			}
		});
	}
}
