/**
 * Copyright 2013-2014 Nitor Creations Oy
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

import static java.lang.System.exit;

import java.io.PrintStream;

public class StandaloneJUnitRunnerMain {
  public static void main(String... args) {
    PrintStream err = System.err;
    if (args.length == 0) {
      err.println("Usage:");
      err.println("  each argument is a comma separated list of junit testclass names to run in it's own thread");
      err.println("Example:");
      err.println("  Class1 ClassA,ClassB Class2");
      err.println("  Will start 3 threads, where ClassA and ClassB are run in sequence in one thread");
      err.println("System properties:");
      err.println("  -Doutput.dir\tOutput directory for results");
      exit(1);
    }
    try {
      new StandaloneJUnitRunner().main(args);
    } catch (Throwable t) {
      err.println(t.getMessage());
      exit(1);
    }
    exit(0);
  }
}
