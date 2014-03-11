package com.nitorcreations.junit.runners.parameterized;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.GETSTATIC;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;

public class DefaultParameterizationStrategy implements
		ParameterizationStrategy {

	private static final String FIELD_CONSTRUCTOR_ARGS = "$constructorArgs";

	private String nameRaw;

	public void classCreationInProgress(String nameRaw, ClassVisitor cw) {
		this.nameRaw = nameRaw;
		cw.visitField(ACC_PUBLIC + ACC_STATIC, FIELD_CONSTRUCTOR_ARGS,
				"[Ljava/lang/Object;", null, null).visitEnd();
	}

	public void loadConstructorArgs(GeneratorAdapter mv) {
		mv.visitFieldInsn(GETSTATIC, nameRaw, FIELD_CONSTRUCTOR_ARGS,
				"[Ljava/lang/Object;");
	}

	public void classConstructed(Class<?> clazz, byte[] clazzBytes,
			Object[] constructorArgs) {
		try {
			clazz.getField(FIELD_CONSTRUCTOR_ARGS).set(null, constructorArgs);
		} catch (Exception e) {
			throw new RuntimeException(
					"Error installing parameterized arguments", e);
		}
	}
}
