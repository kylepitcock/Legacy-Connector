package org.kylepitcock.legacyconnector.loader.transform;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;

class PalladiumMixinCompat1201Test implements Opcodes {

    private static final String CI = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String CIR = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";
    private static final String POSE = "com/mojang/blaze3d/vertex/PoseStack";
    private static final String MATRIX = "org/joml/Matrix4f";
    private static final String QUAD = "software/bernie/geckolib/cache/object/GeoQuad";
    private static final String VERTEX = "com/mojang/blaze3d/vertex/VertexConsumer";
    private static final String MODEL = "net/minecraft/client/model/HumanoidModel";
    private static final String SLOT = "net/minecraft/world/scores/DisplaySlot";
    private static final String OBJECTIVE = "net/minecraft/world/scores/Objective";

    @Test
    void armorItemMixinUsesPlayerReceiverForLivingEntityEquipmentSlotLookup() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/ArmorItemMixin";
        String handlerDesc = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;"
                + "Lnet/minecraft/world/InteractionHand;" + CIR + ")V";
        String itemStack = "Lnet/minecraft/world/item/ItemStack;";
        String equipmentSlot = "Lnet/minecraft/world/entity/EquipmentSlot;";
        MethodNode handler = new MethodNode(ACC_PUBLIC, "use", handlerDesc, null, null);
        handler.instructions.add(new VarInsnNode(ALOAD, 5));
        handler.instructions.add(new MethodInsnNode(INVOKESTATIC, "net/minecraft/world/entity/Mob",
                "getEquipmentSlotForItem", "(" + itemStack + ")" + equipmentSlot, false));
        handler.instructions.add(new VarInsnNode(ASTORE, 6));
        handler.instructions.add(new InsnNode(RETURN));
        handler.maxLocals = 7;
        handler.maxStack = 1;
        node.methods.add(handler);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        MethodInsnNode lookup = findCall(handler, "getEquipmentSlotForItem");
        assertNotNull(lookup);
        assertEquals(INVOKEVIRTUAL, lookup.getOpcode());
        assertEquals("net/minecraft/world/entity/LivingEntity", lookup.owner);
        assertFalse(lookup.itf);

        AbstractInsnNode before = previousReal(lookup);
        assertTrue(before instanceof InsnNode && before.getOpcode() == SWAP);
        before = previousReal(before);
        assertTrue(before instanceof VarInsnNode receiver && receiver.getOpcode() == ALOAD && receiver.var == 2);

