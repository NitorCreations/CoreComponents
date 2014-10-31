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
package com.nitorcreations.junit.rules;

import static org.junit.Assume.assumeTrue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Fails all tests in the same test class after the first failure has been detected.
 *
 * Normally you should request deterministic ordering of test methods by annotating the test class with
 * {@link org.junit.FixMethodOrder}.
 */
public class SkipTestMethodsAfterFirstFailureRule implements TestRule {
  static final ConcurrentMap<Class<?>, String> failures = new ConcurrentHashMap<Class<?>, String>();
  final Class<?> testClass;

  public SkipTestMethodsAfterFirstFailureRule(Class<?> testClass) {
    this.testClass = testClass;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        String failedTestName = failures.get(testClass);
        if (failedTestName != null) {
          assumeTrue("Previous test '" + failedTestName + "' failed", false);
        }
        boolean ok = false;
        try {
          base.evaluate();
          ok = true;
        } finally {
          if (!ok) {
            failures.put(testClass, description.getDisplayName());
          }
        }
      }
    };
  }
}
