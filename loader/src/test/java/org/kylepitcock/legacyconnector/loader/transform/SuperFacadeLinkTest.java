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
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A {@code super.} call of an override of a legacy method the shim lacks (1.4.7's {@code getIconFromDamage(I)I}, 1.5.2's sprite {@code init}) goes to the facade's
 * {@code mcc$super$<name>} (the default body); a plain facade method of the same name is for virtual calls only, since it may dispatch back to the override.
 */
class SuperFacadeLinkTest implements Opcodes {
    private static final String P7 = "legacyconnector/v1_7_10/net/minecraft/";
    private static final String P6 = "legacyconnector/v1_6_4/facade/net/minecraft/";
    private static final String ITEM = P7 + "item/Item";

    private static ClassInfo cls(String name, String sup) {
        return new ClassInfo(name, sup, List.of(), ACC_PUBLIC);
    }

    private static Hierarchy hierarchy(boolean withSuper) {
        Map<String, ClassInfo> shim = new HashMap<>();
        ClassInfo item = cls(ITEM, "java/lang/Object");
        item.methods.put("<init>()V", ACC_PUBLIC);
        ClassInfo fItem = cls(P6 + "item/Item", "java/lang/Object");
        fItem.methods.put("getIconFromDamage(L" + ITEM + ";I)I", ACC_PUBLIC | ACC_STATIC);
        if (withSuper) fItem.methods.put("mcc$super$getIconFromDamage(L" + ITEM + ";I)I", ACC_PUBLIC | ACC_STATIC);
        for (ClassInfo c : List.of(cls("java/lang/Object", null), item, fItem)) shim.put(c.name, c);
        return new Hierarchy(new ShimIndex(shim), null, Map.of(), LegacyVersion.V1_5_2);
    }

    private static List<String> link(Hierarchy h, int opcode) {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = "mod/Sample";
        n.superName = ITEM;
        MethodNode m = new MethodNode(ACC_PUBLIC, "getIconFromDamage", "(I)I", null, null);
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ILOAD, 1);
        m.visitMethodInsn(opcode, ITEM, "getIconFromDamage", "(I)I", false);
        m.visitInsn(IRETURN);
        n.methods.add(m);
        h.addLocal(ClassInfo.of(n));
        ShimLinker.apply(n, h, new LinkReport(), new SynthDemand());
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode i = n.methods.get(0).instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode c) out.add(c.getOpcode() + " " + c.owner + "." + c.name + c.desc);
        }
        return out;
    }

    @Test
    void superCallGoesToTheSuperDefault() {
        assertEquals(List.of(INVOKESTATIC + " " + P6 + "item/Item.mcc$super$getIconFromDamage(L" + ITEM + ";I)I"), link(hierarchy(true), INVOKESPECIAL));
    }

    @Test
    void virtualCallStillUsesThePlainFacade() {
        assertEquals(List.of(INVOKESTATIC + " " + P6 + "item/Item.getIconFromDamage(L" + ITEM + ";I)I"), link(hierarchy(true), INVOKEVIRTUAL));
    }

    @Test
    void superCallWithoutASuperDefaultIsNotSentToTheDispatchingFacade() {
        List<String> calls = link(hierarchy(false), INVOKESPECIAL);
        for (String c : calls) assertEquals(false, c.contains(P6 + "item/Item.getIconFromDamage"), c);
    }
}
