package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * {@link IdConstructors#adapt} called directly: the numeric-id constructors of the forward versions turned into the shim's object
 * constructors, for each plan the class documents (drop the id and claim it, id to Block/Item parameter, ints to objects at other
 * positions, closest constructor with defaults, 1.4.7 texture claims) plus the operand spill and stack bookkeeping.
 */
class IdConstructorsTest implements Opcodes {
    private static ClassInfo cls(String name, String sup) {
        return new ClassInfo(name, sup, List.of(), ACC_PUBLIC);
    }

    /** The version is 1.4.7 (flat names) or 1.6.4; both facade prefixes follow {@code shimPrefix()} so the test asks the version. */
    private static Hierarchy hierarchy(LegacyVersion version, boolean texture) {
        String facadePrefix = version.shimPrefix() + "facade/net/minecraft/";
        String link = version.linkPrefix() + "net/minecraft/";
        String block = link + "block/Block", item = link + "item/Item", material = link + "block/material/Material";
        Map<String, ClassInfo> shim = new HashMap<>();
        ClassInfo obj = cls("java/lang/Object", null);
        ClassInfo b = cls(block, "java/lang/Object");
        b.methods.put("<init>(L" + material + ";)V", ACC_PROTECTED);
        ClassInfo liquid = cls(link + "block/BlockLiquid", block);
        liquid.methods.put("<init>(L" + material + ";)V", ACC_PROTECTED);
        ClassInfo i = cls(item, "java/lang/Object");
        i.methods.put("<init>()V", ACC_PUBLIC);
        ClassInfo odd = cls(link + "item/ItemOdd", item);
        odd.methods.put("<init>(D)V", ACC_PUBLIC);
        ClassInfo gen = cls(link + "world/gen/feature/WorldGenThing", "java/lang/Object");
        gen.methods.put("<init>(L" + block + ";IL" + block + ";)V", ACC_PUBLIC);
        gen.methods.put("<init>(Ljava/lang/String;L" + item + ";)V", ACC_PUBLIC);
        ClassInfo plain = cls(link + "world/Plain", "java/lang/Object");
        plain.methods.put("<init>(Ljava/lang/String;)V", ACC_PUBLIC);
        ClassInfo mat = cls(material, "java/lang/Object");
        for (ClassInfo c : List.of(obj, b, liquid, i, odd, gen, plain, mat)) shim.put(c.name, c);

        ClassInfo fBlock = cls(facadePrefix + "block/Block", "java/lang/Object");
        fBlock.methods.put("mcc$claimId(L" + block + ";I)V", ACC_PUBLIC | ACC_STATIC);
        fBlock.methods.put("mcc$fromId(I)L" + block + ";", ACC_PUBLIC | ACC_STATIC);
        ClassInfo fItem = cls(facadePrefix + "item/Item", "java/lang/Object");
        fItem.methods.put("mcc$claimId(L" + item + ";I)V", ACC_PUBLIC | ACC_STATIC);
        fItem.methods.put("mcc$fromId(I)L" + item + ";", ACC_PUBLIC | ACC_STATIC);
        if (texture) {
            fBlock.methods.put("mcc$claimTexture(L" + block + ";I)V", ACC_PUBLIC | ACC_STATIC);
            fBlock.methods.put("mcc$claimFluidTexture(L" + block + ";L" + material + ";)V", ACC_PUBLIC | ACC_STATIC);
            fItem.methods.put("mcc$claimItemTexture(L" + item + ";)V", ACC_PUBLIC | ACC_STATIC);
        }
        shim.put(fBlock.name, fBlock);
        shim.put(fItem.name, fItem);
        return new Hierarchy(new ShimIndex(shim), null, Map.of(), version);
    }

    private static String link(Hierarchy h, String rest) {
        return h.version().linkPrefix() + "net/minecraft/" + rest;
    }

    private record Run(MethodNode method, MethodInsnNode call, AbstractInsnNode result, LinkReport report) {}

    /** Builds {@code ctor(args)} on a mod class's method: the receiver, the arguments, the call. */
    private static Run run(Hierarchy h, String owner, String desc) {
        MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", desc, null, null);
        Type[] args = Type.getArgumentTypes(desc);
        m.maxLocals = 1 + Type.getArgumentsAndReturnSizes(desc) / 4;
        m.maxStack = 1;
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        for (int i = 0, slot = 1; i < args.length; i++) {
            m.instructions.add(new VarInsnNode(args[i].getOpcode(ILOAD), slot));
            slot += args[i].getSize();
        }
        MethodInsnNode call = new MethodInsnNode(INVOKESPECIAL, owner, "<init>", desc, false);
        m.instructions.add(call);
        m.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        LinkReport report = new LinkReport();
        AbstractInsnNode result = IdConstructors.adapt(m, call, args, h, report, "mod/Sample");
        return new Run(m, call, result, report);
    }

    private static List<String> calls(MethodNode m) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode c) out.add(c.owner + "." + c.name + c.desc);
        }
        return out;
    }

    @Test
    void constructorWithoutIntegerArgumentsIsNotAnIdConstructor() {
        Hierarchy h = hierarchy(LegacyVersion.V1_6_4, false);
        Run r = run(h, link(h, "world/Plain"), "(Ljava/lang/String;)V");
        assertNull(r.result());
        assertEquals(List.of(link(h, "world/Plain") + ".<init>(Ljava/lang/String;)V"), calls(r.method()), "instructions untouched");
    }

    @Test
    void unknownOwnerIsLeftAlone() {
        Hierarchy h = hierarchy(LegacyVersion.V1_6_4, false);
        assertNull(run(h, "mod/Unknown", "(I)V").result());
    }

    @Test
    void leadingIdWithoutAnObjectConstructorOfTheOwnerStaysUnadapted() {
        Hierarchy h = hierarchy(LegacyVersion.V1_6_4, false);
        // Plain is neither Block nor Item and has no (Item/Block, ...) constructor
        assertNull(run(h, link(h, "world/Plain"), "(ILjava/lang/String;)V").result());
    }

    @Test
    void idsAtSeveralPositionsBecomeObjectsThroughTheFacade() {
        Hierarchy h = hierarchy(LegacyVersion.V1_6_4, false);
        String owner = link(h, "world/gen/feature/WorldGenThing");
        Run r = run(h, owner, "(III)V");
        assertNotNull(r.result());
        String fBlock = h.version().shimPrefix() + "facade/net/minecraft/block/Block";
        String block = link(h, "block/Block");
        assertEquals(List.of(fBlock + ".mcc$fromId(I)L" + block + ";", fBlock + ".mcc$fromId(I)L" + block + ";",
                owner + ".<init>(L" + block + ";IL" + block + ";)V"), calls(r.method()));
        assertEquals("(L" + block + ";IL" + block + ";)V", r.call().desc);
        assertTrue(r.method().maxLocals >= 1 + 3 + 3, "three arguments spilled past the old locals");
    }

    @Test
    void idBeforeAStringWithNoMatchingObjectConstructorStaysUnadapted() {
        Hierarchy h = hierarchy(LegacyVersion.V1_6_4, false);
        String owner = link(h, "world/gen/feature/WorldGenThing");
        Run r = run(h, owner, "(ILjava/lang/String;)V");
        // (int, String): no (Item, String) or (Block, String) constructor, and (String, Item) does not take an int first
        assertNull(r.result());
    }

    @Test
    void itemSubclassWithOnlyAWideRestArgumentSpillsWithTheRightOpcodes() {
        Hierarchy h = hierarchy(LegacyVersion.V1_6_4, false);
        String owner = link(h, "item/ItemOdd");
        Run r = run(h, owner, "(ID)V");
        assertNotNull(r.result());
        assertEquals("(D)V", r.call().desc);
        // arguments spilled in reverse: DSTORE then ISTORE; the double occupies two slots so the next local is two past it
        List<VarInsnNode> stores = new ArrayList<>();
        for (AbstractInsnNode i = r.method().instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof VarInsnNode v && (v.getOpcode() == DSTORE || v.getOpcode() == ISTORE)) stores.add(v);
        }
        assertEquals(DSTORE, stores.get(0).getOpcode());
        assertEquals(ISTORE, stores.get(1).getOpcode());
        assertEquals(stores.get(1).var + 1, stores.get(0).var, "id slot, then the double");
        assertTrue(r.method().maxLocals >= stores.get(0).var + 2);
        // the claim of the original id at the end
        String fItem = h.version().shimPrefix() + "facade/net/minecraft/item/Item";
        assertTrue(calls(r.method()).contains(fItem + ".mcc$claimId(L" + link(h, "item/Item") + ";I)V"), calls(r.method()).toString());
    }

    @Test
    void closestConstructorFillsUnknownParametersWithDefaults() {
        Hierarchy h = hierarchy(LegacyVersion.V1_6_4, false);
        // ItemOdd has no (IF...) constructor: the id is dropped and (D) is the only one left, the float cannot fill it, so it defaults
        Run r = run(h, link(h, "item/ItemOdd"), "(IF)V");
        assertNotNull(r.result());
        assertEquals("(D)V", r.call().desc);
        boolean dconst = false;
        for (AbstractInsnNode i = r.method().instructions.getFirst(); i != null; i = i.getNext()) dconst |= i.getOpcode() == DCONST_0;
        assertTrue(dconst, "the default for a double parameter is 0.0");
    }

    @Test
    void blockFromThe147SheetClaimsItsTextureIndexBeforeItsId() {
        Hierarchy h = hierarchy(LegacyVersion.V1_4_7, true);
        String block = link(h, "block/Block");
        String mat = link(h, "block/material/Material");
        Run r = run(h, block, "(IIL" + mat + ";)V");
        assertNotNull(r.result());
        String f = h.version().shimPrefix() + "facade/net/minecraft/block/Block";
        assertEquals(List.of(block + ".<init>(L" + mat + ";)V", f + ".mcc$claimTexture(L" + block + ";I)V", f + ".mcc$claimId(L" + block + ";I)V"),
                calls(r.method()));
    }

    @Test
    void liquidBlockOf147ClaimsTheFluidTexture() {
        Hierarchy h = hierarchy(LegacyVersion.V1_4_7, true);
        String mat = link(h, "block/material/Material");
        Run r = run(h, link(h, "block/BlockLiquid"), "(IL" + mat + ";)V");
        assertNotNull(r.result());
        String f = h.version().shimPrefix() + "facade/net/minecraft/block/Block";
        String block = link(h, "block/Block");
        assertTrue(calls(r.method()).contains(f + ".mcc$claimFluidTexture(L" + block + ";L" + mat + ";)V"), calls(r.method()).toString());
    }

    @Test
    void itemOf147ClaimsTheDefaultIcon() {
        Hierarchy h = hierarchy(LegacyVersion.V1_4_7, true);
        Run r = run(h, link(h, "item/ItemOdd"), "(ID)V");
        assertNotNull(r.result());
        String f = h.version().shimPrefix() + "facade/net/minecraft/item/Item";
        assertTrue(calls(r.method()).contains(f + ".mcc$claimItemTexture(L" + link(h, "item/Item") + ";)V"), calls(r.method()).toString());
    }

    @Test
    void textureClaimsAreSkippedOnVersionsWithTheirOwnSheets() {
        Hierarchy h = hierarchy(LegacyVersion.V1_6_4, true);
        String mat = link(h, "block/material/Material");
        Run r = run(h, link(h, "block/Block"), "(IIL" + mat + ";)V");
        // 1.6.4 has no flat names: (int, int, Material) has no matching shim constructor, the closest one (Material) is chosen, still claimed by id only
        if (r.result() != null) {
            assertTrue(calls(r.method()).stream().noneMatch(c -> c.contains("claimTexture")), calls(r.method()).toString());
        }
    }
}
