package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.synth.CtorChooser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Constructors of a forward version that take numeric ids (1.6.4: {@code new Item(id)}, {@code super(id, Material.rock)},
 * {@code new ItemStack(id, 1, 0)}, {@code new WorldGenMinable(blockId, 8)}) onto the shim's object-taking ones. Through the
 * facade classes of the version ({@link Hierarchy#facadeOf}):
 * <ul>
 *     <li>a Block or Item subclass whose shim constructor is the same without the leading id: that one is called and the new object
 *     is claimed under the id afterwards ({@code mcc$claimId} of the Block/Item facade, which owns the id tables);</li>
 *     <li>a shim constructor taking a Block or Item where the leading id was ({@code ItemStack(Item, int, int)}, {@code ItemBlock(Block)});</li>
 *     <li>a shim constructor of the same arity taking Block/Item where some int arguments were ids ({@code WorldGenMinable(Block, int, Block)});
 *     the ids are turned into those objects by the facade ({@code mcc$fromId});</li>
 *     <li>for a Block/Item subclass otherwise, the id is dropped and the closest constructor of {@link CtorChooser} is used for the rest.</li>
 * </ul>
 * Done by spilling the arguments to fresh locals inside one basic block, like {@code ShimLinker.adaptConstructor}.
 */
final class IdConstructors {
    private IdConstructors() {}

    /** One target parameter: the argument it takes (-1 = default value) and the facade call that converts it. */
    private record Param(int arg, MethodInsnNode convert) {}

    /** @return the adapted call (the instruction that now stands for {@code m}), or null when this is not an id constructor. */
    static AbstractInsnNode adapt(MethodNode method, MethodInsnNode m, Type[] args, Hierarchy h, LinkReport report, String from) {
        boolean anyInt = false;
        for (Type t : args) anyInt |= t.getSort() == Type.INT;
        if (!anyInt) return null;
        String link = h.version().linkPrefix();
        String blockCls = link + "net/minecraft/block/Block";
        String itemCls = link + "net/minecraft/item/Item";
        boolean isBlock = h.isSubclassOf(m.owner, blockCls);
        boolean isItem = h.isSubclassOf(m.owner, itemCls);
        ClassInfo owner = h.find(m.owner);
        if (owner == null) return null;

        String targetDesc = null;
        List<Param> plan = new ArrayList<>();
        boolean leadingId = args[0].getSort() == Type.INT;

        if (leadingId) {
            Type[] rest = java.util.Arrays.copyOfRange(args, 1, args.length);
            StringBuilder restDesc = new StringBuilder("(");
            for (Type t : rest) restDesc.append(t.getDescriptor());
            restDesc.append(")V");
            if ((isBlock || isItem) && owner.hasMethod("<init>", restDesc.toString())) {
                targetDesc = restDesc.toString();
                for (int i = 0; i < rest.length; i++) plan.add(new Param(i + 1, null));
            } else {
                // the id becomes a Block/Item parameter: ItemStack(int, int, int) -> ItemStack(Item, int, int), ItemBlock(int) -> ItemBlock(Block)
                for (String cls : new String[] {itemCls, blockCls}) {
                    String d = "(L" + cls + ";" + restDesc.substring(1);
                    if (!owner.hasMethod("<init>", d)) continue;
                    MethodInsnNode conv = converter(h, cls, cls.equals(blockCls) && isItem ? "mcc$fromItemCtorId" : "mcc$fromId");
                    if (conv == null) continue;
                    targetDesc = d;
                    plan.add(new Param(0, conv));
                    for (int i = 0; i < rest.length; i++) plan.add(new Param(i + 1, null));
                    break;
                }
            }
        }
        if (targetDesc == null) {
            // ids in other positions: the shim constructor of the same arity with Block/Item where an int was
            String best = null;
            List<Param> bestPlan = null;
            Map<String, Integer> sorted = new TreeMap<>(owner.methods);
            for (Map.Entry<String, Integer> e : sorted.entrySet()) {
                if (!e.getKey().startsWith("<init>(") || (e.getValue() & Opcodes.ACC_PRIVATE) != 0) continue;
                String d = e.getKey().substring("<init>".length());
                Type[] t = Type.getArgumentTypes(d);
                if (t.length != args.length) continue;
                List<Param> p = new ArrayList<>();
                int conversions = 0;
                boolean ok = true;
                for (int j = 0; j < t.length && ok; j++) {
                    if (t[j].equals(args[j])) {
                        p.add(new Param(j, null));
                    } else if (args[j].getSort() == Type.INT && t[j].getSort() == Type.OBJECT
                            && (t[j].getInternalName().equals(blockCls) || t[j].getInternalName().equals(itemCls))) {
                        MethodInsnNode conv = converter(h, t[j].getInternalName(), "mcc$fromId");
                        if (conv == null) ok = false;
                        else {
                            p.add(new Param(j, conv));
                            conversions++;
                        }
                    } else {
                        ok = false;
                    }
                }
                if (ok && conversions > 0 && best == null) {
                    best = d;
                    bestPlan = p;
                }
            }
            if (best != null) {
                targetDesc = best;
                plan = bestPlan;
            } else if (leadingId && (isBlock || isItem)) {
                Type[] rest = java.util.Arrays.copyOfRange(args, 1, args.length);
                CtorChooser.Choice choice = CtorChooser.choose(owner, rest);
                if (choice == null) return null;
                targetDesc = choice.desc();
                plan.clear();
                for (int j : choice.map()) plan.add(new Param(j < 0 ? -1 : j + 1, null));
            }
        }
        if (targetDesc == null) return null;

        // claim hook: the facade of the Block/Item base class
        String claimOwner = null;
        String claimDesc = null;
        String base = !leadingId ? null : isBlock ? blockCls : isItem ? itemCls : null;
        if (base != null) {
            String facade = h.facadeOf(base);
            String d = "(L" + base + ";I)V";
            if (facade != null && h.find(facade).hasMethod("mcc$claimId", d)) {
                claimOwner = facade;
                claimDesc = d;
            }
        }

        InsnList pre = new InsnList();
        int[] slots = new int[args.length];
        int slot = method.maxLocals;
        for (int i = 0; i < args.length; i++) {
            slots[i] = slot;
            slot += args[i].getSize();
        }
        for (int i = args.length - 1; i >= 0; i--) pre.add(new VarInsnNode(args[i].getOpcode(Opcodes.ISTORE), slots[i]));
        if (claimOwner != null) pre.add(new InsnNode(Opcodes.DUP)); // the receiver stays on the stack for the claim after the call
        Type[] target = Type.getArgumentTypes(targetDesc);
        int pushed = 0;
        for (int j = 0; j < target.length; j++) {
            Param p = plan.get(j);
            if (p.arg() < 0) {
                pre.add(new InsnNode(CtorChooser.defaultConst(target[j])));
            } else {
                pre.add(new VarInsnNode(args[p.arg()].getOpcode(Opcodes.ILOAD), slots[p.arg()]));
                if (p.convert() != null) pre.add(new MethodInsnNode(Opcodes.INVOKESTATIC, p.convert().owner, p.convert().name, p.convert().desc, false));
            }
            pushed += target[j].getSize();
        }
        method.maxLocals = slot;
        method.maxStack += pushed + 2;
        method.instructions.insertBefore(m, pre);
        if (claimOwner != null) {
            InsnList post = new InsnList();
            post.add(new VarInsnNode(Opcodes.ILOAD, slots[0]));
            post.add(new MethodInsnNode(Opcodes.INVOKESTATIC, claimOwner, "mcc$claimId", claimDesc, false));
            method.instructions.insert(m, post);
        }
        report.adapted(m.owner, m.name + m.desc + " -> " + targetDesc, from);
        m.desc = targetDesc;
        return m;
    }

    /** {@code facade-of-cls.name(I)Lcls;}, or null when the version has no such facade method. */
    private static MethodInsnNode converter(Hierarchy h, String cls, String name) {
        String facade = h.facadeOf(cls);
        String desc = "(I)L" + cls + ";";
        if (facade == null || !h.find(facade).hasMethod(name, desc)) return null;
        return new MethodInsnNode(Opcodes.INVOKESTATIC, facade, name, desc, false);
    }
}
