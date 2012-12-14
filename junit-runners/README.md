=============
JUnit Runners
=============

JUnit runners for various purposes.

- NestedRunner for running plain old Java classes in nested configuration.
- NicelyParametrized for parameterized tests.

NestedRunner
============

Example
-------

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

TODO
----

- Nested @Before doesn't work
- Multiple runner runner
- Moar tests!


NicelyParameterized
-------------------


License
=======

[Apache License 2.0](CoreComponents/tree/master/LICENSE)
