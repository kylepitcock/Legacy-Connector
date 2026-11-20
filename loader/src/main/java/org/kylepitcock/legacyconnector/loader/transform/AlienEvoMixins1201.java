package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Objects;
import java.util.function.IntUnaryOperator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Narrow handler-signature adaptations for the native AlienEvo mixins whose
 * Minecraft injection targets changed between 1.20.1 and 1.21.1.
 *
 * This intentionally does not relax Mixin's global policy or manufacture
 * replacements for handlers whose target semantics have not been established.
 */
final class AlienEvoMixins1201 implements Opcodes {
    private static final String CALLBACK = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String DELTA_TRACKER = "Lnet/minecraft/client/DeltaTracker;";
    private static final String GUI_GRAPHICS = "Lnet/minecraft/client/gui/GuiGraphics;";

    private static final String HUD = "net/habb/alienevo/mixin/client/AlienHudMixin";
    private static final String VIGNETTE = "net/habb/alienevo/mixin/client/KineceleranVignetteMixin";
    private static final String NAME_TAG = "net/habb/alienevo/mixin/client/PreviewNameTagMixin";
    private static final String GAME_RENDERER = "net/habb/alienevo/mixin/client/GameRendererMixin";
    private static final String KART_STEER = "net/habb/alienevo/mixin/client/KartSteerMixin";
    private static final String OUTLINE = "net/habb/alienevo/mixin/client/ElectrocutionOutlineChainMixin";
    private static final String SHIELD = "net/habb/alienevo/mixin/ShieldSoundMixin";
    private static final String KART_CAMERA = "net/habb/alienevo/mixin/client/KartCameraMixin";
    private static final String CAMERA_POSITION = "net/habb/alienevo/mixin/client/CameraPositionMixin";
    private static final String CAMERA_ZOOM = "net/habb/alienevo/mixin/scale/client/CameraZoomScaleMixin";
    private static final String MODEL_BAKERY = "net/habb/alienevo/mixin/client/ModelBakeryCrystalMixin";

    private AlienEvoMixins1201() {}

    /** Returns the number of handler methods whose descriptors or bytecode were adapted. */
    static int apply(ClassNode node) {
        Objects.requireNonNull(node, "node");
        return switch (node.name) {
            case HUD -> migrateHudHotbar(node);
            case VIGNETTE -> migrateKineceleranVignette(node);
            case NAME_TAG -> migratePreviewNameTag(node);
            case GAME_RENDERER -> migrateGameRenderer(node);
            case KART_STEER -> migrateKartSteer(node);
            case OUTLINE -> migrateElectrocutionOutline(node);
            case SHIELD -> migrateShieldDisable(node);
            case CAMERA_POSITION -> migrateCameraPosition(node);
            case CAMERA_ZOOM -> migrateCameraZoom(node, "alienevo$scaleCameraDistance");
            case KART_CAMERA -> migrateCameraZoom(node, "alienevo$kartSpeedZoom");
            case MODEL_BAKERY -> migrateModelBakeryCrystal(node);
            default -> 0;
        };
    }

