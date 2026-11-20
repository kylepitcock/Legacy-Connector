package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies the component and instance-method migrations used by Palladium crossbows. */
class PalladiumCrossbowApi1201Test implements Opcodes {
    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String PLAYER = "net/minecraft/world/entity/player/Player";
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String HAND = "net/minecraft/world/InteractionHand";
    private static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";
    private static final String CROSSBOW = "net/minecraft/world/item/CrossbowItem";

    private static ClassNode addonCrossbow() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = PalladiumCrossbowApi1201.ADDON_CROSSBOW;
        node.superName = CROSSBOW;

        MethodNode use = new MethodNode(ACC_PUBLIC, "use",
                "(L" + LEVEL + ";L" + PLAYER + ";L" + HAND + ";)"
                        + "Lnet/minecraft/world/InteractionResultHolder;", null, null);
        node.methods.add(use);
        use.maxLocals = 7;
        use.maxStack = 6;

        use.instructions.add(new VarInsnNode(ALOAD, 4));
        use.instructions.add(new MethodInsnNode(INVOKESTATIC, CROSSBOW, "getShootingPower",
                "(L" + ITEM_STACK + ";)F", false));
        use.instructions.add(new VarInsnNode(ALOAD, 0));
        use.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(GETFIELD, node.name, "velocityMultiplier", "F"));
        use.instructions.add(new InsnNode(FMUL));
        use.instructions.add(new VarInsnNode(ALOAD, 0));
        use.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(GETFIELD, node.name, "inaccuracy", "F"));

        use.instructions.add(new VarInsnNode(ALOAD, 1));
        use.instructions.add(new VarInsnNode(ALOAD, 2));
        use.instructions.add(new VarInsnNode(ALOAD, 3));
        use.instructions.add(new VarInsnNode(ALOAD, 4));
        use.instructions.add(new VarInsnNode(FLOAD, 5));
        use.instructions.add(new VarInsnNode(FLOAD, 6));
        use.instructions.add(new MethodInsnNode(INVOKESTATIC, CROSSBOW, "performShooting",
                "(L" + LEVEL + ";L" + LIVING_ENTITY + ";L" + HAND + ";L" + ITEM_STACK + ";FF)V", false));

        use.instructions.add(new VarInsnNode(ALOAD, 4));
        use.instructions.add(new InsnNode(ICONST_0));
        use.instructions.add(new MethodInsnNode(INVOKESTATIC, CROSSBOW, "setCharged",
                "(L" + ITEM_STACK + ";Z)V", false));
        use.instructions.add(new InsnNode(RETURN));
        return node;
    }

    @Test
    void addonCrossbowUsesChargedProjectilesComponentAndInstanceShootingApi() {
        ClassNode node = addonCrossbow();

        assertEquals(3, PalladiumCrossbowApi1201.apply(node));
        MethodNode use = find(node, "use");
        assertTrue(hasCall(use, INVOKESTATIC, PalladiumCrossbowApi1201.HELPER, "getShootingPower",
                "(L" + ITEM_STACK + ";)F"));
        assertTrue(hasCall(use, INVOKESTATIC, PalladiumCrossbowApi1201.HELPER, "performShooting",
                "(L" + CROSSBOW + ";L" + LEVEL + ";L" + LIVING_ENTITY + ";L" + HAND
                        + ";L" + ITEM_STACK + ";FF)V"));
        assertFalse(hasCall(use, INVOKESTATIC, CROSSBOW, "getShootingPower", "(L" + ITEM_STACK + ";)F"));
        assertFalse(hasCall(use, INVOKESTATIC, CROSSBOW, "performShooting",
                "(L" + LEVEL + ";L" + LIVING_ENTITY + ";L" + HAND + ";L" + ITEM_STACK + ";FF)V"));
        assertFalse(hasCall(use, INVOKESTATIC, CROSSBOW, "setCharged", "(L" + ITEM_STACK + ";Z)V"),
                "performShooting now clears the CHARGED_PROJECTILES component itself");
        assertTrue(hasOpcode(use, FSTORE));
        assertTrue(hasOpcode(use, ASTORE));
        assertTrue(use.maxLocals >= 13);
        assertTrue(use.maxStack >= 7);

        assertEquals(0, PalladiumCrossbowApi1201.apply(node), "the migration must be idempotent");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }

    @Test
    void clientCrossbowChargePredicatePassesTheLivingEntityToTheNewApi() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = PalladiumCrossbowApi1201.PALLADIUM_CLIENT;
        node.superName = "java/lang/Object";
        MethodNode predicate = new MethodNode(ACC_PRIVATE | ACC_STATIC, "lambda$itemModelPredicates$15",
                "(L" + ITEM_STACK + ";Lnet/minecraft/client/multiplayer/ClientLevel;"
                        + "L" + LIVING_ENTITY + ";I)F", null, null);
        node.methods.add(predicate);
        predicate.maxLocals = 4;
        predicate.maxStack = 1;
        predicate.instructions.add(new VarInsnNode(ALOAD, 0));
        predicate.instructions.add(new MethodInsnNode(INVOKESTATIC, CROSSBOW, "getChargeDuration",
                "(L" + ITEM_STACK + ";)I", false));
        predicate.instructions.add(new InsnNode(I2F));
        predicate.instructions.add(new InsnNode(FRETURN));

        assertEquals(1, PalladiumCrossbowApi1201.apply(node));
        assertTrue(hasVar(predicate, ALOAD, 2));
        assertTrue(hasCall(predicate, INVOKESTATIC, CROSSBOW, "getChargeDuration",
                "(L" + ITEM_STACK + ";L" + LIVING_ENTITY + ";)I"));
        assertFalse(hasCall(predicate, INVOKESTATIC, CROSSBOW, "getChargeDuration",
                "(L" + ITEM_STACK + ";)I"));
        assertEquals(0, PalladiumCrossbowApi1201.apply(node));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }

    private static MethodNode find(ClassNode node, String name) {
        for (MethodNode method : node.methods) if (method.name.equals(name)) return method;
        return null;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) return true;
        }
        return false;
    }

    private static boolean hasVar(MethodNode method, int opcode, int local) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable && variable.getOpcode() == opcode && variable.var == local) return true;
        }
        return false;
    }

    private static boolean hasOpcode(MethodNode method, int opcode) {
        for (var instruction : method.instructions.toArray()) if (instruction.getOpcode() == opcode) return true;
        return false;
    }
}
