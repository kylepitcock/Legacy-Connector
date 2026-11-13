package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression test for Palladium's removed AdvancementType#getTexture lookup. */
class PalladiumAdvancementFrame1201Test implements Opcodes {
    private static final String ADVANCEMENT_TYPE = "net/minecraft/advancements/AdvancementType";
    private static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/PalladiumFrameTypeCompat";
    private static final String HELPER_DESC =
            "(Lnet/minecraft/advancements/AdvancementType;)I";

    @Test
    void redirectsTheLegacyFrameTextureLookupToTheCompatibilityHelper() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = "net/threetag/palladium/client/screen/power/TreeAbilityWidget";
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode(
                ACC_PUBLIC | ACC_STATIC, "drawIcon",
                "(L" + ADVANCEMENT_TYPE + ";)I", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL, ADVANCEMENT_TYPE, "getTexture", "()I", false));
        method.instructions.add(new InsnNode(IRETURN));
        node.methods.add(method);

        Mc1201Hierarchy hierarchy = new Mc1201Hierarchy();
        hierarchy.add(node);
        Mc1201Migration.Counts changed = Mc1201Migration.apply(node, hierarchy);

        MethodInsnNode call = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode methodCall) call = methodCall;
        }
        assertNotNull(call);
        assertEquals(1, changed.calls);
        assertEquals(INVOKESTATIC, call.getOpcode());
        assertEquals(HELPER, call.owner);
        assertEquals("getTexture", call.name);
        assertEquals(HELPER_DESC, call.desc);
    }
}
