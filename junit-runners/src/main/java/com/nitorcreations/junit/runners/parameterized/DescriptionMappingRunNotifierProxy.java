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
