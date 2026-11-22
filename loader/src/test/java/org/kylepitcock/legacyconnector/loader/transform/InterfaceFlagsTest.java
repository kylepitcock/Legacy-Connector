package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;

/** {@link InterfaceFlags}: the "owner is an interface" flag of calls and handles must agree with the owner (Java 9+ rejects the class otherwise). */
class InterfaceFlagsTest implements Opcodes {
    private static final Handle LMF = new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "()V", false);

    private static Hierarchy hierarchy() {
        Map<String, ClassInfo> shim = new HashMap<>();
        shim.put("java/lang/Object", new ClassInfo("java/lang/Object", null, List.of(), ACC_PUBLIC));
        shim.put("legacyconnector/I", new ClassInfo("legacyconnector/I", "java/lang/Object", List.of(), ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT));
        shim.put("legacyconnector/C", new ClassInfo("legacyconnector/C", "java/lang/Object", List.of(), ACC_PUBLIC));
        return new Hierarchy(new ShimIndex(shim), null, Map.of(), null);
    }

    private static MethodNode method(ClassNode n) {
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "f", "()V", null, null);
        n.methods.add(m);
        return m;
    }

    private static ClassNode node() {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.name = "t/X";
        n.superName = "java/lang/Object";
        n.access = ACC_PUBLIC;
        return n;
    }

    private static MethodInsnNode firstCall(MethodNode m) {
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) if (i instanceof MethodInsnNode c) return c;
        throw new AssertionError("no call");
    }

    @Test
    void plainMethodRefToAnInterfaceBecomesAnInterfaceCall() {
        ClassNode n = node();
        MethodNode m = method(n);
        m.visitMethodInsn(INVOKESTATIC, "legacyconnector/I", "util", "()V", false);
        m.visitInsn(RETURN);
        assertEquals(1, InterfaceFlags.apply(n, hierarchy()));
        assertTrue(firstCall(m).itf);
        assertEquals(INVOKESTATIC, firstCall(m).getOpcode());
    }

    @Test
    void invokeVirtualOnAnInterfaceIsPromotedToInvokeInterface() {
        ClassNode n = node();
        MethodNode m = method(n);
        m.visitMethodInsn(INVOKEVIRTUAL, "legacyconnector/I", "go", "()V", false);
        m.visitInsn(RETURN);
        assertEquals(1, InterfaceFlags.apply(n, hierarchy()));
        assertEquals(INVOKEINTERFACE, firstCall(m).getOpcode());
        assertTrue(firstCall(m).itf);
    }

    @Test
    void invokeInterfaceOnAClassIsDemotedToInvokeVirtual() {
        ClassNode n = node();
        MethodNode m = method(n);
        m.visitMethodInsn(INVOKEINTERFACE, "legacyconnector/C", "go", "()V", true);
        m.visitInsn(RETURN);
        assertEquals(1, InterfaceFlags.apply(n, hierarchy()));
        assertEquals(INVOKEVIRTUAL, firstCall(m).getOpcode());
        assertFalse(firstCall(m).itf);
    }

    @Test
    void unknownOwnersAndArrayOwnersAreNotTouched() {
        ClassNode n = node();
        MethodNode m = method(n);
        m.visitMethodInsn(INVOKEVIRTUAL, "absent/Mod", "go", "()V", false);
        m.visitMethodInsn(INVOKEVIRTUAL, "[Llegacyconnector/I;", "clone", "()Ljava/lang/Object;", false);
        m.visitInsn(RETURN);
        assertEquals(0, InterfaceFlags.apply(n, hierarchy()));
    }

    @Test
    void invokeInterfaceOnAnUnknownOwnerStaysAnInterfaceCall() {
        ClassNode n = node();
        MethodNode m = method(n);
        m.visitMethodInsn(INVOKEINTERFACE, "absent/Mod", "go", "()V", false);
        m.visitInsn(RETURN);
        assertEquals(1, InterfaceFlags.apply(n, hierarchy()), "the missing flag is set");
        assertEquals(INVOKEINTERFACE, firstCall(m).getOpcode());
        assertTrue(firstCall(m).itf, "INVOKEINTERFACE always targets an interface method ref");
    }

    @Test
    void correctCallsAreNotCounted() {
        ClassNode n = node();
        MethodNode m = method(n);
        m.visitMethodInsn(INVOKEINTERFACE, "legacyconnector/I", "go", "()V", true);
        m.visitMethodInsn(INVOKEVIRTUAL, "legacyconnector/C", "go", "()V", false);
        m.visitInsn(RETURN);
        assertEquals(0, InterfaceFlags.apply(n, hierarchy()));
    }

    @Test
    void lambdaImplementationHandlesAreCorrected() {
        ClassNode n = node();
        MethodNode m = method(n);
        // a default method of an interface used as a lambda body: javac of the old toolchain flagged the handle as a class method
        m.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", LMF, Type.getMethodType("()V"),
                new Handle(H_INVOKEVIRTUAL, "legacyconnector/I", "body", "()V", false), Type.getMethodType("()V"));
        m.visitInsn(RETURN);
        assertEquals(1, InterfaceFlags.apply(n, hierarchy()));
        InvokeDynamicInsnNode d = null;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) if (i instanceof InvokeDynamicInsnNode x) d = x;
        Handle fixed = (Handle) d.bsmArgs[1];
        assertEquals(H_INVOKEINTERFACE, fixed.getTag());
        assertTrue(fixed.isInterface());
        assertEquals("body", fixed.getName());
        assertEquals(Type.getMethodType("()V"), d.bsmArgs[0], "non-handle bootstrap arguments are kept");
    }

    @Test
    void fieldHandlesAreIgnored() {
        ClassNode n = node();
        MethodNode m = method(n);
        m.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", LMF, new Handle(H_GETFIELD, "legacyconnector/I", "x", "I", false));
        m.visitInsn(RETURN);
        assertEquals(0, InterfaceFlags.apply(n, hierarchy()));
    }

    @Test
    void abstractMethodsWithoutBodiesAreSkipped() {
        ClassNode n = node();
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_ABSTRACT, "g", "()V", null, null);
        m.instructions = null;
        n.methods.add(m);
        assertEquals(0, InterfaceFlags.apply(n, hierarchy()));
    }
}
