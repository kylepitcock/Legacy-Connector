package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * {@link LivingCasts152}: 1.5.2 had one {@code EntityLiving} for every living entity, the 1.7.10 shim splits it. Uses of members only the
 * real EntityLiving declares, on a value typed EntityLivingBase after the rename, get a cast to the real class.
 */
class LivingCasts152Test implements Opcodes {
    private static final String E = "legacyconnector/v1_7_10/net/minecraft/entity/";
    private static final String BASE = E + "EntityLivingBase";
    private static final String LIVING = E + "EntityLiving";
    private static final String CREATURE = E + "EntityCreature";
    private static final String LOOK = E + "ai/EntityAILookIdle";
    private static final String TARGET = E + "ai/EntityAITarget";

    private static Hierarchy hierarchy(LegacyVersion version) {
        Map<String, ClassInfo> shim = new HashMap<>();
        shim.put("java/lang/Object", new ClassInfo("java/lang/Object", null, List.of(), ACC_PUBLIC));
        ClassInfo base = new ClassInfo(BASE, "java/lang/Object", List.of(), ACC_PUBLIC);
        base.methods.put("getHealth()F", ACC_PUBLIC);
        ClassInfo living = new ClassInfo(LIVING, BASE, List.of(), ACC_PUBLIC);
        living.methods.put("getNavigator()Ljava/lang/Object;", ACC_PUBLIC);
        living.fields.put("entityToAttack:L" + BASE + ";", ACC_PUBLIC);
        ClassInfo creature = new ClassInfo(CREATURE, LIVING, List.of(), ACC_PUBLIC);
        ClassInfo look = new ClassInfo(LOOK, "java/lang/Object", List.of(), ACC_PUBLIC);
        look.methods.put("<init>(L" + LIVING + ";)V", ACC_PUBLIC);
        look.methods.put("setup(IL" + LIVING + ";)V", ACC_PUBLIC);
        ClassInfo target = new ClassInfo(TARGET, "java/lang/Object", List.of(), ACC_PUBLIC);
        target.methods.put("<init>()V", ACC_PUBLIC);
        target.fields.put("taskOwner:L" + CREATURE + ";", ACC_PUBLIC);
        for (ClassInfo c : List.of(base, living, creature, look, target)) shim.put(c.name, c);
        return new Hierarchy(new ShimIndex(shim), null, Map.of(), version);
    }

    private static ClassNode node() {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = "mod/Ai";
        n.superName = "java/lang/Object";
        return n;
    }

