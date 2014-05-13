=============
JUnit Runners
=============

JUnit test runners for various purposes.

- NestedRunner for running plain old Java classes in nested configuration.
- NicelyParametrized for parameterized tests.

StandaloneJUnitRunner
- Avoids ant/maven/etc dependency when running your tests and generating standard xml reports.
- Built-in support for threads and explicit control and how tests assigned to them
- Supports packaging your integration tests and dependencies to a single executable jar and run it anywhere.

NestedRunner
============

Example
-------
```java
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
```

WrappingParameterizedRunner
===========================

See [/NitorCreations/CoreComponents/wiki/WrappingParameterizedRunner]

NicelyParameterized
-------------------
Deprecated. Use WrappingParameterizedRunner (above) instead.

StandaloneJUnitRunner
=====================

Usage
-----
Each main method argument is a comma separated list of junit testclass names to run in it's own thread.
For example a command line of
```text
  Class1 ClassA,ClassB Class2
```
Will start 3 threads, where ClassA and ClassB are run in sequence in one thread and Class1 and Class2 in their own threads.

Building a standalone test runner
---------------------------------
Add all your tests to src/main/java and configure the pom.xml to generate a jar file with dependencies.

```xml
    <dependency>
      <groupId>com.nitorcreations</groupId>
      <artifactId>junit-runners</artifactId>
      <version>1.2</version>
    </dependency>
    ...
      <plugin>
        <artifactId>maven-shade-plugin</artifactId>
        <version>2.2</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <finalName>${project.artifactId}</finalName>
              <filters>
                <filter>
                  <artifact>*:*</artifact>
                  <excludes>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                  </excludes>
                </filter>
              </filters>
              <shadedArtifactAttached>false</shadedArtifactAttached>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>com.nitorcreations.junit.runner.StandaloneJUnitRunnerMain</mainClass>
                </transformer>
                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                  <resource>META-INF/spring.handlers</resource>
                </transformer>
                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                  <resource>META-INF/spring.schemas</resource>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

TODO
====

- Multiple runner runner
- Moar tests!

License
=======

[Apache License 2.0](../../blob/master/junit-runners/LICENSE)
