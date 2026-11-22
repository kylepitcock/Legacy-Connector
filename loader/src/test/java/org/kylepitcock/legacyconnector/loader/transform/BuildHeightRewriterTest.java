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
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** {@link BuildHeightRewriter}: hard-coded 0..255 checks on the y handed to a (world, x, y, z) method ask the world for its real range. */
class BuildHeightRewriterTest implements Opcodes {
    private static final String P = "legacyconnector/v1_7_10/net/minecraft/world/";
    private static final String WORLD = P + "World";
    private static final String ACCESS = P + "IBlockAccess";
    private static final String HELPER = "org/kylepitcock/legacyconnector/v1_7_10/bridge/BuildHeight";

    private static Hierarchy hierarchy() {
        Map<String, ClassInfo> shim = new HashMap<>();
        shim.put("java/lang/Object", new ClassInfo("java/lang/Object", null, List.of(), ACC_PUBLIC));
        shim.put(ACCESS, new ClassInfo(ACCESS, "java/lang/Object", List.of(), ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT));
        shim.put(WORLD, new ClassInfo(WORLD, "java/lang/Object", List.of(ACCESS), ACC_PUBLIC));
        Hierarchy h = new Hierarchy(new ShimIndex(shim), null, Map.of(), LegacyVersion.V1_7_10);
        h.addLocal(new ClassInfo("mod/MyWorld", WORLD, List.of(), ACC_PUBLIC));
        return h;
    }

    private static ClassNode node() {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = "mod/Placer";
        n.superName = "java/lang/Object";
        return n;
    }

    private static MethodNode method(ClassNode n, int access, String desc) {
        MethodNode m = new MethodNode(access, "check", desc, null, null);
        m.maxStack = 2;
        m.maxLocals = 8;
        n.methods.add(m);
        return m;
    }

    /** {@code iload y; <jump op> L; return; L: return} on a static method (World, x, y, z): y is slot 2. */
    private static MethodNode zeroTest(ClassNode n, int jumpOp) {
        MethodNode m = method(n, ACC_PUBLIC | ACC_STATIC, "(L" + WORLD + ";III)V");
        Label l = new Label();
        m.visitVarInsn(ILOAD, 2);
        m.visitJumpInsn(jumpOp, l);
        m.visitInsn(RETURN);
        m.visitLabel(l);
        m.visitInsn(RETURN);
        return m;
    }

