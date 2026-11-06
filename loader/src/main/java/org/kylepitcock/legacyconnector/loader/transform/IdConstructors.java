package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
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
 * Constructors of a forward version that take a numeric id as the first argument (1.6.4: {@code new Item(id)},
 * {@code super(id, Material.rock)}, {@code new ItemStack(id, 1, 0)}) onto the shim's id-less ones. Three cases, all through the
 * facade classes of the version ({@link Hierarchy#facadeOf}):
 * <ul>
 *     <li>a Block or Item subclass whose shim constructor is the same without the id: that one is called and the new object
 *     is claimed under the id afterwards ({@code mcc$claimId(Object, int)} of the Block/Item facade, which owns the id tables);</li>
 *     <li>a shim constructor taking a Block or Item where the id was ({@code ItemStack(Item, int, int)}, {@code ItemBlock(Block)}):
 *     the id is turned into that object by the facade ({@code mcc$fromId});</li>
 *     <li>otherwise the id is dropped and the closest constructor of {@link CtorChooser} is used for the rest.</li>
 * </ul>
 * Done by spilling the arguments to fresh locals inside one basic block, like {@code ShimLinker.adaptConstructor}.
 */
final class IdConstructors {
    private IdConstructors() {}

    /** @return the adapted call (the instruction that now stands for {@code m}), or null when this is not an id constructor. */
    static AbstractInsnNode adapt(MethodNode method, MethodInsnNode m, Type[] args, Hierarchy h, LinkReport report, String from) {
        if (args.length == 0 || args[0].getSort() != Type.INT) return null;
        String link = h.version().linkPrefix();
        String blockCls = link + "net/minecraft/block/Block";
        String itemCls = link + "net/minecraft/item/Item";
        boolean isBlock = h.isSubclassOf(m.owner, blockCls);
        boolean isItem = h.isSubclassOf(m.owner, itemCls);
        ClassInfo owner = h.find(m.owner);
        if (owner == null) return null;
        Type[] rest = java.util.Arrays.copyOfRange(args, 1, args.length);
        StringBuilder restDesc = new StringBuilder("(");
        for (Type t : rest) restDesc.append(t.getDescriptor());
        restDesc.append(")V");

        String targetDesc = null;
        // per target parameter: index into args (-1 = default), and a converter applied to the loaded argument
        List<int[]> plan = new ArrayList<>();
        MethodInsnNode convert = null;

        if ((isBlock || isItem) && owner.hasMethod("<init>", restDesc.toString())) {
            targetDesc = restDesc.toString();
            for (int i = 0; i < rest.length; i++) plan.add(new int[] {i + 1});
        } else {
            // the id becomes a Block/Item parameter: ItemStack(int, int, int) -> ItemStack(Item, int, int), ItemBlock(int) -> ItemBlock(Block)
            for (String cls : new String[] {itemCls, blockCls}) {
                String d = "(L" + cls + ";" + restDesc.substring(1);
                if (!owner.hasMethod("<init>", d)) continue;
                String facade = h.facadeOf(cls);
                if (facade == null) continue;
                String fn = cls.equals(blockCls) && isItem ? "mcc$fromItemCtorId" : "mcc$fromId";
                String fd = "(I)L" + cls + ";";
                if (!h.find(facade).hasMethod(fn, fd)) continue;
                targetDesc = d;
                convert = new MethodInsnNode(Opcodes.INVOKESTATIC, facade, fn, fd, false);
                plan.add(new int[] {0});
                for (int i = 0; i < rest.length; i++) plan.add(new int[] {i + 1});
                break;
            }
            if (targetDesc == null && (isBlock || isItem)) {
                CtorChooser.Choice choice = CtorChooser.choose(owner, rest);
                if (choice == null) return null;
                targetDesc = choice.desc();
                for (int j : choice.map()) plan.add(new int[] {j < 0 ? -1 : j + 1});
            }
        }
        if (targetDesc == null) return null;

        // claim hook: the facade of the Block/Item base class
        String claimOwner = null;
        String claimDesc = null;
        String base = isBlock ? blockCls : isItem ? itemCls : null;
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
            int i = plan.get(j)[0];
            if (i < 0) {
                pre.add(new InsnNode(CtorChooser.defaultConst(target[j])));
            } else {
                pre.add(new VarInsnNode(args[i].getOpcode(Opcodes.ILOAD), slots[i]));
                if (i == 0 && convert != null) pre.add(new MethodInsnNode(Opcodes.INVOKESTATIC, convert.owner, convert.name, convert.desc, false));
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
}
