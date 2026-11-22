package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adapts AlienEvo's GeckoLib-backed player model animation call to GeckoLib 4.9.x. */
final class AlienEvoGeckoRenderLayerModel1201 implements Opcodes {
    static final String TARGET_CLASS =
            "net/habb/alienevo/compat/geckolib/renderlayer/AlienGeckoRenderLayerModel";
    static final String BUFFERING_GLOW_CONSUMER = "net/habb/alienevo/client/renderer/BufferingGlowConsumer";
    static final String VERTEX_CONSUMER = "com/mojang/blaze3d/vertex/VertexConsumer";
    static final String MULTI_BUFFER_SOURCE = "net/minecraft/client/renderer/MultiBufferSource";
    static final String RENDER_TYPE = "net/minecraft/client/renderer/RenderType";
    static final String RESOURCE_LOCATION = "net/minecraft/resources/ResourceLocation";
    static final String RENDER_TYPE_FUNCTION =
            "net/threetag/palladium/client/renderer/renderlayer/RenderTypeFunction";
    static final String ALIEN_LAYER_STATE = "net/habb/alienevo/compat/geckolib/renderlayer/AlienLayerState";
    static final String ALIEN_GECKO_RENDER_LAYER = "net/habb/alienevo/compat/geckolib/renderlayer/AlienGeckoRenderLayer";
    /**
     * The class remapper has already applied lib-classes.tsv when this adapter runs:
     * GeckoLib 4.8 RenderUtils/CoreGeoBone become GeckoLib 4.9 RenderUtil/GeoBone.
     */
    static final String RENDER_UTIL = "software/bernie/geckolib/util/RenderUtil";
    static final String PREP_MATRIX_FOR_BONE_DESC =
            "(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/cache/object/GeoBone;)V";
    static final String BUFFER_REFRESH =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoBufferRefresh1201";

    static final String MODEL_BUFFER_RENDER_DESC =
            "(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";
    static final String RECURSIVE_DESC =
            "(Lcom/mojang/blaze3d/vertex/PoseStack;L" + ALIEN_LAYER_STATE
                    + ";Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;"
                    + "L" + MULTI_BUFFER_SOURCE + ";L" + VERTEX_CONSUMER + ";ZFIIFFFF)V";
    private static final String MULTI_GET_BUFFER_DESC =
            "(L" + RENDER_TYPE + ";)L" + VERTEX_CONSUMER + ";";
    private static final String PALLADIUM_CONSUMER_DESC =
            "(L" + MULTI_BUFFER_SOURCE + ";L" + RESOURCE_LOCATION + ";Z)L" + VERTEX_CONSUMER + ";";

    private AlienEvoGeckoRenderLayerModel1201() {}