    private static List<String> ops(MethodNode m) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getOpcode() < 0) continue;
            String s = Integer.toString(i.getOpcode());
            if (i instanceof VarInsnNode v) s += " " + v.var;
            else if (i instanceof MethodInsnNode c) s += " " + c.name + c.desc;
            else if (i instanceof org.objectweb.asm.tree.IntInsnNode v) s += " " + v.operand;
            out.add(s);
        }
        return out;
    }

    private static String helperCall(String name) {
        return INVOKESTATIC + " " + name + "(L" + ACCESS + ";)I";
    }

    @Test
    void signComparisonsBecomeComparisonsAgainstTheWorldsBottom() {
        int[][] map = {{IFLT, IF_ICMPLT}, {IFGE, IF_ICMPGE}, {IFLE, IF_ICMPLE}, {IFGT, IF_ICMPGT}};
        for (int[] pair : map) {
            ClassNode n = node();
            MethodNode m = zeroTest(n, pair[0]);
            assertEquals(1, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10));
            assertEquals(List.of(ILOAD + " 2", ALOAD + " 0", helperCall("bottom"), String.valueOf(pair[1]), String.valueOf(RETURN), String.valueOf(RETURN)), ops(m),
                    "for opcode " + pair[0]);
            assertEquals(3, m.maxStack, "one more operand slot");
        }
    }

    @Test
    void equalityWithZeroIsLeftAlone() {
        ClassNode n = node();
        zeroTest(n, IFEQ);
        assertEquals(0, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10));
    }

    @Test
    void topAndLastYReplaceTheConstants256And255() {
        for (Object[] c : new Object[][] {{256, "top"}, {255, "lastY"}}) {
            ClassNode n = node();
            MethodNode m = method(n, ACC_PUBLIC | ACC_STATIC, "(L" + WORLD + ";III)V");
            Label l = new Label();
            m.visitVarInsn(ILOAD, 2);
            m.visitIntInsn(SIPUSH, (Integer) c[0]);
            m.visitJumpInsn(IF_ICMPGE, l);
            m.visitInsn(RETURN);
            m.visitLabel(l);
            m.visitInsn(RETURN);
            assertEquals(1, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10));
            assertEquals(List.of(ILOAD + " 2", ALOAD + " 0", helperCall((String) c[1]), String.valueOf(IF_ICMPGE), String.valueOf(RETURN), String.valueOf(RETURN)),
                    ops(m));
        }
    }

    @Test
    void otherConstantsAndNonComparisonsStay() {
        ClassNode n = node();
        MethodNode m = method(n, ACC_PUBLIC | ACC_STATIC, "(L" + WORLD + ";III)V");
        Label l = new Label();
        m.visitVarInsn(ILOAD, 2);
        m.visitIntInsn(SIPUSH, 200);
        m.visitJumpInsn(IF_ICMPGE, l);
        m.visitVarInsn(ILOAD, 2);
        m.visitIntInsn(SIPUSH, 256);
        m.visitInsn(IADD); // not a comparison
        m.visitInsn(POP);
        m.visitVarInsn(ILOAD, 1); // x, not y
        m.visitJumpInsn(IFLT, l);
        m.visitLabel(l);
        m.visitInsn(RETURN);
        assertEquals(0, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10));
    }

    @Test
    void instanceMethodsCountTheReceiverSlotAndWideParametersTwo() {
        ClassNode n = node();
        // (long, World, x, y, z): this = 0, long = 1..2, world = 3, x = 4, y = 5
        MethodNode m = method(n, ACC_PUBLIC, "(JL" + WORLD + ";III)V");
        Label l = new Label();
        m.visitVarInsn(ILOAD, 5);
        m.visitJumpInsn(IFLT, l);
        m.visitInsn(RETURN);
        m.visitLabel(l);
        m.visitInsn(RETURN);
        assertEquals(1, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10));
        assertEquals(List.of(ILOAD + " 5", ALOAD + " 3", helperCall("bottom"), String.valueOf(IF_ICMPLT), String.valueOf(RETURN), String.valueOf(RETURN)), ops(m));
    }

    @Test
    void worldSubtypesAndIBlockAccessQualify() {
        for (String type : new String[] {"mod/MyWorld", ACCESS}) {
            ClassNode n = node();
            MethodNode m = method(n, ACC_PUBLIC | ACC_STATIC, "(L" + type + ";III)V");
            Label l = new Label();
            m.visitVarInsn(ILOAD, 2);
            m.visitJumpInsn(IFLT, l);
            m.visitInsn(RETURN);
            m.visitLabel(l);
            m.visitInsn(RETURN);
            assertEquals(1, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10), type);
        }
    }

    @Test
    void methodsWithoutAWorldAndThreeIntsAreIgnored() {
        ClassNode n = node();
        MethodNode a = method(n, ACC_PUBLIC | ACC_STATIC, "(Ljava/lang/String;III)V");
        Label l = new Label();
        a.visitVarInsn(ILOAD, 2);
        a.visitJumpInsn(IFLT, l);
        a.visitLabel(l);
        a.visitInsn(RETURN);
        MethodNode b = method(n, ACC_PUBLIC | ACC_STATIC, "(L" + WORLD + ";II)V"); // only two ints
        Label l2 = new Label();
        b.visitVarInsn(ILOAD, 2);
        b.visitJumpInsn(IFLT, l2);
        b.visitLabel(l2);
        b.visitInsn(RETURN);
        assertEquals(0, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10));
    }

    @Test
    void aMethodThatReassignsTheWorldParameterIsNotRewritten() {
        ClassNode n = node();
        MethodNode m = method(n, ACC_PUBLIC | ACC_STATIC, "(L" + WORLD + ";III)V");
        Label l = new Label();
        m.visitInsn(ACONST_NULL);
        m.visitVarInsn(ASTORE, 0);
        m.visitVarInsn(ILOAD, 2);
        m.visitJumpInsn(IFLT, l);
        m.visitLabel(l);
        m.visitInsn(RETURN);
        assertEquals(0, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10));
    }

    @Test
    void forwardVersionsOfThe1710FamilyAreRewrittenAndOthersAreNot() {
        ClassNode n = node();
        zeroTest(n, IFLT);
        assertEquals(0, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_12_2));
        ClassNode n2 = node();
        zeroTest(n2, IFLT);
        assertEquals(1, BuildHeightRewriter.apply(n2, hierarchy(), LegacyVersion.V1_6_4), "1.6.4 links against the 1.7.10 shim");
    }

    @Test
    void severalComparisonsInOneMethodAreAllRewrittenAndCounted() {
        ClassNode n = node();
        MethodNode m = method(n, ACC_PUBLIC | ACC_STATIC, "(L" + WORLD + ";III)V");
        Label l = new Label();
        m.visitVarInsn(ILOAD, 2);
        m.visitJumpInsn(IFLT, l);
        m.visitVarInsn(ILOAD, 2);
        m.visitIntInsn(SIPUSH, 256);
        m.visitJumpInsn(IF_ICMPGE, l);
        m.visitLabel(l);
        m.visitInsn(RETURN);
        assertEquals(2, BuildHeightRewriter.apply(n, hierarchy(), LegacyVersion.V1_7_10));
        assertEquals(3, m.maxStack, "grows once per method, not per comparison");
    }
}