        assertEquals(0, PalladiumMixinCompat1201.apply(node),
                "an already-adapted instance call must not receive a second receiver");
        assertClassCanBeSerialized(node);
    }


    @Test
    void itemTooltipHandlerExtractsLevelFromTooltipContext() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/ItemMixin";
        String oldDesc = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;"
                + "Lnet/minecraft/world/item/TooltipFlag;" + CI + ")V";
        String newDesc = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;"
                + "Lnet/minecraft/world/item/TooltipFlag;" + CI + ")V";
        MethodNode handler = new MethodNode(ACC_PUBLIC, "appendHoverText", oldDesc, null, null);
        handler.instructions.add(new VarInsnNode(ALOAD, 2));
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new InsnNode(RETURN));
        handler.maxLocals = 6;
        node.methods.add(handler);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertEquals(newDesc, handler.desc);
        assertTrue(hasCall(handler, INVOKEINTERFACE, "net/minecraft/world/item/Item$TooltipContext",
                "level", "()Lnet/minecraft/world/level/Level;"));
        assertTrue(hasVar(handler, ALOAD, 6));
    }

    @Test
    void armorStandBrokenByPlayerHandlerReceivesServerLevelAndShiftsLocals() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/ArmorStandMixin";
        String oldDesc = "(Lnet/minecraft/world/damagesource/DamageSource;" + CI + ")V";
        String newDesc = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;" + CI + ")V";
        MethodNode handler = new MethodNode(ACC_PRIVATE, "brokenByPlayer", oldDesc, null, null);
        handler.instructions.add(new VarInsnNode(ALOAD, 1));
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new VarInsnNode(ALOAD, 2));
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new InsnNode(RETURN));
        handler.maxLocals = 3;
        node.methods.add(handler);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertEquals(newDesc, handler.desc);
        assertTrue(hasVar(handler, ALOAD, 2));
        assertTrue(hasVar(handler, ALOAD, 3));
        assertEquals(4, handler.maxLocals);
    }

    @Test
    void scoreboardHandlersUseDisplaySlotEnumAndEnumPacketConstructor() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/ServerScoreboardMixin";
        String oldSet = "(IL" + OBJECTIVE + ";" + CI + ")V";
        String newSet = "(L" + SLOT + ";L" + OBJECTIVE + ";" + CI + ")V";
        MethodNode setDisplay = new MethodNode(ACC_PUBLIC, "setDisplayObjective", oldSet, null, null);
        setDisplay.instructions.add(new VarInsnNode(ILOAD, 1));
        setDisplay.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "net/minecraft/world/scores/Scoreboard", "getDisplayObjective",
                "(I)L" + OBJECTIVE + ";", false));
        setDisplay.instructions.add(new InsnNode(POP));
        setDisplay.instructions.add(new InsnNode(RETURN));
        node.methods.add(setDisplay);

        String stopDesc = "(L" + OBJECTIVE + ";" + CIR + ")V";
        MethodNode stop = new MethodNode(ACC_PUBLIC, "getStopTrackingPackets", stopDesc, null, null);
        stop.instructions.add(new InsnNode(RETURN));
        node.methods.add(stop);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertEquals(newSet, setDisplay.desc);
        assertTrue(hasVar(setDisplay, ALOAD, 1));
        assertTrue(hasCall(setDisplay, INVOKEVIRTUAL, "net/minecraft/world/scores/Scoreboard",
                "getDisplayObjective", "(L" + SLOT + ";)L" + OBJECTIVE + ";"));
        assertTrue(hasCall(stop, INVOKESTATIC, SLOT, "values", "()[L" + SLOT + ";"));
        assertTrue(hasCall(stop, INVOKESPECIAL, "net/minecraft/network/protocol/game/ClientboundSetDisplayObjectivePacket",
                "<init>", "(L" + SLOT + ";L" + OBJECTIVE + ";)V"));
    }

    @Test
    void playerRendererInvokerAddsTheFourthRotationFloat() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/client/PlayerRendererInvoker";
        String oldDesc = "(Lnet/minecraft/client/player/AbstractClientPlayer;L" + POSE + ";FFF)V";
        String newDesc = "(Lnet/minecraft/client/player/AbstractClientPlayer;L" + POSE + ";FFFF)V";
        node.methods.add(new MethodNode(ACC_PUBLIC, "invokeSetupRotations", oldDesc, null, null));

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertTrue(find(node, "invokeSetupRotations", newDesc) != null);
    }

    @Test
    void humanoidArmorPieceHandlerTargetsNeoForgeAnimationOverload() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/client/HumanoidArmorLayerMixin";
        String oldDesc = "(L" + POSE + ";Lnet/minecraft/client/renderer/MultiBufferSource;"
                + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;I"
                + "L" + MODEL + ";" + CI + ")V";
        String newDesc = "(L" + POSE + ";Lnet/minecraft/client/renderer/MultiBufferSource;"
                + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;I"
                + "L" + MODEL + ";FFFFFF" + CI + ")V";
        MethodNode handler = new MethodNode(ACC_PRIVATE, "renderArmorPiece", oldDesc, null, null);
        handler.parameters = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) handler.parameters.add(new ParameterNode("p" + i, 0));
        handler.visibleAnnotations = new java.util.ArrayList<>();
        AnnotationNode inject = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/Inject;");
        inject.values = new java.util.ArrayList<>();
        inject.values.add("method");
        inject.values.add(new java.util.ArrayList<>(List.of("renderArmorPiece")));
        handler.visibleAnnotations.add(inject);
        handler.instructions.add(new VarInsnNode(ALOAD, 7)); // CallbackInfo
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new VarInsnNode(ALOAD, 8)); // first temporary
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new InsnNode(RETURN));
        handler.maxLocals = 9;
        handler.maxStack = 1;
        node.methods.add(handler);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertEquals(newDesc, handler.desc);
        String targetDesc = "(L" + POSE + ";Lnet/minecraft/client/renderer/MultiBufferSource;"
                + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;I"
                + "L" + MODEL + ";FFFFFF)V";
        assertEquals(List.of("renderArmorPiece" + targetDesc), annotationValue(inject, "method"));
        assertEquals(Boolean.FALSE, annotationValue(inject, "remap"),
                "the stale 1.20.1 refmap must not redirect the exact 1.21.1 selector");
        assertTrue(hasVar(handler, ALOAD, 13), "CallbackInfo must move behind the six new float arguments");
        assertTrue(hasVar(handler, ALOAD, 14), "existing locals must move behind the inserted arguments");
        assertFalse(hasVar(handler, ALOAD, 7));
        assertFalse(hasVar(handler, ALOAD, 8));
        assertEquals(15, handler.maxLocals);
        assertNull(handler.parameters, "stale MethodParameters metadata must not describe the old argument list");
        assertNotNull(handler.visibleAnnotations);
        assertEquals("Lorg/spongepowered/asm/mixin/injection/Inject;", handler.visibleAnnotations.get(0).desc);
        assertClassCanBeSerialized(node);
        assertEquals(0, PalladiumMixinCompat1201.apply(node), "the descriptor adaptation must be idempotent");
    }

    @Test
    void humanoidArmorMixinUsesTheCurrentRenderModelSignature() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/client/HumanoidArmorLayerMixin";
        String oldDesc = "(L" + POSE + ";Lnet/minecraft/client/renderer/MultiBufferSource;I"
                + "Lnet/minecraft/world/item/ArmorItem;L" + MODEL + ";ZFFF"
                + "Ljava/lang/String;" + CI + ")V";
        String newDesc = "(L" + POSE + ";Lnet/minecraft/client/renderer/MultiBufferSource;I"
                + "L" + MODEL + ";ILnet/minecraft/resources/ResourceLocation;" + CI + ")V";
        MethodNode handler = new MethodNode(ACC_PRIVATE, "renderModel", oldDesc, null, null);
        handler.visibleParameterAnnotations = parameterAnnotations(11, 9);
        handler.visibleAnnotableParameterCount = 11;
        handler.invisibleParameterAnnotations = parameterAnnotations(11, 10);
        handler.invisibleAnnotableParameterCount = 11;
        handler.instructions.add(new InsnNode(RETURN));
        node.methods.add(handler);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertEquals(newDesc, handler.desc);
        assertNull(handler.visibleParameterAnnotations);
        assertNull(handler.invisibleParameterAnnotations);
        assertEquals(0, handler.visibleAnnotableParameterCount);
        assertEquals(0, handler.invisibleAnnotableParameterCount);
        assertTrue(hasCall(handler, INVOKEVIRTUAL, MODEL, "renderToBuffer",
                "(L" + POSE + ";L" + VERTEX + ";III)V"));
        assertClassCanBeSerialized(node);
    }

    @Test
    void geckoRendererOverwriteUsesPackedColorAndCurrentVertexApi() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/client/GeoRendererMixin";
        String oldDesc = "(L" + QUAD + ";L" + MATRIX + ";Lorg/joml/Vector3f;L" + VERTEX + ";IIFFFF)V";
        String newDesc = "(L" + QUAD + ";L" + MATRIX + ";Lorg/joml/Vector3f;L" + VERTEX + ";III)V";
        MethodNode method = new MethodNode(ACC_PUBLIC, "createVerticesOfQuad", oldDesc, null, null);
        method.visibleParameterAnnotations = parameterAnnotations(10, 9);
        method.visibleAnnotableParameterCount = 10;
        method.instructions.add(new InsnNode(RETURN));
        node.methods.add(method);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertEquals(newDesc, method.desc);
        assertNull(method.visibleParameterAnnotations);
        assertEquals(0, method.visibleAnnotableParameterCount);
        assertTrue(hasCall(method, INVOKEINTERFACE, VERTEX, "addVertex", "(FFFIFFIIFFF)V"));
        assertTrue(hasCall(method, INVOKESTATIC, "net/minecraft/util/Mth", "clamp", "(III)I"));
        assertClassCanBeSerialized(node);
    }

    @Test
    void levelRendererKeepsPhasingHookButDropsObsoleteWatcherPoseStackHook() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/client/LevelRendererMixin";
        String oldDesc = "(L" + POSE + ";L" + MATRIX + ";FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;" + CI + ")V";
        String newDesc = "(L" + MATRIX + ";L" + MATRIX + ";FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;" + CI + ")V";
        MethodNode skip = new MethodNode(ACC_PRIVATE, "skipSkyRenderingForPhasingBlindness", oldDesc, null, null);
        skip.instructions.add(new InsnNode(RETURN));
        MethodNode watcher = new MethodNode(ACC_PRIVATE, "renderWatcher", oldDesc, null, null);
        watcher.instructions.add(new InsnNode(RETURN));
        node.methods.add(skip);
        node.methods.add(watcher);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertEquals(newDesc, skip.desc);
        assertNull(find(node, "renderWatcher", oldDesc));
    }

    @Test
    void modelPartInvokerUsesTheNewPackedColorCompileSignature() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladium/mixin/client/ModelPartInvoker";
        String oldDesc = "(L" + POSE + "$Pose;L" + VERTEX + ";IIFFFF)V";
        String newDesc = "(L" + POSE + "$Pose;L" + VERTEX + ";III)V";
        MethodNode method = new MethodNode(ACC_PUBLIC, "invokeCompile", oldDesc, null, null);
        method.invisibleParameterAnnotations = parameterAnnotations(8, 7);
        method.invisibleAnnotableParameterCount = 8;
        node.methods.add(method);

        assertEquals(1, PalladiumMixinCompat1201.apply(node));
        assertTrue(find(node, "invokeCompile", newDesc) != null);
        assertNull(method.invisibleParameterAnnotations);
        assertEquals(0, method.invisibleAnnotableParameterCount);
        assertClassCanBeSerialized(node);
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<AnnotationNode>[] parameterAnnotations(int parameterCount, int annotatedParameter) {
        java.util.List<AnnotationNode>[] annotations =
                (java.util.List<AnnotationNode>[]) new java.util.List<?>[parameterCount];
        annotations[annotatedParameter] = new java.util.ArrayList<>();
        annotations[annotatedParameter].add(new AnnotationNode("Ltest/ParameterMarker;"));
        return annotations;
    }

    private static Object annotationValue(AnnotationNode annotation, String name) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static void assertClassCanBeSerialized(ClassNode node) {
        assertDoesNotThrow(() -> {
            org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
        });
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) if (method.name.equals(name) && method.desc.equals(desc)) return method;
        return null;
    }

    private static MethodInsnNode findCall(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals(name)) return call;
        }
        return null;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static boolean hasVar(MethodNode method, int opcode, int slot) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof VarInsnNode var && var.getOpcode() == opcode && var.var == slot) return true;
        }
        return false;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) return true;
        }
        return false;
    }
}