    private static int migrateHudHotbar(ClassNode node) {
        String oldDesc = "(F" + GUI_GRAPHICS + CALLBACK + ")V";
        String newDesc = "(" + GUI_GRAPHICS + DELTA_TRACKER + CALLBACK + ")V";
        MethodNode method = find(node, "alienevo$renderCreativeElements", oldDesc);
        if (method == null) return 0;
        // The old partial-tick argument is unused. The graphics argument moves
        // from local 2 to local 1; DeltaTracker occupies local 2 in 1.21.1.
        remapSlots(method, slot -> slot == 2 ? 1 : slot);
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migrateKineceleranVignette(ClassNode node) {
        String oldDesc = "(F" + GUI_GRAPHICS + CALLBACK + ")V";
        String newDesc = "(" + GUI_GRAPHICS + DELTA_TRACKER + CALLBACK + ")V";
        MethodNode method = find(node, "alienevo$drawVignetteBeforeHotbar", oldDesc);
        if (method == null) return 0;
        // The render callback no longer receives the float partial tick. The
        // graphics reference moves from local 2 to local 1; the callback info
        // and the mixin's first free temporary local retain their old slots.
        remapSlots(method, slot -> slot == 2 ? 1 : slot);
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migratePreviewNameTag(ClassNode node) {
        String oldDesc = "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I"
                + CALLBACK + ")V";
        String newDesc = "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF"
                + CALLBACK + ")V";
        MethodNode method = find(node, "alienevo$suppressPreviewNameTag", oldDesc);
        if (method == null) return 0;
        // EntityRenderer.renderNameTag gained a partial-tick float after packed
        // light. Existing callback info and any temporary locals move up one.
        remapSlots(method, slot -> slot >= 6 ? slot + 1 : slot);
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migrateGameRenderer(ClassNode node) {
        String oldDesc = "(FJLcom/mojang/blaze3d/vertex/PoseStack;" + CALLBACK + ")V";
        String newDesc = "(" + DELTA_TRACKER + CALLBACK + ")V";
        MethodNode method = find(node, "alienevo$depthOfFieldBeforeHand", oldDesc);
        if (method == null) return 0;
        if (!replacePartialTickLoad(method, 1)) return 0;
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migrateKartSteer(ClassNode node) {
        String oldDesc = "(FJZ" + CALLBACK + ")V";
        String newDesc = "(" + DELTA_TRACKER + "Z" + CALLBACK + ")V";
        MethodNode method = find(node, "alienevo$kartSteerFrame", oldDesc);
        if (method == null) return 0;
        // The old frame time and timestamp are unused by this handler. Its
        // boolean argument is still the render-outline flag in 1.21.1.
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migrateElectrocutionOutline(ClassNode node) {
        String oldDesc = "(Lcom/mojang/blaze3d/vertex/PoseStack;FJZ"
                + "Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;"
                + "Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;" + CALLBACK + ")V";
        String newDesc = "(" + DELTA_TRACKER + "Z"
                + "Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;"
                + "Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;"
                + CALLBACK + ")V";
        MethodNode method = find(node, "alienevo$lateOutline", oldDesc);
        if (method == null) return 0;
        if (!replacePartialTickLoad(method, 2)) return 0;
        // Old: PoseStack(1), partial tick(2), long(3-4), bool(5), Camera(6),
        // GameRenderer(7), LightTexture(8), matrix(9), CallbackInfo(10), temps(11+).
        // New: DeltaTracker(1), bool(2), Camera(3), GameRenderer(4), LightTexture(5),
        // projection matrix(6), model-view matrix(7), CallbackInfo(8), temps(9+).
        remapSlots(method, slot -> slot >= 5 && slot <= 9 ? slot - 3 : slot >= 10 ? slot - 2 : slot);
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migrateShieldDisable(ClassNode node) {
        String oldDesc = "(Z" + CALLBACK + ")V";
        String newDesc = "(" + CALLBACK + ")V";
        MethodNode method = find(node, "alienevo$preventShieldDisable", oldDesc);
        if (method == null) return 0;
        // Player.disableShield() is now parameterless. The old boolean was
        // unused; CallbackInfo and the one temporary Player local shift down.
        remapSlots(method, slot -> slot >= 2 ? slot - 1 : slot);
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migrateCameraZoom(ClassNode node, String handlerName) {
        MethodNode method = find(node, handlerName, "(D)D");
        if (method == null) return 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable) {
                if (variable.getOpcode() == DLOAD) variable.setOpcode(FLOAD);
                else if (variable.getOpcode() == DSTORE) variable.setOpcode(FSTORE);
            } else if (instruction.getOpcode() == DRETURN) {
                method.instructions.set(instruction, new InsnNode(FRETURN));
            } else if (instruction.getOpcode() == DMUL) {
                method.instructions.set(instruction, new InsnNode(FMUL));
            } else if (instruction.getOpcode() == DCMPL) {
                method.instructions.set(instruction, new InsnNode(FCMPL));
            } else if (instruction.getOpcode() == DCMPG) {
                method.instructions.set(instruction, new InsnNode(FCMPG));
            } else if (instruction.getOpcode() == F2D) {
                method.instructions.remove(instruction);
            }
        }
        replaceDescriptor(method, "(F)F");
        return 1;
    }

    private static int migrateCameraPosition(ClassNode node) {
        String oldDesc = "(DLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";
        String newDesc = "(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";
        MethodNode method = find(node, "alienevo$keepZoomWhenPivotBuried", oldDesc);
        if (method == null) return 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable) {
                int slot = variable.var;
                if (slot == 3) variable.var = 2;       // CallbackInfoReturnable
                else if (slot == 4) variable.var = 3;  // formerly a double temporary, now a float
                else if (slot >= 6) variable.var = slot - 2;
                if (variable.getOpcode() == DLOAD) variable.setOpcode(FLOAD);
                else if (variable.getOpcode() == DSTORE) variable.setOpcode(FSTORE);
            } else if (instruction instanceof org.objectweb.asm.tree.TypeInsnNode type
                    && type.getOpcode() == CHECKCAST && type.desc.equals("java/lang/Double")) {
                type.desc = "java/lang/Float";
            } else if (instruction instanceof MethodInsnNode call && call.owner.equals("java/lang/Double")) {
                call.owner = "java/lang/Float";
                if (call.name.equals("doubleValue") && call.desc.equals("()D")) call.desc = "()F";
                else if (call.name.equals("valueOf") && call.desc.equals("(D)Ljava/lang/Double;")) {
                    call.desc = "(F)Ljava/lang/Float;";
                }
            } else if (instruction.getOpcode() == DCMPG) {
                method.instructions.set(instruction, new InsnNode(FCMPG));
            } else if (instruction.getOpcode() == DCMPL) {
                method.instructions.set(instruction, new InsnNode(FCMPL));
            }
        }
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migrateModelBakeryCrystal(ClassNode node) {
        String oldDesc = "(Ljava/util/function/BiFunction;" + CALLBACK + ")V";
        String textureGetter = "Lnet/minecraft/client/resources/model/ModelBakery$TextureGetter;";
        String newDesc = "(" + textureGetter + CALLBACK + ")V";
        MethodNode method = find(node, "alienevo$bakeStaticCrystal", oldDesc);
        if (method == null) return 0;
        String modelClass = "net/habb/alienevo/client/renderer/DiamondBlockStaticModel";
        boolean adapted = false;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == INVOKESTATIC
                    && call.owner.equals(modelClass) && call.name.equals("install")
                    && call.desc.equals("(Ljava/util/Map;Ljava/util/function/BiFunction;)V")) {
                method.instructions.insertBefore(call, new MethodInsnNode(INVOKESTATIC,
                        "org/kylepitcock/legacyconnector/v1_20_1/migrate/ModelBakeryCompat",
                        "asBiFunction", "(" + textureGetter + ")Ljava/util/function/BiFunction;", false));
                adapted = true;
            }
        }
        if (!adapted) return 0;
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return method;
        }
        return null;
    }

    private static void replaceDescriptor(MethodNode method, String descriptor) {
        method.desc = descriptor;
        method.signature = null;
        // The source handlers have no parameter annotations. Keeping the old
        // parameter-name/debug layout would describe the removed 1.20.1 args.
        method.parameters = null;
        method.visibleParameterAnnotations = null;
        method.invisibleParameterAnnotations = null;
        method.visibleAnnotableParameterCount = 0;
        method.invisibleAnnotableParameterCount = 0;
        method.localVariables = null;
    }

    private static void remapSlots(MethodNode method, IntUnaryOperator mapper) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable) {
                variable.var = mapper.applyAsInt(variable.var);
            } else if (instruction instanceof IincInsnNode increment) {
                increment.var = mapper.applyAsInt(increment.var);
            }
        }
        if (method.localVariables != null) {
            for (var local : method.localVariables) local.index = mapper.applyAsInt(local.index);
        }
    }

    private static boolean replacePartialTickLoad(MethodNode method, int oldSlot) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable
                    && variable.getOpcode() == FLOAD && variable.var == oldSlot) {
                InsnList replacement = new InsnList();
                replacement.add(new VarInsnNode(ALOAD, 1));
                replacement.add(new InsnNode(ICONST_0));
                replacement.add(new MethodInsnNode(INVOKEINTERFACE, "net/minecraft/client/DeltaTracker",
                        "getGameTimeDeltaPartialTick", "(Z)F", true));
                method.instructions.insertBefore(variable, replacement);
                method.instructions.remove(variable);
                return true;
            }
        }
        return false;
    }
}
