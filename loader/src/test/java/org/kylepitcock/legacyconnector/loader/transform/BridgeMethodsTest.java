package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Bridge methods for 1.6.4 overrides on synthetic class nodes (a fake 1.7.10 shim with Block, Item, World and the id facades). */
class BridgeMethodsTest implements Opcodes {
    private static final String P7 = "legacyconnector/v1_7_10/net/minecraft/";
    private static final String P6 = "legacyconnector/v1_6_4/facade/net/minecraft/";
    private static final String BLOCK = P7 + "block/Block";
    private static final String ITEM = P7 + "item/Item";
    private static final String WORLD = P7 + "world/World";
    private static final String ACCESS = P7 + "world/IBlockAccess";
    private static final String PLANTABLE = "legacyconnector/v1_7_10/net/minecraftforge/common/IPlantable";
    private static final String BREAK_OLD = "(L" + WORLD + ";IIIII)V";
    private static final String BREAK_NEW = "(L" + WORLD + ";IIIL" + BLOCK + ";I)V";
    private static final String NEIGHBOR_OLD = "(L" + WORLD + ";IIII)V";
    private static final String NEIGHBOR_NEW = "(L" + WORLD + ";IIIL" + BLOCK + ";)V";

    private static final String RF_CONN = "legacyconnector/v1_7_10/cofh/api/energy/IEnergyConnection";
    private static final String FD = "legacyconnector/v1_7_10/net/minecraftforge/common/util/ForgeDirection";

    private static ClassInfo cls(String name, String sup, String... itf) {
        return new ClassInfo(name, sup, List.of(itf), ACC_PUBLIC);
    }

