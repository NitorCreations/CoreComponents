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
package com.nitorcreations.junit.runner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class StandaloneJUnitRunner {
  static final PrintStream out = System.out;

  public void main(String[] args) {
    Result[] results = runTests(args);
    List<Failure> failures = collectFailures(results);
    if (failures.isEmpty()) {
      return;
    }
    for (Failure f : failures) {
      out.println(f.toString());
    }
    throw new RuntimeException("Tests Failed");
  }

  public Result[] runTests(String... args) {
    Class<?>[][] classSets = parseClassSets(args);
    return runTests(classSets);
  }

  public List<Failure> collectFailures(Result[] results) {
    List<Failure> failures = new ArrayList<Failure>();
    for (Result result : results) {
      if (!result.wasSuccessful()) {
        failures.addAll(result.getFailures());
      }
    }
    return failures;
  }

  private Class<?>[][] parseClassSets(String... args) {
    Set<String> seenClassNames = new HashSet<String>();
    Class<?>[][] classSets = new Class[args.length][];
    for (int i = 0; i < args.length; ++i) {
      String[] classNames = args[i].split(",");
      classSets[i] = new Class[classNames.length];
      for (int j = 0; j < classNames.length; ++j) {
        String className = classNames[j].trim();
        if (!seenClassNames.add(className)) {
          throw new RuntimeException("Duplicate class name specified: " + className);
        }
        try {
          classSets[i][j] = Class.forName(className);
        } catch (Throwable t) {
          throw new RuntimeException("Class " + className + " not available: " + t);
        }
      }
    }
    return classSets;
  }

  private Result[] runTests(final Class<?>[][] classSets) {
    Result[] results = new Result[classSets.length];
    Thread[] runners = createAndStartTestThreads(classSets, results);
    waitForTestThreadsToFinish(runners);
    return results;
  }

  private Thread[] createAndStartTestThreads(final Class<?>[][] classSets, final Result[] results) {
    Thread[] runners = new Thread[classSets.length];
    for (int i = 0; i < classSets.length; ++i) {
      final int testNum = i;
      runners[i] = new Thread("TestRunner-" + testNum) {
        @Override
        public void run() {
          JUnitCore junit = createJunit(testNum);
          results[testNum] = junit.run(classSets[testNum]);
        }
      };
      runners[i].start();
    }
    return runners;
  }

  static JUnitCore createJunit(final int testNum) {
    JUnitCore junit = new JUnitCore();
    junit.addListener(new RunListener() {
      @Override
      public void testStarted(Description description) throws Exception {
        out.printf("Thread-%d: Running: %s%n", testNum, description.getDisplayName());
      }

      @Override
      public void testFinished(Description description) throws Exception {
        out.printf("Thread-%d: Finished: %s%n", testNum, description.getDisplayName());
      }
    });
    junit.addListener(new XmlJUnitFormatter(testNum));
    return junit;
  }

  private void waitForTestThreadsToFinish(Thread[] runners) {
    for (Thread runner : runners) {
      try {
        runner.join();
      } catch (InterruptedException e) {
        // ignore interruption
      }
    }
  }
}
