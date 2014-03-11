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
import org.powermock.core.classloader.ClassPathAdjuster;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.UseClassPathAdjuster;

public class PowermockParameterizationStrategy implements
		ParameterizationStrategy {

	private static final String PACKAGE_FOR_POWERMOCK_AVOIDANCE_RAW = "com/nitorcreations/nomocks";

	private static final String POWERMOCK_HELPER_PREFIX = "PowerMockHelper";

	private static final String FIELD_TEST_CLASS_BYTE_ARR = "testClassByteArr";

	private static final String FIELD_CONSTRUCTOR_ARGS = "constructorArgs";

	private static String getPowerMockHelperClassRaw(String nameRaw) {
		String powerMockHelperClassRaw = PACKAGE_FOR_POWERMOCK_AVOIDANCE_RAW
				+ '/' + POWERMOCK_HELPER_PREFIX + '_'
				+ nameRaw.replace('/', '_').replace('$', '_');
		return powerMockHelperClassRaw;
	}

	private String nameRaw;
	private String powerMockHelperClassRaw;

	public void classCreationInProgress(String nameRaw, ClassVisitor cw) {
		this.nameRaw = nameRaw;
		this.powerMockHelperClassRaw = getPowerMockHelperClassRaw(nameRaw);

		// @UseClassPathAdjuster(PowerMockHelperClass.class)
		AnnotationVisitor av0 = cw.visitAnnotation(
				Type.getType(UseClassPathAdjuster.class).getDescriptor(), true);
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

		cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, powerMockHelperClassRaw, null,
				"java/lang/Object",
				new String[] { Type.getInternalName(ClassPathAdjuster.class) });

		cw.visitField(ACC_PUBLIC + ACC_STATIC, FIELD_TEST_CLASS_BYTE_ARR, "[B",
				null, null).visitEnd();

		cw.visitField(ACC_PUBLIC + ACC_STATIC, FIELD_CONSTRUCTOR_ARGS,
				"[Ljava/lang/Object;", null, null).visitEnd();

		{
			MethodVisitor mv;
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>",
					"()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			MethodVisitor mv;
			mv = cw.visitMethod(ACC_PUBLIC, "adjustClassPath",
					"(Ljavassist/ClassPool;)V", null, null);
			mv.visitCode();
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
