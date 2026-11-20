package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Iterator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Restores three AlienEvo mixins whose 1.20.1 @Shadow members were removed or
 * moved in 1.21.1. Each adaptation keeps the original feature behavior while
 * targeting the new owner/member/API shape.
 */
final class AlienEvoLegacyMixins1201 implements Opcodes {
    static final String RENDER_BUFFERS = "net/habb/alienevo/mixin/client/RenderBuffersGlintMixin";
    static final String RIFLE_CROSSHAIR = "net/habb/alienevo/mixin/client/RifleHeatCrosshairMixin";
    static final String WHIRLPOOL_CAMERA = "net/habb/alienevo/mixin/client/WhirlpoolCameraMixin";
    static final String RENDER_TYPES = "net/habb/alienevo/client/renderer/AlienEvoRenderTypes";

    private static final String BUFFER_SOURCE = "net/minecraft/client/renderer/MultiBufferSource$BufferSource";
    private static final String SEQUENCED_MAP = "Ljava/util/SequencedMap;";
    private static final String BUFFER_BUILDER = "com/mojang/blaze3d/vertex/BufferBuilder";
    // Forge1201Transformer's ClassRemapper maps BufferBuilder to this shim before this adapter runs.
    private static final String COMPAT_BUFFER_BUILDER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/CompatBufferBuilder";
    private static final String BYTE_BUFFER_BUILDER = "com/mojang/blaze3d/vertex/ByteBufferBuilder";
    private static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";
    private static final String DELTA_TRACKER = "Lnet/minecraft/client/DeltaTracker;";

    private AlienEvoLegacyMixins1201() {}

    /** Returns the number of targeted structural/member rewrites. */
    static int apply(ClassNode node) {
        return switch (node.name) {
            case RENDER_BUFFERS -> migrateTintedGlintBuffer(node);
            case RIFLE_CROSSHAIR -> migrateRifleHeatCrosshair(node);
            case WHIRLPOOL_CAMERA -> migrateWhirlpoolCamera(node);
            case RENDER_TYPES -> migrateTintedGlintShader(node);
            default -> 0;
        };
    }

