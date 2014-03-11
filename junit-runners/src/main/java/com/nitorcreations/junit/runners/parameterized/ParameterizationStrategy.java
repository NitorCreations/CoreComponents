package com.nitorcreations.junit.runners.parameterized;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;

public interface ParameterizationStrategy {
	void classCreationInProgress(String nameRaw, ClassVisitor cw);

	void loadConstructorArgs(GeneratorAdapter mv);

	void classConstructed(Class<?> clazz, byte[] clazzBytes,
			Object[] constructorArgs);
}
