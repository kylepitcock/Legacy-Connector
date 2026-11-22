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
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** {@link WorldFloorClampRewriter}: {@code if (y < K) y = K;} on a value derived from the tile's yCoord counts from the world's real bottom. */
class WorldFloorClampRewriterTest implements Opcodes {
    private static final String TILE = "legacyconnector/v1_7_10/net/minecraft/tileentity/TileEntity";

    private static Hierarchy hierarchy() {
        Map<String, ClassInfo> shim = new HashMap<>();
        shim.put("java/lang/Object", new ClassInfo("java/lang/Object", null, List.of(), ACC_PUBLIC));
        shim.put(TILE, new ClassInfo(TILE, "java/lang/Object", List.of(), ACC_PUBLIC));
        return new Hierarchy(new ShimIndex(shim), null, Map.of(), LegacyVersion.V1_7_10);
    }

    /** As the translator does: the class itself is known to the hierarchy as a local one. */
    private static int run(ClassNode n, Hierarchy h, LegacyVersion v) {
        h.addLocal(ClassInfo.of(n));
        return WorldFloorClampRewriter.apply(n, h, v);
    }

    private static ClassNode tile(String superName) {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = "mod/Lamp";
        n.superName = superName;
        return n;
    }

    private static MethodNode method(ClassNode n, int access) {
        MethodNode m = new MethodNode(access, "update", "()V", null, null);
        m.maxStack = 3;
        m.maxLocals = 4;
        n.methods.add(m);
        return m;
    }

