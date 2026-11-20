package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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
    private static final String POSE_STACK = "Lcom/mojang/blaze3d/vertex/PoseStack;";
    private static final String ENTITY = "Lnet/minecraft/world/entity/Entity;";
    private static final String BAKED_MODEL = "Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;";
    private static final String GEO_BONE = "Lsoftware/bernie/geckolib/cache/object/GeoBone;";
    private static final String RENDER_TYPE = "Lnet/minecraft/client/renderer/RenderType;";
    private static final String BUFFER_SOURCE = "Lnet/minecraft/client/renderer/MultiBufferSource;";
    private static final String VERTEX_CONSUMER = "Lcom/mojang/blaze3d/vertex/VertexConsumer;";

    private static final String OLD_RENDER_ARGS = renderArgs(BAKED_MODEL, false);
    private static final String NEW_RENDER_ARGS = renderArgs(BAKED_MODEL, true);
    private static final String OLD_BONE_RENDER_ARGS = renderArgs(GEO_BONE, false);
    private static final String NEW_BONE_RENDER_ARGS = renderArgs(GEO_BONE, true);

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
        MethodNode method = findNamed(node, name);
        if (method == null) {
            throw new IllegalStateException("AlienEvo GeckoStillClockMixin lost its poseStill handler");
        }

        Type[] arguments = Type.getArgumentTypes(method.desc);
        if (isStillClockDescriptor(arguments, true) && Type.VOID_TYPE.equals(Type.getReturnType(method.desc))) {
            return 0;
        }
        if (!isStillClockDescriptor(arguments, false) || Type.VOID_TYPE != Type.getReturnType(method.desc)
                || (method.access & ACC_STATIC) != 0) {
            throw new IllegalStateException("AlienEvo GeckoStillClockMixin has an unexpected poseStill descriptor: "
                    + method.desc);
        }

        // The source JAR's GeckoLib owners can be remapped before this adapter
        // runs. Match the stable parameter roles rather than hardcoding those
        // owners, then preserve the already-remapped types in the new descriptor.
        Type[] migratedArguments = new Type[arguments.length + 1];
        System.arraycopy(arguments, 0, migratedArguments, 0, arguments.length - 1);
        migratedArguments[arguments.length - 1] = Type.FLOAT_TYPE;
        migratedArguments[arguments.length] = arguments[arguments.length - 1];
        String migratedDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, migratedArguments);

        int callbackSlot = 1; // instance methods reserve slot zero for this
        for (int i = 0; i < arguments.length - 1; i++) {
            callbackSlot += arguments[i].getSize();
        }
        // GeckoLib's handler shape is (GeoAnimatable, long, AnimationState, CallbackInfo).
        // The float occupies the old CallbackInfo slot, so move it and all later locals.
        shiftLocals(method, callbackSlot, 1);
        method.maxLocals = Math.max(method.maxLocals + 1, callbackSlot + 2);
        replaceDescriptor(method, migratedDescriptor);
        return 1;
    }

    private static boolean isStillClockDescriptor(Type[] args, boolean withPartialTick) {
        if (args.length != (withPartialTick ? 5 : 4)) return false;
        return hasSimpleObjectName(args[0], "GeoAnimatable")
                && args[1].equals(Type.LONG_TYPE)
                && hasSimpleObjectName(args[2], "AnimationState")
                && (!withPartialTick || args[3].equals(Type.FLOAT_TYPE))
                && CALLBACK.equals(args[withPartialTick ? 4 : 3].getDescriptor());
    }

    private static boolean hasSimpleObjectName(Type type, String simpleName) {
        return type.getSort() == Type.OBJECT
                && (type.getInternalName().equals(simpleName)
                || type.getInternalName().endsWith("/" + simpleName));
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
        final MethodNode[] legacyMethods = new MethodNode[names.length];
        int oldHandlers = 0;
        int newHandlers = 0;
        for (int i = 0; i < names.length; i++) {
            // The first two injections target GeoRenderer#render and receive a
            // BakedGeoModel. The recursive bone injection receives a GeoBone.
            String oldDesc = i == 2 ? OLD_BONE_RENDER_ARGS : OLD_RENDER_ARGS;
            String newDesc = i == 2 ? NEW_BONE_RENDER_ARGS : NEW_RENDER_ARGS;
            MethodNode legacy = find(node, names[i], oldDesc);
            if (legacy != null) {
                legacyMethods[i] = legacy;
                oldHandlers++;
            } else if (find(node, names[i], newDesc) != null) {
                newHandlers++;
            } else {
                throw new IllegalStateException("AlienEvo RideSeatBoneMixin lost its inspected handler " + names[i]);
            }
        }
        if (oldHandlers == 0 && newHandlers == names.length) return 0;
        if (oldHandlers != names.length || newHandlers != 0) {
            throw new IllegalStateException("AlienEvo RideSeatBoneMixin has a partially migrated handler set "
                    + "(legacy=" + oldHandlers + ", current=" + newHandlers + ")");
        }

        for (int i = 0; i < names.length; i++) {
            MethodNode method = legacyMethods[i];
            String newDesc = i == 2 ? NEW_BONE_RENDER_ARGS : NEW_RENDER_ARGS;
            shiftLocals(method, 12, -3);
            method.maxLocals = Math.max(method.maxLocals - 3, 13);
            replaceDescriptor(method, newDesc);
        }
        return 1;
    }

    private static String renderArgs(String geometryType, boolean packedColor) {
        return "(" + POSE_STACK + ENTITY + geometryType + RENDER_TYPE + BUFFER_SOURCE
                + VERTEX_CONSUMER + (packedColor ? "ZFIII" : "ZFIIFFFF") + CALLBACK + ")V";
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

    private static MethodNode findNamed(ClassNode node, String name) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!method.name.equals(name)) continue;
            if (found != null) {
                throw new IllegalStateException("AlienEvo mixin has duplicate " + name + " handlers");
            }
            found = method;
        }
        return found;
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
