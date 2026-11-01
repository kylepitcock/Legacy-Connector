package org.kylepitcock.legacyconnector.loader.transform;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * 1.7.10's worlds started at y=0 with bedrock up to y=4, and tile entities that pick a random height around themselves clamp it
 * to just above that floor: Thaumcraft's Arcane Lamp does {@code y = yCoord + rand - rand; if (y < 5) y = 5;}, so a lamp below
 * y=5 of a modern overworld (-64..319) lit y=5 instead of the room it stood in. This pass makes such clamps count from the
 * world's real bottom.
 *
 * <p>Rules. Only instance methods of tile entities (subclasses of the legacy {@code TileEntity}) are looked at, and only locals
 * that the method assigns from an expression reading the tile's own {@code yCoord}. A clamp of such a local is javac's form of
 * {@code if (y < K) y = K;} with the same small constant K (0..8) on both sides: {@code iload y; ldc K; if_icmpge L; ldc K;
 * istore y; L:} (or {@code iload y; ifge L; iconst_0; istore y; L:} for K = 0). Both constants become
 * {@code aload_0; ldc K; invokestatic BuildHeight.aboveFloor(TileEntity, int)}, the world's bottom plus K, which is K itself in
 * worlds and generation views that keep 1.7.10's range. The stack is empty at the branch as before, so the stack map frames stay
 * valid; the method's max stack grows by two.</p>
 */
public final class WorldFloorClampRewriter {
    private static final String HELPER = "org/kylepitcock/legacyconnector/v1_7_10/bridge/BuildHeight";
    /** The deepest 1.7.10 floor a clamp names (bedrock was y=0..4). */
    private static final int MAX_FLOOR = 8;
    /** How far back from a store the expression that computed the stored value is searched for {@code yCoord}. */
    private static final int EXPRESSION_REACH = 48;

    private WorldFloorClampRewriter() {}

    /** Number of clamps rewritten in the class. */
    public static int apply(ClassNode node, Hierarchy hierarchy, LegacyVersion version) {
        if (version != LegacyVersion.V1_7_10) return 0;
        String tileEntity = version.shimPrefix() + "net/minecraft/tileentity/TileEntity";
        Map<String, Boolean> tiles = new HashMap<>();
        if (!isSubclass(node.name, tileEntity, hierarchy, tiles, 0)) return 0;
        String desc = "(L" + tileEntity + ";I)I";
        int rewritten = 0;
        for (MethodNode m : node.methods) {
            if ((m.access & Opcodes.ACC_STATIC) != 0 || m.instructions == null || m.instructions.size() == 0) continue;
            BitSet fromY = yLocals(m, tileEntity, hierarchy, tiles);
            if (fromY.isEmpty()) continue;
            int n = 0;
            InsnList insns = m.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() != Opcodes.ILOAD || !fromY.get(((VarInsnNode) insn).var)) continue;
                int var = ((VarInsnNode) insn).var;
                AbstractInsnNode a = insn.getNext();
                if (a == null) break;
                AbstractInsnNode jump, b;
                int k;
                if (a.getOpcode() == Opcodes.IFGE) {
                    k = 0;
                    jump = a;
                    b = jump.getNext();
                    if (b == null || constant(b) != 0) continue;
                } else {
                    k = constant(a);
                    if (k < 1 || k > MAX_FLOOR) continue;
                    jump = a.getNext();
                    if (jump == null || jump.getOpcode() != Opcodes.IF_ICMPGE) continue;
                    b = jump.getNext();
                    if (b == null || constant(b) != k) continue;
                }
                AbstractInsnNode store = b.getNext();
                if (store == null || store.getOpcode() != Opcodes.ISTORE || ((VarInsnNode) store).var != var) continue;
                if (!jumpsTo((JumpInsnNode) jump, store.getNext())) continue;
                if (k == 0) {
                    insns.insert(insn, floor(0, desc));
                    ((JumpInsnNode) jump).setOpcode(Opcodes.IF_ICMPGE);
                } else {
                    insns.insert(a, floor(k, desc));
                    insns.remove(a);
                }
                insns.insert(b, floor(k, desc));
                insns.remove(b);
                insn = store;
                n++;
            }
            if (n > 0) {
                m.maxStack += 2;
                rewritten += n;
            }
        }
        return rewritten;
    }

    /** {@code aload_0; K; invokestatic BuildHeight.aboveFloor}. */
    private static InsnList floor(int k, String desc) {
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ALOAD, 0));
        l.add(k <= 5 ? new InsnNode(Opcodes.ICONST_0 + k) : new IntInsnNode(Opcodes.BIPUSH, k));
        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "aboveFloor", desc, false));
        return l;
    }

    /** The int pushed by an iconst/bipush instruction, or -1. */
    private static int constant(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        if (op >= Opcodes.ICONST_0 && op <= Opcodes.ICONST_5) return op - Opcodes.ICONST_0;
        if (op == Opcodes.BIPUSH) return ((IntInsnNode) insn).operand;
        return -1;
    }

    /** Whether the jump lands right after the clamp's store (labels, line numbers and frames in between are fine). */
    private static boolean jumpsTo(JumpInsnNode jump, AbstractInsnNode after) {
        for (AbstractInsnNode i = after; i != null && i.getOpcode() < 0; i = i.getNext()) {
            if (i instanceof LabelNode l && l == jump.label) return true;
        }
        return false;
    }

    /** Locals the method stores a value into that was computed from the tile's {@code yCoord}. */
    private static BitSet yLocals(MethodNode m, String tileEntity, Hierarchy hierarchy, Map<String, Boolean> tiles) {
        BitSet out = new BitSet();
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.ISTORE) continue;
            int reach = 0;
            for (AbstractInsnNode p = insn.getPrevious(); p != null && reach < EXPRESSION_REACH; p = p.getPrevious()) {
                int op = p.getOpcode();
                if (op < 0) continue;
                reach++;
                if (op >= Opcodes.ISTORE && op <= Opcodes.ASTORE || p instanceof JumpInsnNode || op == Opcodes.POP || op == Opcodes.POP2) break;
                if (op == Opcodes.GETFIELD && p instanceof FieldInsnNode f && f.name.equals("yCoord") && f.desc.equals("I")
                        && p.getPrevious() != null && p.getPrevious().getOpcode() == Opcodes.ALOAD && ((VarInsnNode) p.getPrevious()).var == 0
                        && isSubclass(f.owner, tileEntity, hierarchy, tiles, 0)) {
                    out.set(((VarInsnNode) insn).var);
                    break;
                }
            }
        }
        return out;
    }

    private static boolean isSubclass(String name, String target, Hierarchy hierarchy, Map<String, Boolean> cache, int depth) {
        if (name == null || depth > 32) return false;
        if (name.equals(target)) return true;
        if (name.startsWith("java/")) return false;
        Boolean known = cache.get(name);
        if (known != null) return known;
        ClassInfo info = hierarchy.find(name);
        boolean result = info != null && isSubclass(info.superName, target, hierarchy, cache, depth + 1);
        cache.put(name, result);
        return result;
    }
}
