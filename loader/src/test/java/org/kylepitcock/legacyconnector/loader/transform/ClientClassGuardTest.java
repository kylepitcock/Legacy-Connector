package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** {@link ClientClassGuard}: on a dedicated server a client class named inside a {@code catch (NoClassDefFoundError)} range throws it first. */
class ClientClassGuardTest implements Opcodes {
    private static final String SHIM = "legacyconnector/v1_7_10/";
    private static final String MC = SHIM + "net/minecraft/client/Minecraft";

    private static ClassNode node() {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = "mod/CommonUtils";
        n.superName = "java/lang/Object";
        return n;
    }

    /** {@code try { Minecraft.getMinecraft(); other() } catch (<type> e) { } } */
    private static MethodNode tryCatch(ClassNode n, String type, String owner) {
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "dir", "()V", null, null);
        Label start = new Label(), end = new Label(), handler = new Label(), after = new Label();
        m.visitTryCatchBlock(start, end, handler, type);
        m.visitLabel(start);
        m.visitMethodInsn(INVOKESTATIC, owner, "getMinecraft", "()V", false);
        m.visitMethodInsn(INVOKESTATIC, owner, "second", "()V", false);
        m.visitLabel(end);
        m.visitJumpInsn(GOTO, after);
        m.visitLabel(handler);
        m.visitInsn(POP);
        m.visitLabel(after);
        m.visitInsn(RETURN);
        n.methods.add(m);
        return m;
    }

    private static List<String> calls(MethodNode m) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode c) out.add(c.owner + "." + c.name);
            else if (i instanceof LdcInsnNode l) out.add("ldc " + l.cst);
        }
        return out;
    }

    @Test
    void theFirstClientReferenceInAProtectedRangeIsPrecededByTheGuardOnAServer() {
        ClassNode n = node();
        MethodNode m = tryCatch(n, "java/lang/NoClassDefFoundError", MC);
        assertEquals(1, ClientClassGuard.apply(n, SHIM, "SERVER"));
        assertEquals(List.of("ldc legacyconnector.v1_7_10.net.minecraft.client.Minecraft",
                "org/kylepitcock/legacyconnector/compat/jdk/JdkCompat.clientClassAbsent", MC + ".getMinecraft", MC + ".second"), calls(m));
    }

    @Test
    void linkageErrorHandlersCountToo() {
        ClassNode n = node();
        tryCatch(n, "java/lang/LinkageError", MC);
        assertEquals(1, ClientClassGuard.apply(n, SHIM, "SERVER"));
    }

    @Test
    void nothingIsDoneOnTheClient() {
        ClassNode n = node();
        MethodNode m = tryCatch(n, "java/lang/NoClassDefFoundError", MC);
        int before = m.instructions.size();
        assertEquals(0, ClientClassGuard.apply(n, SHIM, "CLIENT"));
        assertEquals(0, ClientClassGuard.apply(n, SHIM, null));
        assertEquals(before, m.instructions.size());
    }

    @Test
    void otherExceptionTypesAndNonClientClassesAreNotGuarded() {
        ClassNode a = node();
        tryCatch(a, "java/lang/RuntimeException", MC);
        assertEquals(0, ClientClassGuard.apply(a, SHIM, "SERVER"));
        ClassNode b = node();
        tryCatch(b, "java/lang/NoClassDefFoundError", SHIM + "net/minecraft/world/World");
        assertEquals(0, ClientClassGuard.apply(b, SHIM, "SERVER"));
    }

    @Test
    void classLiteralsAndTypeInstructionsOfClientClassesAreReferences() {
        ClassNode n = node();
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "f", "()V", null, null);
        Label start = new Label(), end = new Label(), handler = new Label();
        m.visitTryCatchBlock(start, end, handler, "java/lang/NoClassDefFoundError");
        m.visitLabel(start);
        m.visitLdcInsn(Type.getObjectType(SHIM + "cpw/mods/fml/client/FMLClientHandler"));
        m.visitInsn(POP);
        m.visitLabel(end);
        m.visitInsn(RETURN);
        m.visitLabel(handler);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        n.methods.add(m);
        assertEquals(1, ClientClassGuard.apply(n, SHIM, "SERVER"));
    }

    @Test
    void referencesOutsideTheTryRangeAreNotGuarded() {
        ClassNode n = node();
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "f", "()V", null, null);
        Label start = new Label(), end = new Label(), handler = new Label();
        m.visitMethodInsn(INVOKESTATIC, MC, "before", "()V", false);
        m.visitTryCatchBlock(start, end, handler, "java/lang/NoClassDefFoundError");
        m.visitLabel(start);
        m.visitInsn(NOP);
        m.visitLabel(end);
        m.visitInsn(RETURN);
        m.visitLabel(handler);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        n.methods.add(m);
        assertEquals(0, ClientClassGuard.apply(n, SHIM, "SERVER"));
    }
}
