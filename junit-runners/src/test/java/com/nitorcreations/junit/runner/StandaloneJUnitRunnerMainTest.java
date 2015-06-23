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

import static java.lang.System.setProperty;
import static java.lang.System.setSecurityManager;

import java.io.File;
import java.security.Permission;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StandaloneJUnitRunnerMainTest {
  public class StopExitSecurityManager extends SecurityManager {
    @Override
    public void checkExit(int status) {
      super.checkExit(status);
      throw new SecurityException("Exit disabled: " + status);
    }
    
    @Override
    public void checkPermission(Permission perm) {
    }
  }
  
  @Rule
  public ExpectedException expect = ExpectedException.none();
  
  @Before
  public void disableSystemExit() {
    setProperty("output.dir", System.getProperty("user.dir") + File.separator + "target" + File.separator + "testrunner");
    setSecurityManager(new StopExitSecurityManager());
  }
  
  @After
  public void resetSecurityManager() {
    setSecurityManager(null);
  }
  
  @Test
  public void testUsage() {
    expect.expectMessage("Exit disabled: 1");
    StandaloneJUnitRunnerMain.main();
  }
  
  @Test
  public void testFailingTests() {
    expect.expectMessage("Exit disabled: 1");
    StandaloneJUnitRunnerMain.main(SimpleTestNotForMaven.class.getName());
  }

  @Test
  public void testSuccess() {
    expect.expectMessage("Exit disabled: 0");
    StandaloneJUnitRunnerMain.main(SuccessTestNotForMaven.class.getName());
  }
}
