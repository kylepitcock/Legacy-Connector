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
 * A call of a shim method the shim lacks with a reference argument type that is a subtype of the type the shim declares
 * (1.5.2 {@code BlockPistonBase.determineOrientation(..., EntityLiving)}, 1.7.10 takes {@code EntityLivingBase}) links to the shim's method
 * instead of a stub, so the passed value still verifies.
 */
class SubtypeArgLinkTest implements Opcodes {
    private static final String P7 = "legacyconnector/v1_7_10/net/minecraft/";
    private static final String BASE = P7 + "entity/EntityLivingBase";
    private static final String LIVING = P7 + "entity/EntityLiving";
    private static final String PISTON = P7 + "block/BlockPistonBase";

    private static ClassInfo cls(String name, String sup) {
        return new ClassInfo(name, sup, List.of(), ACC_PUBLIC);
    }

    @Test
    void subtypeArgumentLinksToTheShimsWiderParameter() {
        Map<String, ClassInfo> shim = new HashMap<>();
        ClassInfo piston = cls(PISTON, "java/lang/Object");
        piston.methods.put("determineOrientation(IL" + BASE + ";)I", ACC_PUBLIC | ACC_STATIC);
        for (ClassInfo c : List.of(cls("java/lang/Object", null), cls(BASE, "java/lang/Object"), cls(LIVING, BASE), piston)) shim.put(c.name, c);
        Hierarchy h = new Hierarchy(new ShimIndex(shim), null, Map.of(), LegacyVersion.V1_6_4);

        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = "mod/Sample";
        n.superName = "java/lang/Object";
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "orient", "(IL" + LIVING + ";)I", null, null);
        n.methods.add(m);
        m.visitVarInsn(ILOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitMethodInsn(INVOKESTATIC, PISTON, "determineOrientation", "(IL" + LIVING + ";)I", false);
        m.visitInsn(IRETURN);
        h.addLocal(ClassInfo.of(n));
        ShimLinker.apply(n, h, new LinkReport(), new SynthDemand());

        List<String> calls = new ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode c) calls.add(c.owner + "." + c.name + c.desc);
        }
        assertEquals(List.of(PISTON + ".determineOrientation(IL" + BASE + ";)I"), calls);
    }
}
