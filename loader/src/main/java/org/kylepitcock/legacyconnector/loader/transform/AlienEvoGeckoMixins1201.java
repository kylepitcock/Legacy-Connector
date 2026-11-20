package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adapts three AlienEvo mixins to the inspected GeckoLib 4.9 renderer method
 * descriptors. The rewrites preserve the existing handler bodies and only
 * account for the new partial-tick and packed-color parameters.
 */
final class AlienEvoGeckoMixins1201 implements Opcodes {
    static final String STILL_CLOCK = "net/habb/alienevo/mixin/client/GeckoStillClockMixin";
    static final String GEO_ENTITY_RENDERER = "net/habb/alienevo/mixin/client/GeoEntityRendererMixin";
    static final String RIDE_SEAT_BONE = "net/habb/alienevo/mixin/client/RideSeatBoneMixin";

    private static final String CALLBACK = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String ANIMATABLE = "Lsoftware/bernie/geckolib/animatable/GeoAnimatable;";
    private static final String ANIMATION_STATE = "Lsoftware/bernie/geckolib/animation/AnimationState;";
    private static final String POSE_STACK = "Lcom/mojang/blaze3d/vertex/PoseStack;";
    private static final String ENTITY = "Lnet/minecraft/world/entity/Entity;";
    private static final String BAKED_MODEL = "Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;";
    private static final String RENDER_TYPE = "Lnet/minecraft/client/renderer/RenderType;";
    private static final String BUFFER_SOURCE = "Lnet/minecraft/client/renderer/MultiBufferSource;";
    private static final String VERTEX_CONSUMER = "Lcom/mojang/blaze3d/vertex/VertexConsumer;";

    private static final String OLD_RENDER_ARGS =
            "(" + POSE_STACK + ENTITY + BAKED_MODEL + RENDER_TYPE + BUFFER_SOURCE
                    + VERTEX_CONSUMER + "ZFIIFFFF" + CALLBACK + ")V";
    private static final String NEW_RENDER_ARGS =
            "(" + POSE_STACK + ENTITY + BAKED_MODEL + RENDER_TYPE + BUFFER_SOURCE
                    + VERTEX_CONSUMER + "ZFIII" + CALLBACK + ")V";

    private AlienEvoGeckoMixins1201() {}

    /** Returns the number of mixin classes adapted. */
    static int apply(ClassNode node) {
        return switch (node.name) {
            case STILL_CLOCK -> migrateStillClock(node);
            case GEO_ENTITY_RENDERER -> migrateGeoEntityRenderer(node);
            case RIDE_SEAT_BONE -> migrateRideSeatBone(node);
            default -> 0;
        };
    }

    private static int migrateStillClock(ClassNode node) {
        final String name = "alienevo$poseStill";
        final String oldDesc = "(" + ANIMATABLE + "J" + ANIMATION_STATE + CALLBACK + ")V";
        final String newDesc = "(" + ANIMATABLE + "J" + ANIMATION_STATE + "F" + CALLBACK + ")V";
        MethodNode method = find(node, name, oldDesc);
        if (method == null) {
            if (find(node, name, newDesc) != null) return 0;
            throw new IllegalStateException("AlienEvo GeckoStillClockMixin lost its inspected poseStill handler");
        }

        // GeckoLib 4.9 inserted partialTick before CallbackInfo. Move CallbackInfo
        // and the two existing local references (manager and processor) one slot.
        shiftLocals(method, 5, 1);
        method.maxLocals = Math.max(method.maxLocals + 1, 9);
        replaceDescriptor(method, newDesc);
        return 1;
    }

    private static int migrateGeoEntityRenderer(ClassNode node) {
        final String name = "alienevo$ensureAlienEvoLayers";
        MethodNode method = find(node, name, OLD_RENDER_ARGS);
        if (method == null) {
            if (find(node, name, NEW_RENDER_ARGS) != null) return 0;
            throw new IllegalStateException("AlienEvo GeoEntityRendererMixin lost its inspected layer-registration handler");
        }

        // GeckoLib replaced RGBA float components with one packed-color int.
        // Old local 11 remains the packed color slot; callback 15 -> 12 and
        // the renderer temporary 16 -> 13.
        shiftLocals(method, 12, -3);
        method.maxLocals = Math.max(method.maxLocals - 3, 13);
        replaceDescriptor(method, NEW_RENDER_ARGS);
        return 1;
    }

    private static int migrateRideSeatBone(ClassNode node) {
        final String[] names = {
                "alienevo$beginSeatMeasure",
                "alienevo$endSeatMeasure",
                "alienevo$measureSeatBone"
        };
        int oldHandlers = 0;
        int newHandlers = 0;
        for (String name : names) {
            if (find(node, name, OLD_RENDER_ARGS) != null) oldHandlers++;
            else if (find(node, name, NEW_RENDER_ARGS) != null) newHandlers++;
            else {
                throw new IllegalStateException("AlienEvo RideSeatBoneMixin lost its inspected handler " + name);
            }
        }
        if (oldHandlers == 0 && newHandlers == names.length) return 0;
        if (oldHandlers != names.length || newHandlers != 0) {
            throw new IllegalStateException("AlienEvo RideSeatBoneMixin has a partially migrated handler set "
                    + "(legacy=" + oldHandlers + ", current=" + newHandlers + ")");
        }

        for (String name : names) {
            MethodNode method = find(node, name, OLD_RENDER_ARGS);
            shiftLocals(method, 12, -3);
            method.maxLocals = Math.max(method.maxLocals - 3, 13);
            replaceDescriptor(method, NEW_RENDER_ARGS);
        }
        return 1;
    }

    private static void shiftLocals(MethodNode method, int firstSlot, int delta) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable && variable.var >= firstSlot) {
                variable.var += delta;
            } else if (instruction instanceof IincInsnNode increment && increment.var >= firstSlot) {
                increment.var += delta;
            }
        }
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
        method.parameters = null;
        method.visibleParameterAnnotations = null;
        method.invisibleParameterAnnotations = null;
        method.visibleAnnotableParameterCount = 0;
        method.invisibleAnnotableParameterCount = 0;
        // Old local-variable slots described the removed callback/color layout.
        method.localVariables = null;
    }
}
