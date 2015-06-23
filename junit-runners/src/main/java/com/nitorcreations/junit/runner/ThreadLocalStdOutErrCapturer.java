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

import static java.lang.System.setErr;
import static java.lang.System.setOut;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ThreadLocalStdOutErrCapturer {
  public static final ThreadLocal<CaptureStreams> stdoutCapture = new CaptureStreamThreadLocal();
  public static final ThreadLocal<CaptureStreams> stderrCapture = new CaptureStreamThreadLocal();

  public static void captureStdOutAndErrStreams() {
    setOut(new RedirectingPrintStream(stdoutCapture));
    setErr(new RedirectingPrintStream(stderrCapture));
  }

  static final class CaptureStreamThreadLocal extends InheritableThreadLocal<CaptureStreams> {
    @Override
    protected CaptureStreams initialValue() {
      return new CaptureStreams();
    }
  }

  public static class CaptureStreams {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024);
    final PrintStream writer;

    CaptureStreams() {
      try {
        writer = new PrintStream(bytes, true, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    
    public String toString() {
      try {
        return bytes.toString("UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class RedirectingPrintStream extends PrintStream {
    private ThreadLocal<CaptureStreams> capture;

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "PrintStream encoding is never used")
    public RedirectingPrintStream(ThreadLocal<CaptureStreams> capture) {
      super(new FailingOutputStream());
      this.capture = capture;
    }

    @Override
    public void println() {
      capture.get().writer.println();
    }

    @Override
    public void println(String x) {
      capture.get().writer.println(x);
    }

    @Override
    public void write(byte buf[], int off, int len) {
      capture.get().writer.write(buf, off, len);
    }

    @Override
    public PrintStream printf(String format, Object... args) {
      return capture.get().writer.printf(format, args);
    }
  }

  static class FailingOutputStream extends OutputStream {
    @Override
    public void write(int b) throws IOException {
      throw new IllegalStateException("RedirectingPrintStream did not redirect this method");
    }
  }
}
