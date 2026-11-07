package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * The second half of the {@code EntityLiving} / {@code EntityLivingBase} split of Forge 1.5.2 jars (see {@link LegacyVersion#V1_5_2}).
 *
 * <p>1.5.2 has no {@code EntityLivingBase}: {@code EntityLiving} is the base of every living entity, the player included. 1.7.10 split it, and
 * {@code EntityPlayer} is no {@code EntityLiving} any more. So the class rename maps every type use of {@code EntityLiving} to
 * {@code EntityLivingBase} (what the player, a zombie and a villager share), and the places that need the real {@code EntityLiving}
 * ({@code extends}, {@code super.} calls) are kept by {@link LegacyRemapper}. What is left are the uses of members that only the 1.7.10
 * {@code EntityLiving} declares (the navigator, the attack target, the look helper, the 1.5.2 fields {@code entityToAttack}, ...), called on a
 * value typed {@code EntityLivingBase} now. For each, this pass casts the value to the real {@code EntityLiving} (a ClassCastException for a
 * player is the 1.7.10 behaviour of such code anyway) and calls the member on it. Likewise for shim methods that take an
 * {@code EntityLiving} parameter ({@code new EntityAILookIdle(entityLiving)}): the argument is cast and the descriptor keeps 1.7.10's type.</p>
 *
 * <p>Runs after relocation and the forward call rewrites, before {@link ShimLinker} (so the member is looked up on the right class).</p>
 */
public final class LivingCasts152 {
    private LivingCasts152() {}

    /** @return the number of call or field sites that got a cast */
    public static int apply(ClassNode node, Hierarchy h, LegacyVersion version) {
        if (version != LegacyVersion.V1_5_2) return 0;
        String base = version.linkPrefix() + "net/minecraft/entity/EntityLivingBase";
        String living = version.linkPrefix() + "net/minecraft/entity/EntityLiving";
        if (h.find(base) == null || h.find(living) == null) return 0;
        int count = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            List<AbstractInsnNode> work = new ArrayList<>();
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode || insn instanceof FieldInsnNode) work.add(insn);
            }
            for (AbstractInsnNode insn : work) {
                if (insn instanceof MethodInsnNode m && fixCall(method, m, h, base, living)) count++;
                else if (insn instanceof FieldInsnNode f && fixField(method, f, h, base, living)) count++;
            }
        }
        return count;
    }

    private static boolean fixCall(MethodNode method, MethodInsnNode m, Hierarchy h, String base, String living) {
        if (m.owner.startsWith("[") || m.getOpcode() == Opcodes.INVOKEDYNAMIC) return false;
        boolean hasBaseArg = m.desc.contains("L" + base + ";");
        boolean ownerBase = m.owner.equals(base) && m.getOpcode() != Opcodes.INVOKESTATIC && m.getOpcode() != Opcodes.INVOKESPECIAL;
        if (!hasBaseArg && !ownerBase) return false;
        if (h.resolveMethod(m.owner, m.name, m.desc) != Hierarchy.Lookup.MISSING_MEMBER) return false;
        Type[] args = Type.getArgumentTypes(m.desc);
        List<Integer> baseArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) if (args[i].getDescriptor().equals("L" + base + ";")) baseArgs.add(i);
        int subsets = 1 << Math.min(baseArgs.size(), 5);
        // the nearest variant first: fewest changed arguments, receiver last
        for (int bits = 0; bits < subsets; bits++) {
            for (int recv = 0; recv <= (ownerBase ? 1 : 0); recv++) {
                if (bits == 0 && recv == 0) continue;
                String desc = m.desc;
                List<Integer> castArgs = new ArrayList<>();
                for (int b = 0; b < baseArgs.size() && b < 5; b++) {
                    if ((bits & (1 << b)) != 0) castArgs.add(baseArgs.get(b));
                }
                desc = withLiving(args, castArgs, Type.getReturnType(m.desc), living);
                String owner = recv == 1 ? living : m.owner;
                if (h.resolveMethod(owner, m.name, desc) != Hierarchy.Lookup.FOUND) continue;
                // cast the receiver (position 0) and the chosen arguments (position i + 1; static calls have no receiver)
                boolean isStatic = m.getOpcode() == Opcodes.INVOKESTATIC;
                List<Integer> positions = new ArrayList<>();
                if (recv == 1) positions.add(0);
                for (int a : castArgs) positions.add(a + (isStatic ? 0 : 1));
                insertCasts(method, m, args, !isStatic, positions, living);
                m.owner = owner;
                m.desc = desc;
                return true;
            }
        }
        return false;
    }

    private static String withLiving(Type[] args, List<Integer> cast, Type ret, String living) {
        Type[] out = args.clone();
        for (int i : cast) out[i] = Type.getObjectType(living);
        return Type.getMethodDescriptor(ret, out);
    }

    private static boolean fixField(MethodNode method, FieldInsnNode f, Hierarchy h, String base, String living) {
        if (!f.owner.equals(base)) return false;
        boolean isStatic = f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.PUTSTATIC;
        if (isStatic) return false;
        if (h.resolveField(base, f.name, f.desc) != Hierarchy.Lookup.MISSING_MEMBER || h.findVirtualField(base, f.name, f.desc, false) != null) return false;
        if (h.resolveField(living, f.name, f.desc) != Hierarchy.Lookup.FOUND && h.findVirtualField(living, f.name, f.desc, false) == null) return false;
        InsnList list = method.instructions;
        if (f.getOpcode() == Opcodes.GETFIELD) {
            list.insertBefore(f, new TypeInsnNode(Opcodes.CHECKCAST, living));
        } else {
            // PUTFIELD: receiver, value; the value goes through a temporary
            Type value = Type.getType(f.desc);
            int slot = method.maxLocals;
            method.maxLocals += value.getSize();
            list.insertBefore(f, new VarInsnNode(value.getOpcode(Opcodes.ISTORE), slot));
            list.insertBefore(f, new TypeInsnNode(Opcodes.CHECKCAST, living));
            list.insertBefore(f, new VarInsnNode(value.getOpcode(Opcodes.ILOAD), slot));
        }
        f.owner = living;
        return true;
    }

    /**
     * Casts the stack items at {@code positions} (0 = receiver when {@code hasReceiver}, then the arguments) to {@code living} just before
     * {@code call}: the items above the lowest one are parked in temporary locals, then reloaded, with the cast where wanted.
     */
    private static void insertCasts(MethodNode method, MethodInsnNode call, Type[] args, boolean hasReceiver, List<Integer> positions, String living) {
        int lowest = Integer.MAX_VALUE;
        for (int p : positions) lowest = Math.min(lowest, p);
        int items = args.length + (hasReceiver ? 1 : 0);
        Type[] stack = new Type[items];
        if (hasReceiver) stack[0] = Type.getObjectType("java/lang/Object");
        for (int i = 0; i < args.length; i++) stack[i + (hasReceiver ? 1 : 0)] = args[i];
        InsnList list = method.instructions;
        if (lowest == items - 1) {
            list.insertBefore(call, new TypeInsnNode(Opcodes.CHECKCAST, living));
            return;
        }
        int[] slots = new int[items];
        int next = method.maxLocals;
        for (int i = lowest; i < items; i++) {
            slots[i] = next;
            next += stack[i].getSize();
        }
        method.maxLocals = next;
        for (int i = items - 1; i >= lowest; i--) list.insertBefore(call, new VarInsnNode(stack[i].getOpcode(Opcodes.ISTORE), slots[i]));
        for (int i = lowest; i < items; i++) {
            list.insertBefore(call, new VarInsnNode(stack[i].getOpcode(Opcodes.ILOAD), slots[i]));
            if (positions.contains(i)) list.insertBefore(call, new TypeInsnNode(Opcodes.CHECKCAST, living));
        }
    }
}
