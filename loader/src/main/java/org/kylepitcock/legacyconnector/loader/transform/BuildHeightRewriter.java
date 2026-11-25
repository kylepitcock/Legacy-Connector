package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashMap;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * 1.7.10 worlds were 0..255 high and mod code hard-codes that range on the y coordinate it is handed: RotaryCraft's and
 * ChromatiCraft's {@code ItemBlockPlacer.checkValidBounds(stack, player, world, x, y, z)} is {@code y >= 0 && y < ...}, so their
 * machines could not be placed below y=0 of a modern overworld (-64..319). This pass makes such checks ask the world for its
 * real build range.
 *
 * <p>Rules. A method qualifies when its descriptor has a parameter that is a legacy {@code World} or {@code IBlockAccess} (or a
 * subtype) directly followed by three {@code int}s (x, y, z), and the method never stores to the world parameter's slot. The
 * middle int is {@code y}. Stores to y are accepted: javac never reuses a parameter's slot for another variable, so the local
 * stays the source's {@code y} (scans like {@code for (; y > 0; y--)} keep meaning "down to the bottom"). Then, with the y
 * load directly followed by the comparison:</p>
 * <ul>
 *   <li>{@code iload y; iflt|ifge|ifle|ifgt L} becomes {@code iload y; aload world; invokestatic BuildHeight.bottom; if_icmplt|ge|le|gt L}
 *       ({@code y < 0} is "below the bottom", {@code y > 0} "above the bottom", which is how "has a block under it" checks read);</li>
 *   <li>{@code iload y; sipush 256; if_icmp* L} becomes {@code iload y; aload world; invokestatic BuildHeight.top; if_icmp* L}, and
 *       {@code sipush 255} likewise {@code BuildHeight.lastY} (top - 1).</li>
 * </ul>
 * <p>{@code y == 0} tests and comparisons against other constants are left alone. Every rewrite keeps the stack empty at the
 * branch, so the existing stack map frames stay valid; the method's max stack grows by one. The runtime answers 0..256 for
 * generation views and dimensions that are that high, so world generation keeps 1.7.10's range (see the shim World's
 * {@code mcc$buildBottom}).</p>
 */
public final class BuildHeightRewriter {
    private static final String HELPER = "org/kylepitcock/legacyconnector/v1_7_10/bridge/BuildHeight";

    private BuildHeightRewriter() {}

