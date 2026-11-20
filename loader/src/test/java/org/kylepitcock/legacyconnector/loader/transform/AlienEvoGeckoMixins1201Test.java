package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies AlienEvo's GeckoLib 4.8 handlers are adapted to GeckoLib 4.9 signatures. */
class AlienEvoGeckoMixins1201Test implements Opcodes {
    private static final String CI = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String POSE_STACK = "Lcom/mojang/blaze3d/vertex/PoseStack;";
    private static final String ENTITY = "Lnet/minecraft/world/entity/Entity;";
    private static final String BAKED_MODEL = "Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;";
    private static final String RENDER_TYPE = "Lnet/minecraft/client/renderer/RenderType;";
    private static final String BUFFER_SOURCE = "Lnet/minecraft/client/renderer/MultiBufferSource;";
    private static final String VERTEX_CONSUMER = "Lcom/mojang/blaze3d/vertex/VertexConsumer;";
    private static final String OLD_RENDER_ARGS =
            "(" + POSE_STACK + ENTITY + BAKED_MODEL + RENDER_TYPE + BUFFER_SOURCE
                    + VERTEX_CONSUMER + "ZFIIFFFF" + CI + ")V";
    private static final String NEW_RENDER_ARGS =
            "(" + POSE_STACK + ENTITY + BAKED_MODEL + RENDER_TYPE + BUFFER_SOURCE
                    + VERTEX_CONSUMER + "ZFIII" + CI + ")V";

    private static ClassNode node(String name) {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC | ACC_ABSTRACT;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String desc, int maxLocals, int... loads) {
        MethodNode method = new MethodNode(ACC_PRIVATE, name, desc, null, null);
        for (int i = 0; i < loads.length; i += 2) {
            method.instructions.add(new VarInsnNode(loads[i], loads[i + 1]));
        }
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = maxLocals;
        method.maxStack = 2;
        node.methods.add(method);
        return method;
    }

    private static List<Integer> localSlots(MethodNode method) {
        List<Integer> slots = new ArrayList<>();
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable) slots.add(variable.var);
        }
        return slots;
    }

    @Test
    void stillClockInsertsPartialTickBeforeCallbackAndMovesExistingLocals() {
        ClassNode node = node(AlienEvoGeckoMixins1201.STILL_CLOCK);
        String oldDesc = "(Lsoftware/bernie/geckolib/animatable/GeoAnimatable;JL"
                + "software/bernie/geckolib/animation/AnimationState;" + CI + ")V";
        String newDesc = "(Lsoftware/bernie/geckolib/animatable/GeoAnimatable;JL"
                + "software/bernie/geckolib/animation/AnimationState;F" + CI + ")V";
        MethodNode handler = method(node, "alienevo$poseStill", oldDesc, 8, ALOAD, 5, ALOAD, 6, ALOAD, 7);

        assertEquals(1, AlienEvoGeckoMixins1201.apply(node));
        assertEquals(newDesc, handler.desc);
        assertEquals(List.of(6, 7, 8), localSlots(handler));
        assertEquals(9, handler.maxLocals);
        assertNull(handler.localVariables);
        assertEquals(0, AlienEvoGeckoMixins1201.apply(node), "the descriptor rewrite must be idempotent");
    }

    @Test
    void geoEntityRendererPacksColorAndMovesCallbackAndRendererTemporary() {
        ClassNode node = node(AlienEvoGeckoMixins1201.GEO_ENTITY_RENDERER);
        MethodNode handler = method(node, "alienevo$ensureAlienEvoLayers", OLD_RENDER_ARGS, 17, ILOAD, 7, ALOAD, 15, ALOAD, 16);

        assertEquals(1, AlienEvoGeckoMixins1201.apply(node));
        assertEquals(NEW_RENDER_ARGS, handler.desc);
        assertEquals(List.of(7, 12, 13), localSlots(handler));
        assertEquals(14, handler.maxLocals);
        assertNull(handler.localVariables);
    }

    @Test
    void rideSeatBoneMigratesAllThreeCallbacksTogether() {
        ClassNode node = node(AlienEvoGeckoMixins1201.RIDE_SEAT_BONE);
        String[] names = {
                "alienevo$beginSeatMeasure",
                "alienevo$endSeatMeasure",
                "alienevo$measureSeatBone"
        };
        for (String name : names) method(node, name, OLD_RENDER_ARGS, 16, ILOAD, 7, ALOAD, 15);

        assertEquals(1, AlienEvoGeckoMixins1201.apply(node));
        for (String name : names) {
            MethodNode handler = node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElse(null);
            assertNotNull(handler);
            assertEquals(NEW_RENDER_ARGS, handler.desc);
            assertEquals(List.of(7, 12), localSlots(handler));
            assertEquals(13, handler.maxLocals);
            assertNull(handler.localVariables);
        }
        assertEquals(0, AlienEvoGeckoMixins1201.apply(node), "the descriptor rewrite must be idempotent");
    }
}