    private static int migrateTintedGlintBuffer(ClassNode node) {
        FieldNode shadow = findField(node, "fixedBuffers", "Ljava/util/SortedMap;");
        if (shadow == null) shadow = findField(node, "f_110093_", "Ljava/util/SortedMap;");
        if (shadow == null) return 0;
        MethodNode handler = findMethod(node, "alienevo$registerTintedGlint",
                "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        if (handler == null) {
            throw new IllegalStateException("AlienEvo RenderBuffersGlintMixin lost its inspected constructor callback");
        }

        String bufferSourceDesc = "L" + BUFFER_SOURCE + ";";
        shadow.name = "bufferSource";
        shadow.desc = bufferSourceDesc;
        shadow.signature = null;

        int oldFieldReads = 0;
        int bufferBuilderNews = 0;
        int bufferBuilderConstructors = 0;
        int sortedMapPuts = 0;
        for (AbstractInsnNode instruction : handler.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETFIELD
                    && field.owner.equals(node.name)
                    && (field.name.equals("f_110093_") || field.name.equals("fixedBuffers"))
                    && field.desc.equals("Ljava/util/SortedMap;")) {
                field.name = "bufferSource";
                field.desc = bufferSourceDesc;
                // The 1.21.1 RenderBuffers stores fixed entity buffers on its
                // BufferSource, not directly on RenderBuffers.
                handler.instructions.insert(field, new FieldInsnNode(GETFIELD,
                        BUFFER_SOURCE, "fixedBuffers", SEQUENCED_MAP));
                oldFieldReads++;
            } else if (instruction instanceof TypeInsnNode type && type.getOpcode() == NEW
                    && (type.desc.equals(BUFFER_BUILDER) || type.desc.equals(COMPAT_BUFFER_BUILDER))) {
                // BufferBuilder is class-remapped to CompatBufferBuilder before this adapter runs.
                // BufferSource.fixedBuffers stores the underlying ByteBufferBuilder, not the wrapper.
                type.desc = BYTE_BUFFER_BUILDER;
                bufferBuilderNews++;
            } else if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESPECIAL
                    && (call.owner.equals(BUFFER_BUILDER) || call.owner.equals(COMPAT_BUFFER_BUILDER))
                    && call.name.equals("<init>") && call.desc.equals("(I)V")) {
                call.owner = BYTE_BUFFER_BUILDER;
                bufferBuilderConstructors++;
            } else if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEINTERFACE
                    && call.owner.equals("java/util/SortedMap")
                    && call.name.equals("put")
                    && call.desc.equals("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")) {
                // SequencedMap inherits Map.put; use the stable parent interface.
                call.owner = "java/util/Map";
                sortedMapPuts++;
            }
        }
        if (oldFieldReads != 1 || bufferBuilderNews != 1 || bufferBuilderConstructors != 1 || sortedMapPuts != 1) {
            throw new IllegalStateException("AlienEvo RenderBuffersGlintMixin bytecode no longer matches the inspected "
                    + "fixed-buffer registration shape (fieldReads=" + oldFieldReads
                    + ", bufferNews=" + bufferBuilderNews + ", bufferConstructors=" + bufferBuilderConstructors
                    + ", mapPuts=" + sortedMapPuts + ")");
        }
        return 1;
    }

    /**
     * AlienEvo's 1.20.1 tinted item-glint render type referenced the direct
     * glint shader, which was removed in 1.21.1. Its POSITION_TEX format and
     * item glint texture use the current item-glint shader instead.
     *
     * Run before SoftLink1201: otherwise the removed field read is replaced
     * with null and RenderType.CompositeState fails during client bootstrap.
     */
    private static int migrateTintedGlintShader(ClassNode node) {
        final String shaderOwner = "net/minecraft/client/renderer/RenderStateShard";
        final String shaderDesc = "Lnet/minecraft/client/renderer/RenderStateShard$ShaderStateShard;";
        int oldReads = 0;
        int alreadyMigrated = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof FieldInsnNode field)
                        || field.getOpcode() != GETSTATIC
                        || !field.owner.equals(shaderOwner)
                        || !field.desc.equals(shaderDesc)) continue;
                if (field.name.equals("RENDERTYPE_GLINT_DIRECT_SHADER")) {
                    field.name = "RENDERTYPE_GLINT_SHADER";
                    oldReads++;
                } else if (field.name.equals("RENDERTYPE_GLINT_SHADER")) {
                    alreadyMigrated++;
                }
            }
        }
        if (oldReads == 0 && alreadyMigrated == 1) return 0;
        if (oldReads != 1 || alreadyMigrated != 0) {
            throw new IllegalStateException("AlienEvoRenderTypes no longer contains its inspected direct item-glint shader read "
                    + "(legacyReads=" + oldReads + ", currentReads=" + alreadyMigrated + ")");
        }
        return 1;
    }

    private static int migrateRifleHeatCrosshair(ClassNode node) {
        String oldDesc = "(L" + GUI_GRAPHICS + ";Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";
        String newDesc = "(L" + GUI_GRAPHICS + ";" + DELTA_TRACKER
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";
        MethodNode handler = findMethod(node, "alienevo$drawHeatMeter", oldDesc);
        if (handler == null) return 0;

        int widthReads = 0;
        int heightReads = 0;
        for (AbstractInsnNode instruction : handler.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode field) || field.getOpcode() != GETFIELD
                    || !field.owner.equals(node.name) || !field.desc.equals("I")) continue;
            String methodName;
            if (field.name.equals("f_92977_") || field.name.equals("screenWidth")) {
                methodName = "guiWidth";
                widthReads++;
            } else if (field.name.equals("f_92978_") || field.name.equals("screenHeight")) {
                methodName = "guiHeight";
                heightReads++;
            } else {
                continue;
            }

            AbstractInsnNode receiver = previousRealInstruction(field);
            if (!(receiver instanceof VarInsnNode variable)
                    || variable.getOpcode() != ALOAD || variable.var != 0) {
                throw new IllegalStateException("AlienEvo RifleHeatCrosshairMixin GUI dimension read lost its this receiver");
            }
            // The replacement accessor belongs to the GuiGraphics argument,
            // not to the mixin target Gui instance. Reuse the old receiver slot
            // but load the new callback argument instead.
            variable.var = 1;
            handler.instructions.set(field, new MethodInsnNode(INVOKEVIRTUAL, GUI_GRAPHICS,
                    methodName, "()I", false));
        }
        if (widthReads != 1 || heightReads != 1) {
            throw new IllegalStateException("AlienEvo RifleHeatCrosshairMixin bytecode no longer reads both legacy GUI dimensions "
                    + "(width=" + widthReads + ", height=" + heightReads + ")");
        }

        // The two @Shadow fields existed only to supply Gui's old screenWidth/
        // screenHeight values. GuiGraphics now owns these scaled dimensions.
        for (Iterator<FieldNode> iterator = node.fields.iterator(); iterator.hasNext();) {
            FieldNode field = iterator.next();
            if ((field.name.equals("f_92977_") || field.name.equals("screenWidth")
                    || field.name.equals("f_92978_") || field.name.equals("screenHeight")) && field.desc.equals("I")) {
                iterator.remove();
            }
        }
        replaceDescriptor(handler, newDesc);
        return 1;
    }

    private static int migrateWhirlpoolCamera(ClassNode node) {
        MethodNode shadow = findMethod(node, "m_90568_", "(DDD)V");
        if (shadow == null) shadow = findMethod(node, "move", "(DDD)V");
        if (shadow == null) return 0;
        MethodNode handler = findMethod(node, "alienevo$whirlpoolZoom",
                "(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZFL"
                        + "org/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        if (handler == null) {
            throw new IllegalStateException("AlienEvo WhirlpoolCameraMixin lost its inspected Camera.setup callback");
        }

        int moveCalls = 0;
        int convertedDoubleConstants = 0;
        int convertedZeroes = 0;
        for (AbstractInsnNode instruction : handler.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEVIRTUAL
                    && call.owner.equals(node.name)
                    && (call.name.equals("m_90568_") || call.name.equals("move"))
                    && call.desc.equals("(DDD)V")) {
                call.name = "move";
                call.desc = "(FFF)V";
                moveCalls++;
            } else if (instruction instanceof LdcInsnNode ldc && ldc.cst instanceof Double value) {
                ldc.cst = value.floatValue();
                convertedDoubleConstants++;
            } else if (instruction.getOpcode() == DCONST_0) {
                handler.instructions.set(instruction, new InsnNode(FCONST_0));
                convertedZeroes++;
            }
        }
        if (moveCalls != 1 || convertedDoubleConstants != 1 || convertedZeroes != 2) {
            throw new IllegalStateException("AlienEvo WhirlpoolCameraMixin no longer contains its inspected -6.0/0.0/0.0 move call "
                    + "(moveCalls=" + moveCalls + ", doubleConstants=" + convertedDoubleConstants
                    + ", zeroes=" + convertedZeroes + ")");
        }
        shadow.name = "move";
        shadow.desc = "(FFF)V";
        shadow.signature = null;
        shadow.localVariables = null;
        return 1;
    }

    private static AbstractInsnNode previousRealInstruction(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static FieldNode findField(ClassNode node, String name, String descriptor) {
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(descriptor)) return field;
        }
        return null;
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
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
        method.localVariables = null;
    }
}
