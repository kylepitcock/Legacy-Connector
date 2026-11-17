package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Math.abs on an entity's height (Traincraft's slope code) is dropped; other abs calls stay. */
class EntityHeightAbsRewriterTest implements Opcodes {
    private static final String ENTITY = "legacyconnector/v1_7_10/net/minecraft/entity/Entity";

    private static MethodInsnNode abs() {
        return new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
    }

    private static int absCount(MethodNode m) {
        int n = 0;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.name.equals("abs")) n++;
        }
        return n;
    }

    private static ClassNode cls(MethodNode m) {
        ClassNode c = new ClassNode();
        c.version = V1_8;
        c.name = "mod/Cart";
        c.superName = ENTITY;
        c.methods.add(m);
        return c;
    }

    /** void m(double y, double z) { setPosition(1.0, abs(y) or y..., z) } */
    private static MethodNode setPosition(boolean absOutermost) {
        MethodNode m = new MethodNode(ACC_PUBLIC, "m", "(DD)V", null, null);
        m.maxStack = 8;
        m.maxLocals = 5;
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new InsnNode(DCONST_1));
        m.instructions.add(new VarInsnNode(DLOAD, 1));
        if (absOutermost) m.instructions.add(abs());
        else {
            m.instructions.add(abs());
            m.instructions.add(new InsnNode(DCONST_1));
            m.instructions.add(new InsnNode(DADD));
        }
        m.instructions.add(new VarInsnNode(DLOAD, 3));
        m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "mod/Cart", "setPosition", "(DDD)V", false));
        m.instructions.add(new InsnNode(RETURN));
        return m;
    }

    @Test
    void dropsAbsOfTheHeightArgument() {
        MethodNode m = setPosition(true);
        assertEquals(1, EntityHeightAbsRewriter.apply(cls(m), LegacyVersion.V1_7_10));
        assertEquals(0, absCount(m));
    }

    @Test
    void keepsAbsDeeperInTheExpression() {
        MethodNode m = setPosition(false);
        assertEquals(0, EntityHeightAbsRewriter.apply(cls(m), LegacyVersion.V1_7_10));
        assertEquals(1, absCount(m));
    }

    @Test
    void dropsAbsStoredIntoPosY() {
        MethodNode m = new MethodNode(ACC_PUBLIC, "m", "(D)V", null, null);
        m.maxStack = 4;
        m.maxLocals = 3;
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new VarInsnNode(DLOAD, 1));
        m.instructions.add(abs());
        m.instructions.add(new FieldInsnNode(PUTFIELD, ENTITY, "posY", "D"));
        m.instructions.add(new InsnNode(RETURN));
        assertEquals(1, EntityHeightAbsRewriter.apply(cls(m), LegacyVersion.V1_7_10));
        assertEquals(0, absCount(m));
    }

    @Test
    void keepsAbsStoredIntoOtherFields() {
        MethodNode m = new MethodNode(ACC_PUBLIC, "m", "(D)V", null, null);
        m.maxStack = 4;
        m.maxLocals = 3;
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new VarInsnNode(DLOAD, 1));
        m.instructions.add(abs());
        m.instructions.add(new FieldInsnNode(PUTFIELD, ENTITY, "posX", "D"));
        m.instructions.add(new InsnNode(RETURN));
        assertEquals(0, EntityHeightAbsRewriter.apply(cls(m), LegacyVersion.V1_7_10));
        assertEquals(1, absCount(m));
    }
}
