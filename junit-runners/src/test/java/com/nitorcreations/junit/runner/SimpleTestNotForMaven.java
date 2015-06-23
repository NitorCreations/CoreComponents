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
package com.nitorcreations.junit.runner;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import org.junit.Ignore;
import org.junit.Test;

public class SimpleTestNotForMaven {
  @Test
  public void sucess() {
  }
  
  @Test
  public void error() {
    throw new RuntimeException("Testing exception reporting ]]> \"><");
  }

  @Test
  public void failed() {
    fail("Testing assert failure ]]> \"><");
  }
  
  @Test
  public void assumeFailed() {
    assumeTrue("Testing assume failure ]]> \"><", false);
  }

  @Test
  @Ignore
  public void ignore() {
    throw new RuntimeException("This is never thrown ]]> \"><");
  }
}
