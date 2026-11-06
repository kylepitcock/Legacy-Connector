package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The 1.6.4 id facade as the linker applies it: a fake 1.7.10 shim (Block, Item, ItemStack, World with only the 1.7.10 members) and the
 * facade classes next to it, and a 1.6.4 style mod class compiled against the ids. Checks which instructions the linker leaves.
 */
class IdFacadeLinkTest implements Opcodes {
    private static final String P7 = "legacyconnector/v1_7_10/net/minecraft/";
    private static final String P6 = "legacyconnector/v1_6_4/net/minecraft/";
    private static final String BLOCK = P7 + "block/Block";
    private static final String ITEM = P7 + "item/Item";
    private static final String STACK = P7 + "item/ItemStack";
    private static final String WORLD = P7 + "world/World";
    private static final String ACCESS = P7 + "world/IBlockAccess";
    private static final String MATERIAL = P7 + "block/material/Material";

    private static ClassInfo cls(String name, String sup, String... itf) {
        return new ClassInfo(name, sup, List.of(itf), ACC_PUBLIC);
    }

    private static Hierarchy hierarchy() {
        Map<String, ClassInfo> shim = new HashMap<>();
        ClassInfo obj = cls("java/lang/Object", null);
        ClassInfo block = cls(BLOCK, "java/lang/Object");
        block.methods.put("<init>(L" + MATERIAL + ";)V", ACC_PROTECTED);
        ClassInfo item = cls(ITEM, "java/lang/Object");
        item.methods.put("<init>()V", ACC_PUBLIC);
        ClassInfo food = cls(P7 + "item/ItemFood", ITEM);
        food.methods.put("<init>(IFZ)V", ACC_PUBLIC);
        ClassInfo itemBlock = cls(P7 + "item/ItemBlock", ITEM);
        itemBlock.methods.put("<init>(L" + BLOCK + ";)V", ACC_PUBLIC);
        ClassInfo stack = cls(STACK, "java/lang/Object");
        stack.methods.put("<init>(L" + ITEM + ";II)V", ACC_PUBLIC);
        ClassInfo access = new ClassInfo(ACCESS, "java/lang/Object", List.of(), ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
        ClassInfo world = cls(WORLD, "java/lang/Object", ACCESS);
        ClassInfo material = cls(MATERIAL, "java/lang/Object");
        for (ClassInfo c : List.of(obj, block, item, food, itemBlock, stack, access, world, material)) shim.put(c.name, c);

        // the facades of the version
        ClassInfo fBlock = cls(P6 + "block/Block", "java/lang/Object");
        fBlock.methods.put("mcc$get$blockID(L" + BLOCK + ";)I", ACC_PUBLIC | ACC_STATIC);
        fBlock.methods.put("mcc$get$dirt()L" + BLOCK + ";", ACC_PUBLIC | ACC_STATIC);
        fBlock.methods.put("mcc$claimId(L" + BLOCK + ";I)V", ACC_PUBLIC | ACC_STATIC);
        fBlock.methods.put("mcc$fromId(I)L" + BLOCK + ";", ACC_PUBLIC | ACC_STATIC);
        fBlock.methods.put("mcc$fromItemCtorId(I)L" + BLOCK + ";", ACC_PUBLIC | ACC_STATIC);
        ClassInfo fItem = cls(P6 + "item/Item", "java/lang/Object");
        fItem.methods.put("mcc$claimId(L" + ITEM + ";I)V", ACC_PUBLIC | ACC_STATIC);
        fItem.methods.put("mcc$fromId(I)L" + ITEM + ";", ACC_PUBLIC | ACC_STATIC);
        ClassInfo fAccess = cls(P6 + "world/IBlockAccess", "java/lang/Object");
        fAccess.methods.put("getBlockId(L" + ACCESS + ";III)I", ACC_PUBLIC | ACC_STATIC);
        for (ClassInfo c : List.of(fBlock, fItem, fAccess)) shim.put(c.name, c);
        return new Hierarchy(new ShimIndex(shim), null, Map.of(), LegacyVersion.V1_6_4);
    }

    private static ClassNode mod(Hierarchy h, java.util.function.Consumer<ClassNode> body) {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = "mod/Sample";
        n.superName = "java/lang/Object";
        body.accept(n);
        h.addLocal(ClassInfo.of(n));
        ShimLinker.apply(n, h, new LinkReport(), new SynthDemand());
        return n;
    }

    private static MethodNode method(ClassNode n, String name, String desc) {
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, name, desc, null, null);
        n.methods.add(m);
        return m;
    }