    private static void loadY(MethodNode m, int local) {
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, TILE, "yCoord", "I");
        m.visitInsn(ICONST_2);
        m.visitInsn(IADD);
        m.visitVarInsn(ISTORE, local);
    }

    private static void push(MethodNode m, int k) {
        if (k <= 5) m.visitInsn(ICONST_0 + k);
        else m.visitIntInsn(BIPUSH, k);
    }

    /** {@code y = yCoord + 2; if (y < loadK) y = storeK;} */
    private static MethodNode clamp(ClassNode n, int cmpK, int storeK) {
        MethodNode m = method(n, ACC_PUBLIC);
        loadY(m, 1);
        Label l = new Label();
        m.visitVarInsn(ILOAD, 1);
        if (cmpK == 0) {
            m.visitJumpInsn(IFGE, l);
        } else {
            push(m, cmpK);
            m.visitJumpInsn(IF_ICMPGE, l);
        }
        push(m, storeK);
        m.visitVarInsn(ISTORE, 1);
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
            else if (i instanceof MethodInsnNode c) s += " " + c.name;
            else if (i instanceof IntInsnNode v) s += " " + v.operand;
            out.add(s);
        }
        return out;
    }

    private static List<String> clampTail(int k, boolean frontLoadFloor) {
        List<String> r = new ArrayList<>();
        r.add(ALOAD + " 0");
        r.add(String.valueOf(k <= 5 ? ICONST_0 + k : BIPUSH) + (k > 5 ? " " + k : ""));
        r.add(INVOKESTATIC + " aboveFloor");
        return r;
    }

    @Test
    void theClassicFiveClampBecomesAFloorRelativeOne() {
        ClassNode n = tile(TILE);
        MethodNode m = clamp(n, 5, 5);
        assertEquals(1, run(n, hierarchy(), LegacyVersion.V1_7_10));
        List<String> o = ops(m);
        // ... ILOAD 1; [aload_0, iconst_5, aboveFloor]; IF_ICMPGE; [aload_0, iconst_5, aboveFloor]; ISTORE 1; RETURN
        int i = o.indexOf(ILOAD + " 1");
        assertEquals(clampTail(5, false), o.subList(i + 1, i + 4));
        assertEquals(String.valueOf(IF_ICMPGE), o.get(i + 4));
        assertEquals(clampTail(5, false), o.subList(i + 5, i + 8));
        assertEquals(ISTORE + " 1", o.get(i + 8));
        assertEquals(5, m.maxStack, "two more operand slots");
    }

    @Test
    void aZeroClampUsesTheSignTestWithAnExplicitFloorLoad() {
        ClassNode n = tile(TILE);
        MethodNode m = clamp(n, 0, 0);
        assertEquals(1, run(n, hierarchy(), LegacyVersion.V1_7_10));
        List<String> o = ops(m);
        int i = o.indexOf(ILOAD + " 1");
        assertEquals(clampTail(0, true), o.subList(i + 1, i + 4));
        assertEquals(String.valueOf(IF_ICMPGE), o.get(i + 4));
        assertEquals(clampTail(0, true), o.subList(i + 5, i + 8));
        assertEquals(ISTORE + " 1", o.get(i + 8));
    }

    @Test
    void constantsAboveFiveUseBipush() {
        ClassNode n = tile(TILE);
        MethodNode m = clamp(n, 7, 7);
        assertEquals(1, run(n, hierarchy(), LegacyVersion.V1_7_10));
        assertEquals(1, ops(m).stream().filter(s -> s.equals(BIPUSH + " 7")).count() / 2, "both constants kept as 7 inside a floor call");
    }

    @Test
    void floorsDeeperThanBedrockAndMismatchedConstantsAreLeftAlone() {
        ClassNode a = tile(TILE);
        clamp(a, 9, 9);
        assertEquals(0, run(a, hierarchy(), LegacyVersion.V1_7_10));
        ClassNode b = tile(TILE);
        clamp(b, 5, 4);
        assertEquals(0, run(b, hierarchy(), LegacyVersion.V1_7_10));
    }

    @Test
    void aLocalNotDerivedFromYCoordIsNotAClamp() {
        ClassNode n = tile(TILE);
        MethodNode m = method(n, ACC_PUBLIC);
        Label l = new Label();
        m.visitInsn(ICONST_3);
        m.visitVarInsn(ISTORE, 1);
        m.visitVarInsn(ILOAD, 1);
        m.visitInsn(ICONST_5);
        m.visitJumpInsn(IF_ICMPGE, l);
        m.visitInsn(ICONST_5);
        m.visitVarInsn(ISTORE, 1);
        m.visitLabel(l);
        m.visitInsn(RETURN);
        assertEquals(0, run(n, hierarchy(), LegacyVersion.V1_7_10));
    }

    @Test
    void classesThatAreNoTileEntitiesAreSkipped() {
        ClassNode n = tile("java/lang/Object");
        clamp(n, 5, 5);
        assertEquals(0, run(n, hierarchy(), LegacyVersion.V1_7_10));
    }

    @Test
    void subclassesOfALocalTileEntityAreRecognised() {
        Hierarchy h = hierarchy();
        h.addLocal(new ClassInfo("mod/BaseTile", TILE, List.of(), ACC_PUBLIC));
        ClassNode n = tile("mod/BaseTile");
        clamp(n, 5, 5);
        assertEquals(1, run(n, h, LegacyVersion.V1_7_10));
    }

    @Test
    void staticMethodsAndOtherVersionsAreSkipped() {
        ClassNode n = tile(TILE);
        MethodNode m = method(n, ACC_PUBLIC | ACC_STATIC);
        m.visitInsn(RETURN);
        assertEquals(0, run(n, hierarchy(), LegacyVersion.V1_7_10));
        ClassNode n2 = tile(TILE);
        clamp(n2, 5, 5);
        assertEquals(0, run(n2, hierarchy(), LegacyVersion.V1_12_2));
    }

    @Test
    void aJumpThatSkipsMoreThanTheStoreIsNotTheClampIdiom() {
        ClassNode n = tile(TILE);
        MethodNode m = method(n, ACC_PUBLIC);
        loadY(m, 1);
        Label far = new Label();
        m.visitVarInsn(ILOAD, 1);
        m.visitInsn(ICONST_5);
        m.visitJumpInsn(IF_ICMPGE, far);
        m.visitInsn(ICONST_5);
        m.visitVarInsn(ISTORE, 1);
        m.visitVarInsn(ILOAD, 1); // more code after the store, then the jump target
        m.visitInsn(POP);
        m.visitLabel(far);
        m.visitInsn(RETURN);
        assertEquals(0, run(n, hierarchy(), LegacyVersion.V1_7_10));
    }
}
