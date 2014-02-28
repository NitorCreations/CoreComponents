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
