package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Descriptor and local-slot migrations for the verified AlienEvo 1.20.1 render handlers. */
class AlienEvoMixins1201Test implements Opcodes {
    private static ClassNode node(String name) {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String desc) {
        MethodNode method = new MethodNode(ACC_PRIVATE, name, desc, null, null);
        node.methods.add(method);
        return method;
    }

    private static MethodInsnNode call(MethodNode method, String owner, String name) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) return call;
        }
        return null;
    }

    private static void verify(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertNotNull(writer.toByteArray());
    }

    @Test
    void alienHudHotbarHandlerMovesGraphicsAndReceivesDeltaTracker() {
        ClassNode node = node("net/habb/alienevo/mixin/client/AlienHudMixin");
        MethodNode handler = method(node, "alienevo$renderCreativeElements",
                "(FLnet/minecraft/client/gui/GuiGraphics;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        handler.instructions.add(new VarInsnNode(ALOAD, 2));
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", handler.desc);
        assertEquals(1, ((VarInsnNode) handler.instructions.getFirst()).var);
        assertEquals(0, AlienEvoMixins1201.apply(node));
        verify(node);
    }

    @Test
    void kineceleranVignetteHandlerMovesGraphicsAndPreservesTempLocals() {
        ClassNode node = node("net/habb/alienevo/mixin/client/KineceleranVignetteMixin");
        MethodNode handler = method(node, "alienevo$drawVignetteBeforeHotbar",
                "(FLnet/minecraft/client/gui/GuiGraphics;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        handler.instructions.add(new VarInsnNode(ALOAD, 2));
        handler.instructions.add(new VarInsnNode(ASTORE, 4));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", handler.desc);
        assertEquals(1, ((VarInsnNode) handler.instructions.getFirst()).var);
        assertEquals(4, ((VarInsnNode) handler.instructions.get(1)).var);
        verify(node);
    }

    @Test
    void previewNameTagHandlerAddsPartialTickAndMovesCallbackInfo() {
        ClassNode node = node("net/habb/alienevo/mixin/client/PreviewNameTagMixin");
        MethodNode handler = method(node, "alienevo$suppressPreviewNameTag",
                "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        handler.instructions.add(new VarInsnNode(ALOAD, 6));
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", handler.desc);
        assertEquals(7, ((VarInsnNode) handler.instructions.getFirst()).var);
        verify(node);
    }

    @Test
    void gameRendererUsesDeltaTrackerPartialTick() {
        ClassNode node = node("net/habb/alienevo/mixin/client/GameRendererMixin");
        MethodNode handler = method(node, "alienevo$depthOfFieldBeforeHand",
                "(FJLcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        handler.instructions.add(new VarInsnNode(FLOAD, 1));
        handler.instructions.add(new MethodInsnNode(INVOKESTATIC, "net/habb/alienevo/client/DepthOfFieldShaderHandler",
                "renderBeforeHand", "(F)V", false));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(Lnet/minecraft/client/DeltaTracker;"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", handler.desc);
        assertNull(findVar(handler, FLOAD, 1));
        assertNotNull(findVar(handler, ALOAD, 1));
        assertEquals(ICONST_0, handler.instructions.get(1).getOpcode());
        MethodInsnNode partialTick = call(handler, "net/minecraft/client/DeltaTracker", "getGameTimeDeltaPartialTick");
        assertNotNull(partialTick);
        assertEquals("(Z)F", partialTick.desc);
        assertEquals(INVOKEINTERFACE, partialTick.getOpcode());
        verify(node);
    }

    @Test
    void kartSteerHandlerUsesNewLevelRendererArguments() {
        ClassNode node = node("net/habb/alienevo/mixin/client/KartSteerMixin");
        MethodNode handler = method(node, "alienevo$kartSteerFrame",
                "(FJZLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(Lnet/minecraft/client/DeltaTracker;Z"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", handler.desc);
        verify(node);
    }

    @Test
    void electrocutionOutlineGetsDeltaTrackerAndTwoProjectionMatrices() {
        ClassNode node = node("net/habb/alienevo/mixin/client/ElectrocutionOutlineChainMixin");
        MethodNode handler = method(node, "alienevo$lateOutline",
                "(Lcom/mojang/blaze3d/vertex/PoseStack;FJZ"
                + "Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;"
                + "Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        handler.instructions.add(new VarInsnNode(FLOAD, 2));
        handler.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/client/renderer/PostChain",
                "process", "(F)V", false));
        handler.instructions.add(new VarInsnNode(ALOAD, 11));
        handler.instructions.add(new VarInsnNode(ASTORE, 12));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(Lnet/minecraft/client/DeltaTracker;Z"
                + "Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;"
                + "Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", handler.desc);
        assertNotNull(call(handler, "net/minecraft/client/DeltaTracker", "getGameTimeDeltaPartialTick"));
        assertEquals(9, ((VarInsnNode) handler.instructions.get(4)).var);
        assertEquals(10, ((VarInsnNode) handler.instructions.get(5)).var);
        verify(node);
    }

    @Test
    void shieldDisableHandlerDropsRemovedBooleanParameter() {
        ClassNode node = node("net/habb/alienevo/mixin/ShieldSoundMixin");
        MethodNode handler = method(node, "alienevo$preventShieldDisable",
                "(ZLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        handler.instructions.add(new VarInsnNode(ALOAD, 3));
        handler.instructions.add(new VarInsnNode(ASTORE, 3));
        handler.instructions.add(new VarInsnNode(ALOAD, 2));
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", handler.desc);
        assertEquals(2, ((VarInsnNode) handler.instructions.getFirst()).var);
        assertEquals(2, ((VarInsnNode) handler.instructions.get(1)).var);
        assertEquals(1, ((VarInsnNode) handler.instructions.get(2)).var);
        verify(node);
    }

    @Test
    void cameraZoomScaleUsesFloatDistanceInOneTwentyOne() {
        ClassNode node = node("net/habb/alienevo/mixin/scale/client/CameraZoomScaleMixin");
        MethodNode handler = method(node, "alienevo$scaleCameraDistance", "(D)D");
        handler.instructions.add(new VarInsnNode(DLOAD, 1));
        handler.instructions.add(new VarInsnNode(FLOAD, 3));
        handler.instructions.add(new InsnNode(F2D));
        handler.instructions.add(new InsnNode(DMUL));
        handler.instructions.add(new InsnNode(DRETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(F)F", handler.desc);
        assertEquals(FLOAD, ((VarInsnNode) handler.instructions.get(0)).getOpcode());
        assertEquals(FLOAD, ((VarInsnNode) handler.instructions.get(1)).getOpcode());
        assertEquals(FMUL, handler.instructions.get(2).getOpcode());
        assertEquals(FRETURN, handler.instructions.get(3).getOpcode());
        verify(node);
    }

    @Test
    void kartCameraZoomScaleUsesFloatDistanceInOneTwentyOne() {
        ClassNode node = node("net/habb/alienevo/mixin/client/KartCameraMixin");
        MethodNode handler = method(node, "alienevo$kartSpeedZoom", "(D)D");
        handler.instructions.add(new VarInsnNode(DLOAD, 1));
        handler.instructions.add(new InsnNode(DRETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(F)F", handler.desc);
        assertEquals(FLOAD, ((VarInsnNode) handler.instructions.get(0)).getOpcode());
        assertEquals(FRETURN, handler.instructions.get(1).getOpcode());
        verify(node);
    }

    @Test
    void cameraPositionHandlerConvertsDoubleZoomAndCallbackSlotsToFloat() {
        ClassNode node = node("net/habb/alienevo/mixin/client/CameraPositionMixin");
        MethodNode handler = method(node, "alienevo$keepZoomWhenPivotBuried",
                "(DLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V");
        handler.instructions.add(new VarInsnNode(ALOAD, 3));
        handler.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(CHECKCAST, "java/lang/Double"));
        handler.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
        handler.instructions.add(new VarInsnNode(DSTORE, 4));
        handler.instructions.add(new VarInsnNode(ALOAD, 6));
        handler.instructions.add(new VarInsnNode(DLOAD, 4));
        handler.instructions.add(new VarInsnNode(DLOAD, 1));
        handler.instructions.add(new InsnNode(DCMPG));
        handler.instructions.add(new VarInsnNode(ALOAD, 3));
        handler.instructions.add(new VarInsnNode(DLOAD, 1));
        handler.instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf",
                "(D)Ljava/lang/Double;", false));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V", handler.desc);
        assertEquals(2, ((VarInsnNode) handler.instructions.get(0)).var);
        assertEquals("java/lang/Float", ((org.objectweb.asm.tree.TypeInsnNode) handler.instructions.get(1)).desc);
        assertEquals("java/lang/Float", ((MethodInsnNode) handler.instructions.get(2)).owner);
        assertEquals("()F", ((MethodInsnNode) handler.instructions.get(2)).desc);
        assertEquals(3, ((VarInsnNode) handler.instructions.get(3)).var);
        assertEquals(4, ((VarInsnNode) handler.instructions.get(4)).var);
        assertEquals(FCMPG, handler.instructions.get(7).getOpcode());
        assertEquals(2, ((VarInsnNode) handler.instructions.get(8)).var);
        assertEquals("java/lang/Float", ((MethodInsnNode) handler.instructions.get(10)).owner);
        assertEquals("(F)Ljava/lang/Float;", ((MethodInsnNode) handler.instructions.get(10)).desc);
        verify(node);
    }

    @Test
    void modelBakeryCrystalHandlerAdaptsTextureGetterToLegacySpriteFunction() {
        ClassNode node = node("net/habb/alienevo/mixin/client/ModelBakeryCrystalMixin");
        node.fields.add(new org.objectweb.asm.tree.FieldNode(ACC_PRIVATE, "f_119215_", "Ljava/util/Map;", null, null));
        MethodNode handler = method(node, "alienevo$bakeStaticCrystal",
                "(Ljava/util/function/BiFunction;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        handler.instructions.add(new VarInsnNode(ALOAD, 0));
        handler.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(GETFIELD, node.name, "f_119215_", "Ljava/util/Map;"));
        handler.instructions.add(new VarInsnNode(ALOAD, 1));
        handler.instructions.add(new MethodInsnNode(INVOKESTATIC,
                "net/habb/alienevo/client/renderer/DiamondBlockStaticModel", "install",
                "(Ljava/util/Map;Ljava/util/function/BiFunction;)V", false));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals("(Lnet/minecraft/client/resources/model/ModelBakery$TextureGetter;"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", handler.desc);
        assertNotNull(call(handler, "org/kylepitcock/legacyconnector/v1_20_1/migrate/ModelBakeryCompat", "asBiFunction"));
        assertNotNull(call(handler, "net/habb/alienevo/client/renderer/DiamondBlockStaticModel", "install"));
        verify(node);
    }

    private static VarInsnNode findVar(MethodNode method, int opcode, int slot) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable && variable.getOpcode() == opcode
                    && (slot < 0 || variable.var == slot)) return variable;
        }
        return null;
    }
}
