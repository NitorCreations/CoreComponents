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
    setProperty("output.dir", "target" + File.separator + "testrunner");
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