    /**
     * The old call stack is [GeoModel, GeoAnimatable, long, AnimationState].
     * Duplicate the AnimationState to append GeckoLib 4.9's partial-tick argument.
     */
    static int apply(ClassNode node) {
        if (!TARGET_CLASS.equals(node.name)) return 0;

        int legacyCalls = 0;
        int adaptedCalls = 0;
        MethodNode targetMethod = null;
        MethodInsnNode targetCall = null;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !PalladiumGeckoRenderLayer1201.GEO_MODEL.equals(call.owner)
                        || !"handleAnimations".equals(call.name)) continue;
                if (!"renderToBuffer".equals(method.name) || call.getOpcode() != INVOKEVIRTUAL) {
                    throw new IllegalStateException(
                            "AlienEvo AlienGeckoRenderLayerModel has an unexpected GeoModel.handleAnimations call site");
                }
                if (PalladiumGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    legacyCalls++;
                    targetMethod = method;
                    targetCall = call;
                } else if (PalladiumGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    adaptedCalls++;
                } else {
                    throw new IllegalStateException(
                            "AlienEvo AlienGeckoRenderLayerModel has an unknown GeoModel.handleAnimations descriptor: "
                                    + call.desc);
                }
            }
        }

        int changes = 0;
        if (legacyCalls == 0 && adaptedCalls == 1) {
            // Animation bridge is already present; continue validating the buffer seams.
        } else {
            if (legacyCalls != 1 || adaptedCalls != 0 || targetMethod == null || targetCall == null) {
                throw new IllegalStateException(
                        "AlienEvo AlienGeckoRenderLayerModel expected exactly one legacy GeoModel.handleAnimations call; found "
                                + legacyCalls + " legacy and " + adaptedCalls + " adapted");
            }
            InsnList partialTickArgument = new InsnList();
            partialTickArgument.add(new InsnNode(DUP));
            partialTickArgument.add(new MethodInsnNode(INVOKEVIRTUAL,
                    PalladiumGeckoRenderLayer1201.ANIMATION_STATE, "getPartialTick", "()F", false));
            targetMethod.instructions.insertBefore(targetCall, partialTickArgument);
            targetCall.desc = PalladiumGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC;
            targetMethod.maxStack++;
            changes++;
        }
        changes += applyRecursiveBufferRefresh(node);
        changes += applyBufferingGlowSolidType(node);
        return changes;
    }

    /** Reacquires only recognized stale consumers using the layer's Palladium factory. */
    private static int applyRecursiveBufferRefresh(ClassNode node) {
        MethodNode recursive = findUniqueMethod(node, "renderRecursively", RECURSIVE_DESC);
        if (recursive == null) throw new IllegalStateException("Missing concrete AlienLayerState renderRecursively overload");
        int helperCalls = countCalls(recursive, BUFFER_REFRESH, "needsRefresh", "(L" + VERTEX_CONSUMER + ";)Z");
        int glowCalls = countCalls(recursive, BUFFERING_GLOW_CONSUMER, "legacyRefreshSolid", "(L" + MULTI_BUFFER_SOURCE + ";)V");
        if (helperCalls == 1 && glowCalls == 1) return 0;
        if (helperCalls != 0 || glowCalls != 0) throw new IllegalStateException("Partial recursive buffer-refresh adapter state");

        // ClassReader/ClassRemapper has already folded the GeckoLib 4.8 names
        // before this adapter runs (lib-classes.tsv): inspect the 4.9 owner and
        // descriptor that are actually present in this ClassNode.
        MethodInsnNode prep = findUniqueCall(recursive, RENDER_UTIL, "prepMatrixForBone",
                PREP_MATRIX_FOR_BONE_DESC);
        MethodInsnNode child = findUniqueCall(recursive, node.name, "renderRecursively", RECURSIVE_DESC);
        if (prep == null || prep.getOpcode() != INVOKESTATIC
                || child == null || child.getOpcode() != INVOKEVIRTUAL) {
            throw new IllegalStateException(
                    "AlienEvo recursive rendering anchors changed after GeckoLib 4.9 remapping");
        }

        LabelNode skip = new LabelNode();
        LabelNode ordinary = new LabelNode();
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 7));
        code.add(new JumpInsnNode(IFNE, skip));

        code.add(new VarInsnNode(ALOAD, 6));
        code.add(new TypeInsnNode(INSTANCEOF, BUFFERING_GLOW_CONSUMER));
        code.add(new JumpInsnNode(IFEQ, ordinary));
        code.add(new VarInsnNode(ALOAD, 6));
        code.add(new TypeInsnNode(CHECKCAST, BUFFERING_GLOW_CONSUMER));
        code.add(new VarInsnNode(ALOAD, 5));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, BUFFERING_GLOW_CONSUMER,
                "legacyRefreshSolid", "(L" + MULTI_BUFFER_SOURCE + ";)V", false));
        code.add(new JumpInsnNode(GOTO, skip));

        code.add(ordinary);
        code.add(new VarInsnNode(ALOAD, 6));
        code.add(new MethodInsnNode(INVOKESTATIC, BUFFER_REFRESH, "needsRefresh",
                "(L" + VERTEX_CONSUMER + ";)Z", false));
        code.add(new JumpInsnNode(IFEQ, skip));
        code.add(new VarInsnNode(ALOAD, 2));
        code.add(new FieldInsnNode(GETFIELD, ALIEN_LAYER_STATE, "layer", "L" + ALIEN_GECKO_RENDER_LAYER + ";"));
        code.add(new FieldInsnNode(GETFIELD, ALIEN_GECKO_RENDER_LAYER, "renderType", "L" + RENDER_TYPE_FUNCTION + ";"));
        code.add(new VarInsnNode(ALOAD, 5));
        code.add(new VarInsnNode(ALOAD, 2));
        code.add(new FieldInsnNode(GETFIELD, ALIEN_LAYER_STATE, "layer", "L" + ALIEN_GECKO_RENDER_LAYER + ";"));
        code.add(new FieldInsnNode(GETFIELD, ALIEN_GECKO_RENDER_LAYER, "cachedTexture", "L" + RESOURCE_LOCATION + ";"));
        code.add(new InsnNode(ICONST_0));
        code.add(new MethodInsnNode(INVOKEINTERFACE, RENDER_TYPE_FUNCTION,
                "createVertexConsumer", PALLADIUM_CONSUMER_DESC, true));
        code.add(new VarInsnNode(ASTORE, 6));
        code.add(skip);

        recursive.instructions.insert(prep, code);
        recursive.maxStack = Math.max(recursive.maxStack, 6);
        return 1;
    }

    /** Saves entityTranslucent(holoTex) separately from the BGC's independent glow type. */
    private static int applyBufferingGlowSolidType(ClassNode node) {
        MethodNode render = findUniqueMethod(node, "renderToBuffer", MODEL_BUFFER_RENDER_DESC);
        if (render == null) throw new IllegalStateException("Missing AlienEvo model buffer render method");
        int setterCalls = countCalls(render, BUFFERING_GLOW_CONSUMER, "legacySetSolidType", "(L" + RENDER_TYPE + ";)V");
        int constructorCalls = countCalls(render, BUFFERING_GLOW_CONSUMER, "<init>",
                "(L" + VERTEX_CONSUMER + ";L" + RENDER_TYPE + ";)V");
        if (setterCalls == 1 && constructorCalls == 1) return 0;
        if (setterCalls != 0 || constructorCalls != 1) throw new IllegalStateException("Unexpected BGC constructor shape");

        MethodInsnNode constructor = findUniqueCall(render, BUFFERING_GLOW_CONSUMER, "<init>",
                "(L" + VERTEX_CONSUMER + ";L" + RENDER_TYPE + ";)V");
        AbstractInsnNode next = nextMeaningful(constructor);
        if (!(next instanceof VarInsnNode store) || store.getOpcode() != ASTORE || store.var != 2) {
            throw new IllegalStateException("Preview BGC is no longer stored in local 2");
        }
        AbstractInsnNode glowLoad = previousMeaningful(constructor);
        AbstractInsnNode getBuffer = previousMeaningful(glowLoad);
        AbstractInsnNode solidType = previousMeaningful(getBuffer);
        AbstractInsnNode textureLoad = previousMeaningful(solidType);
        AbstractInsnNode sourceLoad = previousMeaningful(textureLoad);
        if (!(glowLoad instanceof VarInsnNode glow && glow.getOpcode() == ALOAD && glow.var == 19)
                || !(getBuffer instanceof MethodInsnNode get && get.getOpcode() == INVOKEINTERFACE
                && MULTI_BUFFER_SOURCE.equals(get.owner) && "getBuffer".equals(get.name)
                && MULTI_GET_BUFFER_DESC.equals(get.desc))
                || !(solidType instanceof MethodInsnNode type && type.getOpcode() == INVOKESTATIC
                && RENDER_TYPE.equals(type.owner) && "entityTranslucent".equals(type.name)
                && ("(L" + RESOURCE_LOCATION + ";)L" + RENDER_TYPE + ";").equals(type.desc))
                || !(textureLoad instanceof VarInsnNode texture && texture.getOpcode() == ALOAD && texture.var == 18)
                || !(sourceLoad instanceof VarInsnNode source && source.getOpcode() == ALOAD && source.var == 10)) {
            throw new IllegalStateException("Preview solid consumer is no longer getBuffer(entityTranslucent(holoTex))");
        }

        InsnList setSolid = new InsnList();
        setSolid.add(new VarInsnNode(ALOAD, 2));
        setSolid.add(new VarInsnNode(ALOAD, 18));
        setSolid.add(new MethodInsnNode(INVOKESTATIC, RENDER_TYPE, "entityTranslucent",
                "(L" + RESOURCE_LOCATION + ";)L" + RENDER_TYPE + ";", false));
        setSolid.add(new MethodInsnNode(INVOKEVIRTUAL, BUFFERING_GLOW_CONSUMER,
                "legacySetSolidType", "(L" + RENDER_TYPE + ";)V", false));
        render.instructions.insert(store, setSolid);
        render.maxStack = Math.max(render.maxStack, 3);
        return 1;
    }

    private static MethodNode findUniqueMethod(ClassNode node, String name, String desc) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                if (found != null) throw new IllegalStateException("Duplicate AlienEvo method " + name + desc);
                found = method;
            }
        }
        return found;
    }

    private static MethodInsnNode findUniqueCall(MethodNode method, String owner, String name, String desc) {
        MethodInsnNode found = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(name) && call.desc.equals(desc)) {
                if (found != null) throw new IllegalStateException("Duplicate call " + owner + "." + name + desc);
                found = call;
            }
        }
        return found;
    }

    private static int countCalls(MethodNode method, String owner, String name, String desc) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(name) && call.desc.equals(desc)) count++;
        }
        return count;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode value = instruction == null ? null : instruction.getPrevious();
        while (value != null && value.getOpcode() < 0) value = value.getPrevious();
        return value;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode value = instruction == null ? null : instruction.getNext();
        while (value != null && value.getOpcode() < 0) value = value.getNext();
        return value;
    }
}
