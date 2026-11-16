package org.kylepitcock.legacyconnector.loader.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.transform.Hierarchy;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Synthetic classes generated with ASM through {@link BytecodeCheck}. */
class BytecodeCheckTest implements Opcodes {
    private static byte[] plain(String name, String sup) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V17, ACC_PUBLIC, name, null, sup, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, sup, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** A class with a static {@code take(B)} and the given static test methods. */
    private static byte[] user(String name, Consumer<ClassWriter> methods) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V17, ACC_PUBLIC, name, null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "take", "(Lt/B;)V", null, null);
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
        methods.accept(cw);
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** {@code static void name(PARAM p) { take(p); }} where take wants t/B. */
    private static void passer(ClassWriter cw, String method, String paramType) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, method, "(L" + paramType + ";)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "t/User", "take", "(Lt/B;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static BytecodeCheck check(byte[]... classes) {
        Hierarchy h = new Hierarchy(ShimIndex.empty(), BytecodeCheckTest.class.getClassLoader());
        return BytecodeCheck.runClasses(List.of(classes), h);
    }

    @Test
    void unrelatedTypeIsReportedWithBothNames() {
        BytecodeCheck c = check(plain("t/A", "java/lang/Object"), plain("t/B", "java/lang/Object"), user("t/User", cw -> passer(cw, "bad", "t/A")));
        assertEquals(1, c.findingCount, c.summary() + c.unknownTypes);
        assertEquals(1, c.groups.size());
        BytecodeCheck.Group g = c.groups.values().iterator().next();
        assertEquals("t.B", g.expected);
        assertEquals("t.A", g.found);
        assertTrue(g.describe().contains("t.A is not assignable to t.B"), g.describe());
        assertTrue(g.examples.get(0).site().contains("INVOKESTATIC t/User.take"), g.examples.get(0).site());
        assertTrue(c.summary().startsWith("verify: 3 classes, "), c.summary());
        assertTrue(c.summary().contains("1 findings in 1 groups"), c.summary());
    }

    @Test
    void subclassIsClean() {
        BytecodeCheck c = check(plain("t/B", "java/lang/Object"), plain("t/A", "t/B"), user("t/User", cw -> passer(cw, "ok", "t/A")));
        assertEquals(0, c.findingCount, c.groups.toString());
        assertEquals(0, c.methodsSkipped);
    }

    @Test
    void unresolvableTypeIsUnknownNotAnError() {
        BytecodeCheck c = check(plain("t/B", "java/lang/Object"), user("t/User", cw -> passer(cw, "maybe", "x/Missing")));
        assertEquals(0, c.findingCount);
        assertTrue(c.unknownTypes.contains("x/Missing"), c.unknownTypes.toString());
        assertTrue(c.summary().contains("1 unknown types"), c.summary());
    }

    @Test
    void stackUnderflowIsReported() {
        byte[] bad = user("t/User", cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "broken", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        });
        BytecodeCheck c = check(bad);
        assertEquals(1, c.findingCount, c.summary() + c.unknownTypes);
        assertTrue(c.failures.get(0).contains("broken"), c.failures.toString());
    }

    @Test
    void wrongSlotKindIsReported() {
        // an int on the stack where a reference is required
        byte[] bad = user("t/User", cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "intAsRef", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKESTATIC, "t/User", "take", "(Lt/B;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        });
        BytecodeCheck c = check(plain("t/B", "java/lang/Object"), bad);
        assertEquals(1, c.findingCount, c.summary() + c.unknownTypes);
    }

    @Test
    void repeatedTypePairIsOneGroupWithSiteCount() {
        List<byte[]> classes = new ArrayList<>(List.of(plain("t/A", "java/lang/Object"), plain("t/B", "java/lang/Object"),
                user("t/User", cw -> {
                    passer(cw, "one", "t/A");
                    passer(cw, "two", "t/A");
                    passer(cw, "three", "t/A");
                })));
        BytecodeCheck c = check(classes.toArray(new byte[0][]));
        assertEquals(3, c.findingCount, c.summary() + c.unknownTypes);
        assertEquals(1, c.groups.size());
        assertEquals(3, c.groups.values().iterator().next().sites);
        assertEquals(3, c.toJson().getAsJsonArray("groups").get(0).getAsJsonObject().get("examples").getAsJsonArray().size());
        assertEquals(3, c.toJson().getAsJsonArray("groups").get(0).getAsJsonObject().get("sites").getAsInt());
    }
}
