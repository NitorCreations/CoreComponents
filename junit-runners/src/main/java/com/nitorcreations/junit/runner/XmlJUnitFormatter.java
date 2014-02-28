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

import static com.nitorcreations.junit.runner.ThreadLocalStdOutErrCapturer.captureStdOutAndErrStreams;
import static com.nitorcreations.junit.runner.ThreadLocalStdOutErrCapturer.stderrCapture;
import static com.nitorcreations.junit.runner.ThreadLocalStdOutErrCapturer.stdoutCapture;
import static com.nitorcreations.junit.runner.XmlJUnitFormatter.Status.assumptionFailure;
import static com.nitorcreations.junit.runner.XmlJUnitFormatter.Status.error;
import static com.nitorcreations.junit.runner.XmlJUnitFormatter.Status.failed;
import static com.nitorcreations.junit.runner.XmlJUnitFormatter.Status.ignored;
import static com.nitorcreations.junit.runner.XmlJUnitFormatter.Status.success;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.err;
import static java.lang.System.getProperty;
import static java.lang.System.out;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.nitorcreations.junit.runner.ThreadLocalStdOutErrCapturer.CaptureStreams;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class XmlJUnitFormatter extends RunListener {
  private final List<TestStatus> results = new ArrayList<TestStatus>();
  private final int threadNumber;
  private String testName;
  private PrintWriter xml;
  private Status status;
  private String failureTrace;
  private long testStartTime;
  private int errorCount;
  private int failureCount;
  private int skippedCount;

  private static final String outputDir;
  
  static {
    outputDir = getProperty("output.dir", ".") + File.separator;
    File dir = new File(outputDir);
    if (!dir.mkdirs() && !dir.exists()) {
      throw new RuntimeException("Failed to create output directory " + outputDir);
    }
    captureStdOutAndErrStreams();
  }
  
  enum Status {
    success(null),
    ignored("skipped"),
    error("error"),
    failed("failure"),
    assumptionFailure("skipped");
    
    public final String traceElement;

    Status(String traceElement) {
      this.traceElement = traceElement;
    }
  }

  public XmlJUnitFormatter(int threadNumber) {
    this.threadNumber = threadNumber;
  }

  static class TestStatus {
    public final String name;
    public final String classname;
    public final String stderr;
    public final String stdout;
    public final Status status;
    public final String trace;
    public final long runTime;

    public TestStatus(Description description, Status status, String trace, String stdout, String stderr, long runTime) {
      this.status = status;
      this.trace = trace;
      this.stdout = stdout;
      this.stderr = stderr;
      this.runTime = runTime;
      name = description.getMethodName();
      classname = description.getClassName();
    }
  }

  @Override
  public void testRunStarted(Description description) throws Exception {
    if ((description.getClassName() == null || description.getClassName().equals("null")) && !description.getChildren().isEmpty()) {
      testName = getProperty("test.name", "");
      if (!testName.isEmpty()) {
        testName += '-';
      }
      testName += description.getChildren().get(0).getClassName();
    } else {
      testName = getProperty("test.name", description.getClassName());
    }
    String name = "TEST-" + testName + "-" + threadNumber + ".xml";
    xml = new PrintWriter(outputDir + name, "UTF-8");
  }

  @Override
  @SuppressFBWarnings(value="UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JUnit runner calls testRunStarted before this method")
  public void testRunFinished(Result result) throws Exception {
    xml.printf("<?xml version=\"1.1\" encoding=\"UTF-8\"?>%n");
    xml.printf("<testsuite name=\"%s\" time=\"%f\" tests=\"%d\" errors=\"%d\" skipped=\"%d\" failures=\"%d\">%n", testName,
        result.getRunTime() / 1000.0, results.size(), errorCount, skippedCount, failureCount);
    for (TestStatus test : results) {
      String ignoreAttr = "";
      if (test.status == ignored) {
        ignoreAttr = " ignored=\"true\"";
      }
      xml.printf(" <testcase name=\"%s\" classname=\"%s\" time=\"%f\"%s>", test.name, test.classname,
          test.runTime / 1000.0, ignoreAttr);
      printIfPresent("system-out", test.stdout);
      printIfPresent("system-err", test.stderr);
      printIfPresent(test.status.traceElement, test.trace);
      xml.printf("</testcase>%n");
    }
    xml.print("</testsuite>");
    xml.close();
  }

  @SuppressFBWarnings(value="UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "JUnit runner calls testRunStarted before this method")
  private void printIfPresent(String element, String text) {
    if (text == null || text.isEmpty()) {
      return;
    }
    xml.printf("%n  <%s>%s</%s>", element, xmlEscape(text), element);
  }

  private static String xmlEscape(String str) {
    return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  @Override
  public void testStarted(Description description) throws Exception {
    status = success;
    failureTrace = null;
    testStartTime = currentTimeMillis();
  }

  @Override
  public void testFinished(Description description) throws Exception {
    addResult(description);
  }

  @Override
  public void testAssumptionFailure(Failure failure) {
    status = assumptionFailure;
    failureTrace = failure.getTrace();
    skippedCount++;
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    failureTrace = "ignored";
    status = ignored;
    skippedCount++;
    addResult(description);
  }
  
  @Override
  public void testFailure(Failure failure) {
    if (status == success) {
      if (failure.getException() instanceof AssertionError) {
        status = failed;
        failureCount++;
      } else {
        status = error;
        errorCount++;
      }
      failureTrace = failure.getTrace();
    }
  }

  private void addResult(Description description) {
    long duration;
    if (testStartTime > 0) {
      duration = currentTimeMillis() - testStartTime;
      testStartTime = 0;
    } else {
      duration = 0;
    }
    out.flush();
    err.flush();
    CaptureStreams outCapture = stdoutCapture.get();
    CaptureStreams errCapture = stderrCapture.get();
    results.add(new TestStatus(description, status, failureTrace, outCapture.toString(), errCapture.toString(), duration));
    outCapture.bytes.reset();
    errCapture.bytes.reset();
  }
}
