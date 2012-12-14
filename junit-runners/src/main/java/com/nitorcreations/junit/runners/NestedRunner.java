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
package com.nitorcreations.junit.runners;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class NestedRunner extends ParentRunner<Object> {
    private final List<Object> children = new ArrayList<Object>();
    private final NestedClassRunner delegatedRunner;
    private final NestedRunner parentRunner;

    public NestedRunner(Class<?> testClass) throws InitializationError {
        this(testClass, null);
    }

    public NestedRunner(Class<?> testClass, NestedRunner parentRunner) throws InitializationError {
        super(testClass);
        this.parentRunner = parentRunner;
        delegatedRunner = new NestedClassRunner(testClass);
        children.addAll(delegatedRunner.giveMeTheDamnChildren());
        addToChildrenAllNestedClassesWithTestsAndTestMethods(testClass);
    }

    private void addToChildrenAllNestedClassesWithTestsAndTestMethods(Class<?> testClass) throws InitializationError {
        for (Class<?> child : testClass.getDeclaredClasses()) {
            if (containsTests(child)) {
                children.add(new NestedRunner(child, this));
            }
        }
    }

    private boolean containsTests(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(Test.class) != null) {
                return true;
            }
        }
        return childrenContainTests(clazz);
    }

    public boolean childrenContainTests(Class<?> clazz) {
        for (Class<?> klazz : clazz.getDeclaredClasses()) {
            if (containsTests(klazz)) {
                return true;
            }
        }
        return false;
    }

    protected List<Object> getChildren() {
        return children;
    }

    @Override
        public String getName() {
        return getTestClass().getJavaClass().getSimpleName();
    }

    protected Description describeChild(Object child) {
        if (child instanceof Runner) {
            return ((Runner)child).getDescription();
        } else {
            return delegatedRunner.callTheProtectedDescribeChild((FrameworkMethod)child);
        }
    }

    protected void runChild(Object child, RunNotifier notifier) {
        if (child instanceof Runner) {
            ((Runner)child).run(notifier);
        } else {
            delegatedRunner.callThePrivateRunChild((FrameworkMethod)child, notifier);
        }
    }

    private Object constructTestClass() throws Exception {
        if (getTestClass().getOnlyConstructor().getParameterTypes().length == 1) {
            if (parentRunner != null) {
                Object parent = parentRunner.constructTestClass();
                Object newInstance = getTestClass().getOnlyConstructor().newInstance(parent);
                delegatedRunner.currentTestObject = newInstance;
                return newInstance;
            }
        }
        Object newInstance = getTestClass().getOnlyConstructor().newInstance();
        delegatedRunner.currentTestObject = newInstance;
        return newInstance;
    }

    public List<FrameworkMethod> getBefores() {
        List<FrameworkMethod> befores = new ArrayList<FrameworkMethod>();
        befores.addAll(getTestClass().getAnnotatedMethods(Before.class));
        return befores;
    }

    public List<FrameworkMethod> getAfters() {
        List<FrameworkMethod> afters = new ArrayList<FrameworkMethod>();
        afters.addAll(getTestClass().getAnnotatedMethods(After.class));
        return afters;
    }

    private Statement withParentBefores(Statement statement) {
        if (parentRunner != null) {
            return parentRunner.withParentBefores(new RunBefores(statement, getBefores(), delegatedRunner.currentTestObject));
        }
        return new RunBefores(statement, getBefores(), delegatedRunner.currentTestObject);
    }

    private Statement withParentAfters(Statement statement) {
        if (parentRunner != null) {
            return new RunAfters(parentRunner.withParentAfters(statement), getAfters(), delegatedRunner.currentTestObject);
        }
        return new RunAfters(statement, getAfters(), delegatedRunner.currentTestObject);
    }

    private class NestedClassRunner extends BlockJUnit4ClassRunner {
        private Object currentTestObject;

        public NestedClassRunner(Class<?> childClass) throws InitializationError {
            super(childClass);
        }

        public void callThePrivateRunChild(FrameworkMethod child, RunNotifier notifier) {
            runChild(child, notifier);
        }

        public Description callTheProtectedDescribeChild(FrameworkMethod child) {
            return describeChild(child);
        }

        public Collection<? extends Object> giveMeTheDamnChildren() {
            return super.getChildren();
        }

        protected void validateConstructor(List<Throwable> errors) {
            validateOnlyOneConstructor(errors);
            validateNonStaticInnerClassWithDefaultConstructor(errors);
        }

        private void validateNonStaticInnerClassWithDefaultConstructor(List<Throwable> errors) {
            try {
                getTestClass().getJavaClass().getConstructor(NestedRunner.this.getTestClass().getJavaClass());
            } catch (NoSuchMethodException e) {
                String gripe = "Nested test classes should be non-static and have a public zero-argument constructor";
                errors.add(new Exception(gripe));
            }
        }

        protected Object createTest() throws Exception {
            return constructTestClass();
        }

        protected Statement methodBlock(FrameworkMethod method) {
            Statement statement = super.methodBlock(method);
            statement = withParentBefores(statement);
            statement = withParentAfters(statement);
            return statement;
        }

        //Disable withBefores so it won't collide with our @Before handler
        @Override
        protected Statement withBefores(FrameworkMethod method, Object target,
                Statement statement) {
            return new RunBefores(statement,
                    new ArrayList<FrameworkMethod>(), target);
        }
    
        //Disable withAfters so it won't collide with our @After handler
        @Override
        protected Statement withAfters(FrameworkMethod method, Object target,
                Statement statement) {
            return new RunAfters(statement,
                    new ArrayList<FrameworkMethod>(), target);
        }

        @Override
        protected void collectInitializationErrors(List<Throwable> errors) {
            validatePublicVoidNoArgMethods(BeforeClass.class, true, errors);
            validatePublicVoidNoArgMethods(AfterClass.class, true, errors);
            // we have to remove this check because we want non-static classes to work
            //validateClassRules(errors);
        }
    }
}
