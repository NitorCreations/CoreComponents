/**
 * Copyright 2012 Nitor Creations Oy
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
package com.nitorcreations.junit.runners.examples;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nitorcreations.junit.runners.NestedRunner;

@RunWith(NestedRunner.class)
public class ListTest {
	
	// inner class for sharing common context
	public class WithArrayList {
    	// some context for these tests
		ArrayList<String> list = new ArrayList<String>();

	    public class WhenEmpty {
	    	@Test
	    	public void itIsEmpty() {
	    		assertTrue(list.isEmpty());
	    	}
	    	
	        public class AfterAddingAnElement {
	        	// some more context for these tests
	        	String element = "Element";
	        	// you can use instance initializer to initialize your context
	        	// it will be run once per test
	        	{	        		
	        		// the list is still empty in here
	        		assertTrue(list.isEmpty());
					list.add(element);
	        	}
	        	
	            @Test
	            public void itIsNotEmpty() {
	                assertFalse(list.isEmpty());
	            }
	            
	            @Test
	            public void itContainsTheElement() {      	
	            	assertTrue(list.contains(element));
	            }
	            
	            @Test
	            public void addingAnotherElementIncreasesSize() {
	            	int sizeBeforeAdding = list.size();
	            	list.add("AnotherElement");
	            	assertThat(list.size(), is(greaterThan(sizeBeforeAdding)));	            	
	            }
	            
	            @Test
	            public void listSizeIsStillOne() {
	            	assertThat(list.size(), is(equalTo(1)));
	            	
	            }
	        }
	        
	        @Test
	        public void isStillEmpty() {
	    		assertTrue(list.isEmpty());      	
	        }
	    }
	    
	    public class WithTwoElements {
	    	@Before
	    	public void init() {
	    		list.add("Element1");
	    		list.add("Element2");
	    	}
	    	
	    	@Test
	    	public void hasSizeOfTwo() {
	    		assertThat(list.size(), is(equalTo(2)));
	    	}
	    }
	}
}
