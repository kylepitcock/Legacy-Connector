package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

class SideOnlyStripperTest implements Opcodes {
    private static final String OWNER = "t/Sample";
    private static final Handle LMF = new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);

    /** Emits {@code return () -> lambda}: a Runnable over the static lambda method {@code target}. */
    private static void runnable(MethodVisitor mv, String target) {
        mv.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", LMF, Type.getMethodType("()V"),
                new Handle(H_INVOKESTATIC, OWNER, target, "()V", false), Type.getMethodType("()V"));
    }

    private static void lambdaBody(ClassWriter cw, String name, String nested) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, name, "()V", null, null);
        mv.visitCode();
        if (nested != null) {
            runnable(mv, nested);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Runnable", "run", "()V", true);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
    }

    private static MethodVisitor method(ClassWriter cw, String name, boolean clientOnly) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, name, "()Ljava/lang/Runnable;", null, null);
        if (clientOnly) {
            var av = mv.visitAnnotation("Lcpw/mods/fml/relauncher/SideOnly;", true);
            av.visitEnum("value", "Lcpw/mods/fml/relauncher/Side;", "CLIENT");
            av.visitEnd();
        }
        mv.visitCode();
        return mv;
    }

    private static void returnRunnable(MethodVisitor mv, String lambda) {
        runnable(mv, lambda);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
    }

    private static byte[] sample() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC, OWNER, null, "java/lang/Object", null);
        // client(): lambda$client$0 -> nested lambda$client$1 -> nested lambda$shared$2 (also used by keep())
        returnRunnable(method(cw, "client", true), "lambda$client$0");
        lambdaBody(cw, "lambda$client$0", "lambda$client$1");
        lambdaBody(cw, "lambda$client$1", "lambda$shared$2");
        lambdaBody(cw, "lambda$shared$2", null);
        // keep(): own lambda plus the shared one through a nested lambda
        returnRunnable(method(cw, "keep", false), "lambda$keep$3");
        lambdaBody(cw, "lambda$keep$3", "lambda$shared$2");
        // other(): a client-only method that calls the kept lambda directly, and a plain method with a name that
        // only looks like a lambda body (not synthetic): must stay
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "lambda$handwritten$9", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        MethodVisitor c = method(cw, "client2", true);
        runnable(c, "lambda$handwritten$9");
        c.visitInsn(ARETURN);
        c.visitMaxs(1, 0);
        c.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static Set<String> names(ClassNode n) {
        return n.methods.stream().map(m -> m.name).collect(Collectors.toSet());
    }

    @Test
    void removesOrphanedLambdasButKeepsReachableOnes() throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(sample()).accept(node, 0);
        int before = SideOnlyStripper.LAMBDAS_REMOVED.get();

        int removed = SideOnlyStripper.apply(node, "", "SERVER");

        Set<String> left = names(node);
        assertEquals(2 + 2, removed, "two annotated methods + two orphaned lambdas");
        assertEquals(2, SideOnlyStripper.LAMBDAS_REMOVED.get() - before);
        assertFalse(left.contains("client"));
        assertFalse(left.contains("client2"));
        assertFalse(left.contains("lambda$client$0"), "direct orphan");
        assertFalse(left.contains("lambda$client$1"), "nested orphan");
        assertTrue(left.contains("keep"));
        assertTrue(left.contains("lambda$keep$3"), "kept method's lambda");
        assertTrue(left.contains("lambda$shared$2"), "reachable through a kept lambda");
        assertTrue(left.contains("lambda$handwritten$9"), "not synthetic");

        ClassWriter cw = new ClassWriter(0);
        node.accept(cw);
        byte[] out = cw.toByteArray();
        StringWriter log = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(out), getClass().getClassLoader(), false, new PrintWriter(log));
        assertEquals("", log.toString(), "class must stay valid");

        // and it must really link and run
        class Loader extends ClassLoader {
            Loader() { super(SideOnlyStripperTest.class.getClassLoader()); }
            Class<?> define() { return defineClass("t.Sample", out, 0, out.length); }
        }
        Object r = new Loader().define().getMethod("keep").invoke(null);
        ((Runnable) r).run();
    }

    @Test
    void otherSideIsLeftAlone() {
        ClassNode node = new ClassNode();
        new ClassReader(sample()).accept(node, 0);
        int count = node.methods.size();
        assertEquals(0, SideOnlyStripper.apply(node, "", "CLIENT"));
        assertEquals(count, node.methods.size());
    }

    @Test
    void creativeListingMethodsStayOnTheServer() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V21, ACC_PUBLIC, OWNER, null, "java/lang/Object", null);
        for (String name : new String[] {"func_150895_a", "func_149666_a", "getSubItems", "other"}) {
            MethodVisitor mv = method(cw, name, true);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        ClassNode node = new ClassNode();
        new ClassReader(cw.toByteArray()).accept(node, 0);
        assertEquals(1, SideOnlyStripper.apply(node, "", "SERVER"));
        assertEquals(Set.of("func_150895_a", "func_149666_a", "getSubItems"), names(node));
    }
}
