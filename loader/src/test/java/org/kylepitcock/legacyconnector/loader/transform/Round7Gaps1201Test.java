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
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** ATM 9 gap round 7: the call rows of this batch redirect to their helpers (the helpers themselves are checked by the runtime compile). */
class Round7Gaps1201Test implements Opcodes {
    private static final String MIGRATE = "org/kylepitcock/legacyconnector/v1_20_1/migrate/";

    private static MethodNode run(MethodInsnNode call) {
        ClassNode c = new ClassNode();
        c.version = V17;
        c.access = ACC_PUBLIC;
        c.name = "test/Mod";
        c.superName = "java/lang/Object";
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "go", "()V", null, null);
        c.methods.add(m);
        m.instructions.add(call);
        m.instructions.add(new InsnNode(RETURN));
        Mc1201Hierarchy h = new Mc1201Hierarchy();
        h.add(c);
        Mc1201Migration.apply(c, h);
        return m;
    }

    private static MethodInsnNode first(MethodNode m) {
        for (AbstractInsnNode in : m.instructions) if (in instanceof MethodInsnNode c) return c;
        return null;
    }

    @Test
    void displayInfoFrameAndShieldColorGoToHelpers() {
        MethodInsnNode call = first(run(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/advancements/DisplayInfo", "getFrame", "()Lnet/minecraft/advancements/AdvancementType;", false)));
        assertEquals(MIGRATE + "Round7Compat", call.owner);
        assertEquals("displayFrame", call.name);
        call = first(run(new MethodInsnNode(INVOKESTATIC, "net/minecraft/world/item/ShieldItem", "getColor", "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/DyeColor;", false)));
        assertEquals("shieldColor", call.name);
    }

    @Test
    void bakedModelAmbientOcclusionBecomesTheClientHelper() {
        MethodInsnNode call = first(run(new MethodInsnNode(INVOKEINTERFACE, "net/minecraft/client/resources/model/BakedModel", "useAmbientOcclusion",
                "(Lnet/minecraft/world/level/block/state/BlockState;)Z", true)));
        assertNotNull(call);
        assertEquals(MIGRATE + "Round7ClientCompat", call.owner);
        assertEquals("(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;)Z", call.desc);
    }

    @Test
    void sheetedDecalConstructorBecomesFactory() {
        ClassNode c = new ClassNode();
        c.version = V17;
        c.access = ACC_PUBLIC;
        c.name = "test/Mod";
        c.superName = "java/lang/Object";
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "go", "()V", null, null);
        c.methods.add(m);
        String owner = "com/mojang/blaze3d/vertex/SheetedDecalTextureGenerator";
        m.instructions.add(new TypeInsnNode(NEW, owner));
        m.instructions.add(new InsnNode(DUP));
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new VarInsnNode(ALOAD, 1));
        m.instructions.add(new VarInsnNode(ALOAD, 2));
        m.instructions.add(new InsnNode(FCONST_1));
        m.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner, "<init>",
                "(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;Lorg/joml/Matrix3f;F)V", false));
        m.instructions.add(new InsnNode(POP));
        m.instructions.add(new InsnNode(RETURN));
        Mc1201Hierarchy h = new Mc1201Hierarchy();
        h.add(c);
        Mc1201Migration.apply(c, h);
        MethodInsnNode call = first(m);
        assertEquals(INVOKESTATIC, call.getOpcode());
        assertEquals("newSheetedDecal", call.name);
        for (AbstractInsnNode in : m.instructions) org.junit.jupiter.api.Assertions.assertFalse(in instanceof TypeInsnNode, "NEW is gone");
    }
}