    private static List<String> calls(MethodNode m) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode c) out.add(c.owner + "." + c.name + c.desc);
        }
        return out;
    }

    @Test
    void blockIdFieldReadBecomesFacadeCall() {
        Hierarchy h = hierarchy();
        ClassNode n = mod(h, c -> {
            MethodNode m = method(c, "id", "(L" + BLOCK + ";)I");
            m.visitVarInsn(ALOAD, 0);
            m.visitFieldInsn(GETFIELD, BLOCK, "blockID", "I");
            m.visitInsn(IRETURN);
        });
        MethodNode m = n.methods.get(0);
        assertEquals(List.of(P6 + "block/Block.mcc$get$blockID(L" + BLOCK + ";)I"), calls(m));
    }

    @Test
    void staticVanillaBlockBecomesFacadeCall() {
        Hierarchy h = hierarchy();
        ClassNode n = mod(h, c -> {
            MethodNode m = method(c, "dirt", "()L" + BLOCK + ";");
            m.visitFieldInsn(GETSTATIC, BLOCK, "dirt", "L" + BLOCK + ";");
            m.visitInsn(ARETURN);
        });
        assertEquals(List.of(P6 + "block/Block.mcc$get$dirt()L" + BLOCK + ";"), calls(n.methods.get(0)));
    }

    @Test
    void getBlockIdOnWorldGoesThroughTheIBlockAccessFacade() {
        Hierarchy h = hierarchy();
        ClassNode n = mod(h, c -> {
            MethodNode m = method(c, "at", "(L" + WORLD + ";)I");
            m.visitVarInsn(ALOAD, 0);
            m.visitInsn(ICONST_1);
            m.visitInsn(ICONST_2);
            m.visitInsn(ICONST_3);
            m.visitMethodInsn(INVOKEVIRTUAL, WORLD, "getBlockId", "(III)I", false);
            m.visitInsn(IRETURN);
        });
        assertEquals(List.of(P6 + "world/IBlockAccess.getBlockId(L" + ACCESS + ";III)I"), calls(n.methods.get(0)));
    }

    @Test
    void blockConstructorDropsTheIdAndClaimsIt() {
        Hierarchy h = hierarchy();
        ClassNode n = mod(h, c -> {
            c.superName = BLOCK;
            MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", "(I)V", null, null);
            m.maxLocals = 2;
            c.methods.add(m);
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ILOAD, 1);
            m.visitInsn(ACONST_NULL);
            m.visitMethodInsn(INVOKESPECIAL, BLOCK, "<init>", "(IL" + MATERIAL + ";)V", false);
            m.visitInsn(RETURN);
        });
        // super(Material) and the claim of the original id follow
        assertEquals(List.of(BLOCK + ".<init>(L" + MATERIAL + ";)V", P6 + "block/Block.mcc$claimId(L" + BLOCK + ";I)V"), calls(n.methods.get(0)));
        assertTrue(n.methods.get(0).maxLocals >= 4, "arguments were spilled to fresh locals");
    }

    @Test
    void itemSubclassConstructorKeepsTheOtherArguments() {
        Hierarchy h = hierarchy();
        ClassNode n = mod(h, c -> {
            c.superName = P7 + "item/ItemFood";
            MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", "(I)V", null, null);
            c.methods.add(m);
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ILOAD, 1);
            m.visitIntInsn(BIPUSH, 4);
            m.visitInsn(FCONST_1);
            m.visitInsn(ICONST_0);
            m.visitMethodInsn(INVOKESPECIAL, P7 + "item/ItemFood", "<init>", "(IIFZ)V", false);
            m.visitInsn(RETURN);
        });
        assertEquals(List.of(P7 + "item/ItemFood.<init>(IFZ)V", P6 + "item/Item.mcc$claimId(L" + ITEM + ";I)V"), calls(n.methods.get(0)));
    }

    @Test
    void itemBlockConstructorTakesTheBlockOfItsId() {
        Hierarchy h = hierarchy();
        ClassNode n = mod(h, c -> {
            c.superName = P7 + "item/ItemBlock";
            MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", "(I)V", null, null);
            c.methods.add(m);
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ILOAD, 1);
            m.visitMethodInsn(INVOKESPECIAL, P7 + "item/ItemBlock", "<init>", "(I)V", false);
            m.visitInsn(RETURN);
        });
        assertEquals(List.of(P6 + "block/Block.mcc$fromItemCtorId(I)L" + BLOCK + ";", P7 + "item/ItemBlock.<init>(L" + BLOCK + ";)V",
                P6 + "item/Item.mcc$claimId(L" + ITEM + ";I)V"), calls(n.methods.get(0)));
    }

    @Test
    void itemStackFromIdsUsesTheItemOfTheId() {
        Hierarchy h = hierarchy();
        ClassNode n = mod(h, c -> {
            MethodNode m = method(c, "stack", "(I)L" + STACK + ";");
            m.visitTypeInsn(NEW, STACK);
            m.visitInsn(DUP);
            m.visitVarInsn(ILOAD, 0);
            m.visitInsn(ICONST_1);
            m.visitInsn(ICONST_0);
            m.visitMethodInsn(INVOKESPECIAL, STACK, "<init>", "(III)V", false);
            m.visitInsn(ARETURN);
        });
        assertEquals(List.of(P6 + "item/Item.mcc$fromId(I)L" + ITEM + ";", STACK + ".<init>(L" + ITEM + ";II)V"), calls(n.methods.get(0)));
    }
}