    private static MethodNode method(ClassNode n, String desc) {
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "f", desc, null, null);
        m.maxStack = 4;
        m.maxLocals = 4;
        n.methods.add(m);
        return m;
    }

    private static List<String> ops(MethodNode m) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getOpcode() < 0) continue;
            String s = Integer.toString(i.getOpcode());
            if (i instanceof VarInsnNode v) s += " " + v.var;
            else if (i instanceof MethodInsnNode c) s += " " + c.owner + "." + c.name + c.desc;
            else if (i instanceof FieldInsnNode f) s += " " + f.owner + "." + f.name + ":" + f.desc;
            else if (i instanceof TypeInsnNode t) s += " " + t.desc;
            out.add(s);
        }
        return out;
    }

    @Test
    void aMemberOnlyTheRealEntityLivingHasIsCalledThroughACast() {
        ClassNode n = node();
        MethodNode m = method(n, "(L" + BASE + ";)V");
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, BASE, "getNavigator", "()Ljava/lang/Object;", false);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        assertEquals(1, LivingCasts152.apply(n, hierarchy(LegacyVersion.V1_5_2), LegacyVersion.V1_5_2));
        assertEquals(List.of(ALOAD + " 0", CHECKCAST + " " + LIVING, INVOKEVIRTUAL + " " + LIVING + ".getNavigator()Ljava/lang/Object;", POP + "", RETURN + ""), ops(m));
    }

    @Test
    void membersTheBaseClassHasAreLeftAlone() {
        ClassNode n = node();
        MethodNode m = method(n, "(L" + BASE + ";)V");
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, BASE, "getHealth", "()F", false);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        assertEquals(0, LivingCasts152.apply(n, hierarchy(LegacyVersion.V1_5_2), LegacyVersion.V1_5_2));
    }

    @Test
    void aConstructorArgumentTypedEntityLivingBaseIsCastToWhatTheShimTakes() {
        ClassNode n = node();
        MethodNode m = method(n, "(L" + BASE + ";)V");
        m.visitTypeInsn(NEW, LOOK);
        m.visitInsn(DUP);
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKESPECIAL, LOOK, "<init>", "(L" + BASE + ";)V", false);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        assertEquals(1, LivingCasts152.apply(n, hierarchy(LegacyVersion.V1_4_7), LegacyVersion.V1_4_7), "1.4.7 has the same single EntityLiving");
        assertEquals(List.of(NEW + " " + LOOK, DUP + "", ALOAD + " 0", CHECKCAST + " " + LIVING, INVOKESPECIAL + " " + LOOK + ".<init>(L" + LIVING + ";)V", POP + "", RETURN + ""),
                ops(m));
    }

    @Test
    void anArgumentBelowOtherArgumentsIsCastThroughTemporaries() {
        ClassNode n = node();
        MethodNode m = method(n, "(L" + LOOK + ";IL" + BASE + ";)V");
        // look.setup(int, base): the entity is the last argument, so a direct cast
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ILOAD, 1);
        m.visitVarInsn(ALOAD, 2);
        m.visitMethodInsn(INVOKEVIRTUAL, LOOK, "setup", "(IL" + BASE + ";)V", false);
        m.visitInsn(RETURN);
        assertEquals(1, LivingCasts152.apply(n, hierarchy(LegacyVersion.V1_5_2), LegacyVersion.V1_5_2));
        assertEquals(List.of(ALOAD + " 0", ILOAD + " 1", ALOAD + " 2", CHECKCAST + " " + LIVING, INVOKEVIRTUAL + " " + LOOK + ".setup(IL" + LIVING + ";)V", RETURN + ""),
                ops(m));
    }

    @Test
    void anEarlierArgumentIsParkedInTemporariesSoItCanBeCast() {
        Hierarchy h = hierarchy(LegacyVersion.V1_5_2);
        h.find(LOOK).methods.put("two(L" + LIVING + ";I)V", ACC_PUBLIC);
        ClassNode n = node();
        MethodNode m = method(n, "(L" + LOOK + ";L" + BASE + ";I)V");
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitVarInsn(ILOAD, 2);
        m.visitMethodInsn(INVOKEVIRTUAL, LOOK, "two", "(L" + BASE + ";I)V", false);
        m.visitInsn(RETURN);
        assertEquals(1, LivingCasts152.apply(n, h, LegacyVersion.V1_5_2));
        // the int and the entity are stored in two fresh locals (past the old maxLocals of 4) and reloaded, the entity with its cast
        assertEquals(List.of(ALOAD + " 0", ALOAD + " 1", ILOAD + " 2", ISTORE + " 5", ASTORE + " 4", ALOAD + " 4", CHECKCAST + " " + LIVING, ILOAD + " 5",
                INVOKEVIRTUAL + " " + LOOK + ".two(L" + LIVING + ";I)V", RETURN + ""), ops(m));
        assertEquals(6, m.maxLocals);
    }

    @Test
    void aFieldOnlyTheRealEntityLivingHasIsReadAndWrittenThroughACast() {
        ClassNode n = node();
        MethodNode m = method(n, "(L" + BASE + ";L" + BASE + ";)V");
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, BASE, "entityToAttack", "L" + BASE + ";");
        m.visitInsn(POP);
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitFieldInsn(PUTFIELD, BASE, "entityToAttack", "L" + BASE + ";");
        m.visitInsn(RETURN);
        assertEquals(2, LivingCasts152.apply(n, hierarchy(LegacyVersion.V1_5_2), LegacyVersion.V1_5_2));
        assertEquals(List.of(ALOAD + " 0", CHECKCAST + " " + LIVING, GETFIELD + " " + LIVING + ".entityToAttack:L" + BASE + ";", POP + "",
                ALOAD + " 0", ALOAD + " 1", ASTORE + " 4", CHECKCAST + " " + LIVING, ALOAD + " 4", PUTFIELD + " " + LIVING + ".entityToAttack:L" + BASE + ";", RETURN + ""), ops(m));
        assertEquals(5, m.maxLocals);
    }

    @Test
    void aFieldTheShimDeclaresMoreNarrowlyTakesTheDeclaredTypeWithACastOnWrites() {
        ClassNode n = node();
        MethodNode m = method(n, "(L" + TARGET + ";L" + BASE + ";)V");
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitFieldInsn(PUTFIELD, TARGET, "taskOwner", "L" + BASE + ";");
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, TARGET, "taskOwner", "L" + BASE + ";");
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        assertEquals(2, LivingCasts152.apply(n, hierarchy(LegacyVersion.V1_5_2), LegacyVersion.V1_5_2));
        assertEquals(List.of(ALOAD + " 0", ALOAD + " 1", CHECKCAST + " " + CREATURE, PUTFIELD + " " + TARGET + ".taskOwner:L" + CREATURE + ";",
                ALOAD + " 0", GETFIELD + " " + TARGET + ".taskOwner:L" + CREATURE + ";", POP + "", RETURN + ""), ops(m));
    }

    @Test
    void otherVersionsAndHierarchiesWithoutTheSplitAreNotTouched() {
        ClassNode n = node();
        MethodNode m = method(n, "(L" + BASE + ";)V");
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, BASE, "getNavigator", "()Ljava/lang/Object;", false);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        assertEquals(0, LivingCasts152.apply(n, hierarchy(LegacyVersion.V1_6_4), LegacyVersion.V1_6_4));
        Hierarchy empty = new Hierarchy(new ShimIndex(new HashMap<>()), null, Map.of(), LegacyVersion.V1_5_2);
        assertEquals(0, LivingCasts152.apply(n, empty, LegacyVersion.V1_5_2));
    }
}