    /** Number of comparisons rewritten in the class. */
    public static int apply(ClassNode node, Hierarchy hierarchy, LegacyVersion version) {
        if (version.family() != LegacyVersion.V1_7_10) return 0;
        String world = version.linkPrefix() + "net/minecraft/world/World";
        String access = version.linkPrefix() + "net/minecraft/world/IBlockAccess";
        String helperArg = "(L" + access + ";)I";
        Map<String, Boolean> worldLike = new HashMap<>();
        int rewritten = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            // IGuiHandler's (id, player, world, x, y, z): y is a marker there, not a height (Railcraft's entity GUIs pass x = entity id, y = -1)
            if (m.name.equals("getServerGuiElement") || m.name.equals("getClientGuiElement")) continue;
            int[] slots = slots(m, t -> worldLike.computeIfAbsent(t, n -> isWorldLike(n, world, access, hierarchy, 0)));
            if (slots == null) {
                rewritten += tileLocals(m, world, helperArg);
                continue;
            }
            int worldSlot = slots[0], ySlot = slots[1];
            if (storesTo(m, worldSlot)) continue;
            int n = 0;
            InsnList insns = m.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() != Opcodes.ILOAD || ((VarInsnNode) insn).var != ySlot) continue;
                AbstractInsnNode next = insn.getNext();
                if (next == null) break;
                int op = next.getOpcode();
                if (op == Opcodes.IFLT || op == Opcodes.IFGE || op == Opcodes.IFLE || op == Opcodes.IFGT) {
                    JumpInsnNode jump = (JumpInsnNode) next;
                    insns.insert(insn, helper(worldSlot, "bottom", helperArg));
                    jump.setOpcode(op - Opcodes.IFLT + Opcodes.IF_ICMPLT);
                    insn = jump;
                    n++;
                } else if (op == Opcodes.SIPUSH && (((IntInsnNode) next).operand == 256 || ((IntInsnNode) next).operand == 255)
                        && next.getNext() != null && next.getNext().getOpcode() >= Opcodes.IF_ICMPEQ && next.getNext().getOpcode() <= Opcodes.IF_ICMPLE) {
                    String name = ((IntInsnNode) next).operand == 256 ? "top" : "lastY";
                    AbstractInsnNode jump = next.getNext();
                    insns.insert(insn, helper(worldSlot, name, helperArg));
                    insns.remove(next);
                    insn = jump;
                    n++;
                }
            }
            if (n > 0) {
                m.maxStack += 1;
                rewritten += n;
            }
        }
        return rewritten;
    }

    /**
     * Tile entity methods that scan from their own {@code yCoord} with a local ({@code int y = yCoord - 1; ... y >= 0}; BuildCraft's
     * mining well and quarry): a local int whose every store is {@code this.yCoord} plus or minus constants (or itself) is a y
     * coordinate, and its zero tests ask the tile's {@code worldObj} for the bottom of the build range instead.
     */
    private static int tileLocals(MethodNode m, String world, String helperArg) {
        if ((m.access & Opcodes.ACC_STATIC) != 0) return 0;
        int paramSlots = Type.getArgumentsAndReturnSizes(m.desc) >> 2;
        java.util.Map<Integer, String> owners = new HashMap<>();
        java.util.Set<Integer> bad = new java.util.HashSet<>();
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.ISTORE) continue;
            int slot = ((VarInsnNode) insn).var;
            if (slot < paramSlots) { bad.add(slot); continue; }
            String owner = null;
            boolean ok = false;
            for (AbstractInsnNode p = insn.getPrevious(); p != null; p = p.getPrevious()) {
                int op = p.getOpcode();
                if (op == -1) continue;
                if (op == Opcodes.GETFIELD) {
                    FieldInsnNode f = (FieldInsnNode) p;
                    if (f.name.equals("yCoord") && f.desc.equals("I")) {
                        AbstractInsnNode q = p.getPrevious();
                        ok = q != null && q.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) q).var == 0;
                        owner = f.owner;
                    }
                    break;
                }
                if (op == Opcodes.ILOAD && ((VarInsnNode) p).var == slot) { ok = true; break; } // y = y +- constants
                if (!( (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) || op == Opcodes.BIPUSH || op == Opcodes.SIPUSH
                        || op == Opcodes.IADD || op == Opcodes.ISUB)) break;
            }
            if (!ok) bad.add(slot);
            else if (owner != null) owners.putIfAbsent(slot, owner);
        }
        int n = 0;
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.ILOAD) continue;
            int slot = ((VarInsnNode) insn).var;
            if (bad.contains(slot) || !owners.containsKey(slot)) continue;
            AbstractInsnNode next = insn.getNext();
            if (next == null) break;
            int op = next.getOpcode();
            if (op == Opcodes.IFLT || op == Opcodes.IFGE || op == Opcodes.IFLE || op == Opcodes.IFGT) {
                JumpInsnNode jump = (JumpInsnNode) next;
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new FieldInsnNode(Opcodes.GETFIELD, owners.get(slot), "worldObj", "L" + world + ";"));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "bottom", helperArg, false));
                m.instructions.insert(insn, l);
                jump.setOpcode(op - Opcodes.IFLT + Opcodes.IF_ICMPLT);
                insn = jump;
                n++;
            }
        }
        if (n > 0) m.maxStack += 1;
        return n;
    }

    private static InsnList helper(int worldSlot, String name, String desc) {
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ALOAD, worldSlot));
        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, name, desc, false));
        return l;
    }

    /** {world slot, y slot} of the first (world, int, int, int) run of parameters, or null. */
    private static int[] slots(MethodNode m, java.util.function.Predicate<String> isWorld) {
        Type[] args = Type.getArgumentTypes(m.desc);
        int slot = (m.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (int i = 0; i < args.length; i++) {
            if (i + 3 < args.length && args[i].getSort() == Type.OBJECT && args[i + 1] == Type.INT_TYPE && args[i + 2] == Type.INT_TYPE
                    && args[i + 3] == Type.INT_TYPE && isWorld.test(args[i].getInternalName())) {
                return new int[] {slot, slot + 2};
            }
            slot += args[i].getSize();
        }
        return null;
    }

    private static boolean storesTo(MethodNode m, int slot) {
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.ASTORE && ((VarInsnNode) insn).var == slot) return true;
        }
        return false;
    }

    private static boolean isWorldLike(String name, String world, String access, Hierarchy hierarchy, int depth) {
        if (name == null || depth > 32) return false;
        if (name.equals(world) || name.equals(access)) return true;
        if (name.startsWith("java/")) return false;
        ClassInfo info = hierarchy.find(name);
        if (info == null) return false;
        if (isWorldLike(info.superName, world, access, hierarchy, depth + 1)) return true;
        for (String i : info.interfaces) if (isWorldLike(i, world, access, hierarchy, depth + 1)) return true;
        return false;
    }
}
