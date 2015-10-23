/**
 * Copyright 2014-2015 Nitor Creations Oy
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
package com.nitorcreations.junit.runners.parameterized;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.GETSTATIC;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Default strategy is to create a public static field "$constructorArgs" which
 * stores the set of parameters to run the tests with.
 */
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
