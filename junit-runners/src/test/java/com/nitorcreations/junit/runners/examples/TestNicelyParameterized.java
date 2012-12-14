package com.nitorcreations.junit.runners.examples;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import com.nitorcreations.junit.runners.NicelyParameterized;

@RunWith(NicelyParameterized.class)
public class TestNicelyParameterized {

	private final String str;
	private final int id;

	@Parameters
	public static List<Object[]> data() {
		return Arrays.asList(new Object[][] { { "kala", 1 }, { "foo", 42 } });
	}

	public TestNicelyParameterized(String str, int id) {
		this.str = str;
		this.id = id;
	}

	@Test
	public void ensureStringNotNull() throws Exception {
		assertNotNull(str);
	}

	@Test
	public void ensureIdAtleast15() throws Exception {
		assertTrue("Id should be at least 15, but was " + id, id >= 15);
	}
}