    private static Hierarchy hierarchy() {
        Map<String, ClassInfo> shim = new HashMap<>();
        ClassInfo obj = cls("java/lang/Object", null);
        ClassInfo block = cls(BLOCK, "java/lang/Object");
        block.methods.put("breakBlock" + BREAK_NEW, ACC_PUBLIC);
        block.methods.put("onNeighborBlockChange" + NEIGHBOR_NEW, ACC_PUBLIC);
        ClassInfo item = cls(ITEM, "java/lang/Object");
        ClassInfo world = cls(WORLD, "java/lang/Object", ACCESS);
        ClassInfo access = new ClassInfo(ACCESS, "java/lang/Object", List.of(), ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
        ClassInfo plantable = new ClassInfo(PLANTABLE, "java/lang/Object", List.of(), ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
        ClassInfo fBlock = cls(P6 + "block/Block", "java/lang/Object");
        fBlock.methods.put("mcc$get$blockID(L" + BLOCK + ";)I", ACC_PUBLIC | ACC_STATIC);
        fBlock.methods.put("mcc$fromId(I)L" + BLOCK + ";", ACC_PUBLIC | ACC_STATIC);
        ClassInfo fItem = cls(P6 + "item/Item", "java/lang/Object");
        fItem.methods.put("mcc$get$itemID(L" + ITEM + ";)I", ACC_PUBLIC | ACC_STATIC);
        fItem.methods.put("mcc$fromId(I)L" + ITEM + ";", ACC_PUBLIC | ACC_STATIC);
        ClassInfo rfConn = new ClassInfo(RF_CONN, "java/lang/Object", List.of(), ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
        rfConn.methods.put("canConnectEnergy(L" + FD + ";)Z", ACC_PUBLIC | ACC_ABSTRACT);
        for (ClassInfo c : List.of(obj, block, item, world, access, plantable, fBlock, fItem, rfConn, cls(FD, "java/lang/Object"))) shim.put(c.name, c);
        return new Hierarchy(new ShimIndex(shim), null, Map.of(), LegacyVersion.V1_6_4);
    }

    private static ClassNode mod(Hierarchy h, String name, String sup, java.util.function.Consumer<ClassNode> body) {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = sup;
        body.accept(n);
        h.addLocal(ClassInfo.of(n));
        return n;
    }

    private static MethodNode method(ClassNode n, String name, String desc) {
        MethodNode m = new MethodNode(ACC_PUBLIC, name, desc, null, null);
        m.instructions.add(new InsnNode(RETURN));
        n.methods.add(m);
        return m;
    }

    private static MethodNode find(ClassNode n, String name, String desc) {
        for (MethodNode m : n.methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        return null;
    }

    private static List<String> ops(MethodNode m) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode c) out.add(c.getOpcode() + ":" + c.owner + "." + c.name + c.desc);
        }
        return out;
    }

    @Test
    void anIdOverrideGetsAShimShapedBridgeThatConvertsTheBlockToItsId() {
        Hierarchy h = hierarchy();
        ClassNode mod = mod(h, "mod/MyBlock", BLOCK, n -> method(n, "breakBlock", BREAK_OLD));
        assertEquals(1, BridgeMethods.apply(mod, h, LegacyVersion.V1_6_4));
        MethodNode bridge = find(mod, "breakBlock", BREAK_NEW);
        assertNotNull(bridge);
        assertEquals(List.of(INVOKESTATIC + ":" + P6 + "block/Block.mcc$get$blockID(L" + BLOCK + ";)I",
                INVOKEVIRTUAL + ":mod/MyBlock.breakBlock" + BREAK_OLD), ops(bridge));
        assertEquals(7, bridge.maxLocals); // this, world, x, y, z, block, meta
        assertTrue(bridge.maxStack >= 7);
        // the mod's own method is untouched
        assertNotNull(find(mod, "breakBlock", BREAK_OLD));
    }

    @Test
    void bridgesInheritedThroughAModSuperclassOnlyWhereDeclared() {
        Hierarchy h = hierarchy();
        ClassNode base = mod(h, "mod/Base", BLOCK, n -> method(n, "onNeighborBlockChange", NEIGHBOR_OLD));
        ClassNode sub = mod(h, "mod/Sub", "mod/Base", n -> {});
        ClassNode other = mod(h, "mod/Other", "java/lang/Object", n -> method(n, "onNeighborBlockChange", NEIGHBOR_OLD));
        assertEquals(1, BridgeMethods.apply(base, h, LegacyVersion.V1_6_4));
        assertEquals(0, BridgeMethods.apply(sub, h, LegacyVersion.V1_6_4));
        assertEquals(0, BridgeMethods.apply(other, h, LegacyVersion.V1_6_4)); // not a Block
        assertNull(find(other, "onNeighborBlockChange", NEIGHBOR_NEW));
    }

    @Test
    void anAlreadyShimShapedMethodGetsNoBridge() {
        Hierarchy h = hierarchy();
        ClassNode mod = mod(h, "mod/Fine", BLOCK, n -> method(n, "breakBlock", BREAK_NEW));
        assertEquals(0, BridgeMethods.apply(mod, h, LegacyVersion.V1_6_4));
        assertEquals(1, mod.methods.size());
    }

    @Test
    void resultsAreConvertedBackFromTheModsIdToAnItem() {
        Hierarchy h = hierarchy();
        String oldDesc = "(ILjava/util/Random;I)I";
        String newDesc = "(ILjava/util/Random;I)L" + ITEM + ";";
        ClassNode mod = mod(h, "mod/Ore", BLOCK, n -> method(n, "idDropped", oldDesc));
        assertEquals(1, BridgeMethods.apply(mod, h, LegacyVersion.V1_6_4));
        MethodNode bridge = find(mod, "getItemDropped", newDesc);
        assertNotNull(bridge);
        assertEquals(List.of(INVOKEVIRTUAL + ":mod/Ore.idDropped" + oldDesc,
                INVOKESTATIC + ":" + P6 + "item/Item.mcc$fromId(I)L" + ITEM + ";"), ops(bridge));
        assertEquals(ARETURN, bridge.instructions.getLast().getOpcode());
    }

    @Test
    void differingReferenceParametersAreCast() {
        Hierarchy h = hierarchy();
        ClassNode mod = mod(h, "mod/Seed", ITEM, n -> {
            n.interfaces.add(PLANTABLE);
            method(n, "getPlantMetadata", "(L" + WORLD + ";III)I");
        });
        assertEquals(1, BridgeMethods.apply(mod, h, LegacyVersion.V1_6_4));
        MethodNode bridge = find(mod, "getPlantMetadata", "(L" + ACCESS + ";III)I");
        assertNotNull(bridge);
        boolean cast = false;
        for (AbstractInsnNode i = bridge.instructions.getFirst(); i != null; i = i.getNext()) cast |= i.getOpcode() == CHECKCAST;
        assertTrue(cast);
    }

    @Test
    void superCallsAndCallsOnShimTypesUseTheNewMethodWithConvertedArguments() {
        Hierarchy h = hierarchy();
        ClassNode mod = mod(h, "mod/Caller", BLOCK, n -> {
            MethodNode m = new MethodNode(ACC_PUBLIC, "onNeighborBlockChange", NEIGHBOR_OLD, null, null);
            m.maxLocals = 6;
            m.maxStack = 6;
            m.instructions.add(new VarInsnNode(ALOAD, 0));
            m.instructions.add(new VarInsnNode(ALOAD, 1));
            m.instructions.add(new VarInsnNode(ILOAD, 2));
            m.instructions.add(new VarInsnNode(ILOAD, 3));
            m.instructions.add(new VarInsnNode(ILOAD, 4));
            m.instructions.add(new VarInsnNode(ILOAD, 5));
            m.instructions.add(new MethodInsnNode(INVOKESPECIAL, BLOCK, "onNeighborBlockChange", NEIGHBOR_OLD, false));
            m.instructions.add(new InsnNode(RETURN));
            n.methods.add(m);
        });
        assertEquals(2, BridgeMethods.apply(mod, h, LegacyVersion.V1_6_4)); // the super call and the bridge
        MethodNode own = find(mod, "onNeighborBlockChange", NEIGHBOR_OLD);
        assertEquals(List.of(INVOKESTATIC + ":" + P6 + "block/Block.mcc$fromId(I)L" + BLOCK + ";",
                INVOKESPECIAL + ":" + BLOCK + ".onNeighborBlockChange" + NEIGHBOR_NEW), ops(own));
        assertTrue(own.maxLocals >= 7);
    }

    @Test
    void aCallOfTheModsOwnOldMethodIsKept() {
        Hierarchy h = hierarchy();
        ClassNode mod = mod(h, "mod/Own", BLOCK, n -> method(n, "onNeighborBlockChange", NEIGHBOR_OLD));
        ClassNode user = mod(h, "mod/User", "java/lang/Object", n -> {
            MethodNode m = new MethodNode(ACC_PUBLIC, "run", "()V", null, null);
            m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "mod/Own", "onNeighborBlockChange", NEIGHBOR_OLD, false));
            m.instructions.add(new InsnNode(RETURN));
            n.methods.add(m);
        });
        assertEquals(0, BridgeMethods.apply(user, h, LegacyVersion.V1_6_4));
        assertEquals("mod/Own", ((MethodInsnNode) find(user, "run", "()V").instructions.getFirst()).owner);
    }

    @Test
    void aCallThroughTheShimTypeIsRewrittenAndTheResultConverted() {
        Hierarchy h = hierarchy();
        // dropItemId-style: int result of an old method on a shim type comes back from the Item
        ClassNode user = mod(h, "mod/User2", "java/lang/Object", n -> {
            MethodNode m = new MethodNode(ACC_PUBLIC, "run", "()I", null, null);
            m.instructions.add(new VarInsnNode(ALOAD, 0));
            m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, BLOCK, "idDropped", "(ILjava/util/Random;I)I", false));
            m.instructions.add(new InsnNode(IRETURN));
            n.methods.add(m);
        });
        // callApplies needs the shim receiver to be below the rule's owner (Block): it is, so the result is Item -> int
        assertEquals(1, BridgeMethods.apply(user, h, LegacyVersion.V1_6_4));
        MethodNode run = find(user, "run", "()I");
        assertEquals(List.of(INVOKEVIRTUAL + ":" + BLOCK + ".getItemDropped(ILjava/util/Random;I)L" + ITEM + ";",
                INVOKESTATIC + ":" + P6 + "item/Item.mcc$get$itemID(L" + ITEM + ";)I"), ops(run));
    }

    // ------------------------------------------------------------------ 1.5.2: int health and damage

    private static final String ENTITY = P7 + "entity/Entity";
    private static final String DAMAGE = P7 + "util/DamageSource";

    private static Hierarchy hierarchy152() {
        Map<String, ClassInfo> shim = new HashMap<>();
        ClassInfo entity = cls(ENTITY, "java/lang/Object");
        entity.methods.put("attackEntityFrom(L" + DAMAGE + ";F)Z", ACC_PUBLIC);
        for (ClassInfo c : List.of(cls("java/lang/Object", null), entity, cls(DAMAGE, "java/lang/Object"))) shim.put(c.name, c);
        return new Hierarchy(new ShimIndex(shim), null, Map.of(), LegacyVersion.V1_5_2);
    }

    @Test
    void anIntDamageOverrideGetsAFloatBridgeThatNarrowsTheArgument() {
        Hierarchy h = hierarchy152();
        ClassNode mod = mod(h, "mod/MyMob", ENTITY, n -> {
            MethodNode m = new MethodNode(ACC_PUBLIC, "attackEntityFrom", "(L" + DAMAGE + ";I)Z", null, null);
            m.instructions.add(new InsnNode(ICONST_1));
            m.instructions.add(new InsnNode(IRETURN));
            n.methods.add(m);
        });
        assertTrue(BridgeMethods.apply(mod, h, LegacyVersion.V1_5_2) >= 1);
        MethodNode bridge = find(mod, "attackEntityFrom", "(L" + DAMAGE + ";F)Z");
        assertNotNull(bridge);
        boolean narrows = false;
        for (AbstractInsnNode i = bridge.instructions.getFirst(); i != null; i = i.getNext()) narrows |= i.getOpcode() == F2I;
        assertTrue(narrows, "the float amount is converted to the mod's int");
    }

    @Test
    void anIntDamageCallIsWidenedToTheShimsFloat() {
        Hierarchy h = hierarchy152();
        ClassNode caller = mod(h, "mod/Caller", "java/lang/Object", n -> {
            MethodNode m = new MethodNode(ACC_PUBLIC, "hit", "(L" + ENTITY + ";L" + DAMAGE + ";)V", null, null);
            m.instructions.add(new VarInsnNode(ALOAD, 1));
            m.instructions.add(new VarInsnNode(ALOAD, 2));
            m.instructions.add(new org.objectweb.asm.tree.IntInsnNode(BIPUSH, 5));
            m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, ENTITY, "attackEntityFrom", "(L" + DAMAGE + ";I)Z", false));
            m.instructions.add(new InsnNode(POP));
            m.instructions.add(new InsnNode(RETURN));
            n.methods.add(m);
        });
        assertEquals(1, BridgeMethods.apply(caller, h, LegacyVersion.V1_5_2));
        MethodNode hit = find(caller, "hit", "(L" + ENTITY + ";L" + DAMAGE + ";)V");
        boolean widens = false;
        String called = null;
        for (AbstractInsnNode i = hit.instructions.getFirst(); i != null; i = i.getNext()) {
            widens |= i.getOpcode() == I2F;
            if (i instanceof MethodInsnNode c) called = c.name + c.desc;
        }
        assertTrue(widens);
        assertEquals("attackEntityFrom(L" + DAMAGE + ";F)Z", called);
    }

    @Test
    void aCofhEnergyHandlerWithCanInterfaceGetsCanConnectEnergy() {
        LegacyVersion.setBridgedRoots(List.of("cofh/api/energy/"));
        try {
            Hierarchy h = hierarchy();
            ClassNode mod = mod(h, "mod/Tile", "java/lang/Object", n -> {
                n.interfaces.add(RF_CONN);
                method(n, "canInterface", "(L" + FD + ";)Z");
            });
            assertEquals(1, BridgeMethods.apply(mod, h, LegacyVersion.V1_6_4));
            MethodNode bridge = find(mod, "canConnectEnergy", "(L" + FD + ";)Z");
            assertNotNull(bridge);
            assertEquals(List.of(INVOKEVIRTUAL + ":mod/Tile.canInterface(L" + FD + ";)Z"), ops(bridge));
            assertEquals(IRETURN, bridge.instructions.getLast().getOpcode());
        } finally {
            LegacyVersion.setBridgedRoots(List.of());
        }
    }
}
