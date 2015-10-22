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
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_5;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

/**
 * Explicit support for PowerMock.
 * <p>
 * PowerMock loads test classes using javassist's ClassPool mechanism, which
 * will not see the classes we inject into the context classloader. So instead
 * we amend PowerMock's classpath with a ByteArrayClassPath that loads the
 * testclass in question, only.
 * <p>
 * To achieve this:
 * <ul>
 * <li>Create a helper class that implements PowerMock's @
 * {@link org.powermock.core.classloader.ClassPathAdjuster.ClassPathAdjuster}
 * interface.</li>
 * <li>Store both the testclass bytecode (as an byte array) and the constructor
 * arguments in public static fields in the helper class.</li>
 * <li>Tell PowerMock to amend its classpath to include our helper class using
 * the @{@link org.powermock.core.classloader.annotations.UseClassPathAdjuster}
 * annotation</li>
 * <li>Also exempt the package of the helper class using @
 * {@link PowerMockIgnore} on the test class.</li>
 * <li>When PowerMock calls our helper class, we add a new @
 * {@link javassist.ByteArrayClassPath} instance initialized to load the test
 * class from the byte array.</li>
 * <li>When PowerMock has loaded our test class and constructs it, fetch the
 * constructor arguments for the superclass from the public static field in the
 * helper class.</li>
 * </ul>
 * <p>
 * <i>The ClassPathAdjuster interface and the UseClassPathAdjuster annotation
 * were contributed to PowerMock in order to unlock this stunt. :)</i>
 */
public class PowermockParameterizationStrategy implements
		ParameterizationStrategy {

	private static final String PACKAGE_FOR_POWERMOCK_AVOIDANCE_RAW = "com/nitorcreations/nomocks";

	private static final String POWERMOCK_HELPER_PREFIX = "PowerMockHelper";

	private static final String FIELD_TEST_CLASS_BYTE_ARR = "testClassByteArr";

	private static final String FIELD_CONSTRUCTOR_ARGS = "constructorArgs";

	private static final String CLASSPATH_ADJUSTER_CLASS_INTERNALNAME = "org/powermock/core/classloader/ClassPathAdjuster";
	private static final String USE_CLASSPATH_ADJUSTER_CLASS_DESCRIPTOR = "Lorg/powermock/core/classloader/annotations/UseClassPathAdjuster;";
	private static final String CLASSPATH_ADJUSTER_CLASSNAME = CLASSPATH_ADJUSTER_CLASS_INTERNALNAME
			.replace('/', '.');

	private static String getPowerMockHelperClassRaw(String nameRaw) {
		String powerMockHelperClassRaw = PACKAGE_FOR_POWERMOCK_AVOIDANCE_RAW
				+ '/' + POWERMOCK_HELPER_PREFIX + '_'
				+ nameRaw.replace('/', '_').replace('$', '_');
		return powerMockHelperClassRaw;
	}

	private String nameRaw;
	private String powerMockHelperClassRaw;

	public PowermockParameterizationStrategy() {
		try {
			Class.forName(CLASSPATH_ADJUSTER_CLASSNAME);
		} catch (ClassNotFoundException e) {
			throw new ParameterizationStrategyNotAvailableException(
					"Your version of PowerMock is too old - version 1.5.5 or later required");
		}
	}

	public void classCreationInProgress(String nameRaw, ClassVisitor cw) {
		this.nameRaw = nameRaw;
		this.powerMockHelperClassRaw = getPowerMockHelperClassRaw(nameRaw);

		// @UseClassPathAdjuster(PowerMockHelperClass.class)
		AnnotationVisitor av0 = cw.visitAnnotation(
				USE_CLASSPATH_ADJUSTER_CLASS_DESCRIPTOR, true);
		av0.visit("value", Type.getObjectType(powerMockHelperClassRaw));
		av0.visitEnd();

		// @PowerMockIgnore("package.for.powermock.avoidance.*")
		AnnotationVisitor av1 = cw.visitAnnotation(
				Type.getType(PowerMockIgnore.class).getDescriptor(), true);
		AnnotationVisitor av2 = av1.visitArray("value");
		av2.visit(null, PACKAGE_FOR_POWERMOCK_AVOIDANCE_RAW.replace('/', '.')
				+ ".*");
		av2.visitEnd();
		av1.visitEnd();
	}

	public void loadConstructorArgs(GeneratorAdapter mv) {
		mv.visitFieldInsn(GETSTATIC, powerMockHelperClassRaw,
				FIELD_CONSTRUCTOR_ARGS, "[Ljava/lang/Object;");
	}

	public void classConstructed(Class<?> clazz, byte[] clazzBytes,
			Object[] constructorArgs) {
		ClassWriter cw = new ClassWriter(0);

		// public class PowerMockHelperClass implements ClassPathAdjuster {
		cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, powerMockHelperClassRaw, null,
				"java/lang/Object",
				new String[] { CLASSPATH_ADJUSTER_CLASS_INTERNALNAME });

		// public static byte[] testClassBytes;
		cw.visitField(ACC_PUBLIC + ACC_STATIC, FIELD_TEST_CLASS_BYTE_ARR, "[B",
				null, null).visitEnd();

		// public static Object[] constructorArgs;
		cw.visitField(ACC_PUBLIC + ACC_STATIC, FIELD_CONSTRUCTOR_ARGS,
				"[Ljava/lang/Object;", null, null).visitEnd();

		// public PowerMockHelperClass() {
		{
			MethodVisitor mv;
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			// super();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>",
					"()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		// @Override
		// public void adjustClassPath(ClassPool pool) {
		{
			MethodVisitor mv;
			mv = cw.visitMethod(ACC_PUBLIC, "adjustClassPath",
					"(Ljavassist/ClassPool;)V", null, null);
			mv.visitCode();
			// pool.appendClassPath(new ByteArrayClassPath("TestClass_0",
			// testClassBytes));
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(NEW, "javassist/ByteArrayClassPath");
			mv.visitInsn(DUP);
			mv.visitLdcInsn(Type.getObjectType(nameRaw).getClassName());
			mv.visitFieldInsn(GETSTATIC, powerMockHelperClassRaw,
					FIELD_TEST_CLASS_BYTE_ARR, "[B");
			mv.visitMethodInsn(INVOKESPECIAL, "javassist/ByteArrayClassPath",
					"<init>", "(Ljava/lang/String;[B)V");
			mv.visitMethodInsn(INVOKEVIRTUAL, "javassist/ClassPool",
					"appendClassPath",
					"(Ljavassist/ClassPath;)Ljavassist/ClassPath;");
			mv.visitInsn(POP);
			mv.visitInsn(RETURN);
			mv.visitMaxs(5, 2);
			mv.visitEnd();
		}
		cw.visitEnd();

		byte[] clazzBytesCPA = cw.toByteArray();
		Class<?> clazzCPA = ClassLoaderInjector.injectClass(
				Type.getObjectType(powerMockHelperClassRaw).getClassName(),
				clazzBytesCPA);

		try {
			clazzCPA.getField(FIELD_TEST_CLASS_BYTE_ARR).set(null, clazzBytes);
		} catch (Exception e) {
			throw new RuntimeException("Error installing powermock helper", e);
		}

		try {
			clazzCPA.getField(FIELD_CONSTRUCTOR_ARGS)
					.set(null, constructorArgs);
		} catch (Exception e) {
			throw new RuntimeException(
					"Error installing parameterized arguments", e);
		}
	}
}
