package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Installs opt-in observations around AlienEvo's transform-bubble coordinate
 * conversions. The injected runtime methods are no-ops unless the dedicated
 * transform-bubble debug property is enabled.
 */
final class AlienEvoTransformBubbleDiagnostics1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/client/energybeam/TransformBubbleRenderer";
    static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoTransformBubbleDiagnostics1201Runtime";

    static final String RENDER_DESC =
            "(Lnet/minecraft/client/player/AbstractClientPlayer;"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;F"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;IZF)V";
    static final String SPHERE_DESC =
            "(Lnet/minecraft/client/player/AbstractClientPlayer;F"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;FZ)V";
    static final String TRANSLATE_DESC = "(DDD)V";

    static final String TRACE_ENTRY = "traceRenderEntry";
    static final String TRACE_ENTRY_DESC =
            "(Lnet/minecraft/client/player/AbstractClientPlayer;"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;F"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;ZF)V";
    static final String TRACE_HEAD = "traceHeadAnchor";
    static final String TRACE_HEAD_DESC =
            "(Lnet/minecraft/client/player/AbstractClientPlayer;"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;DFFZF"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;)V";
    static final String TRACE_CAMERA = "traceCameraOffset";
    static final String TRACE_CAMERA_DESC =
            "(Lnet/minecraft/client/player/AbstractClientPlayer;Ljava/lang/Object;"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;DDDF"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;)V";

    private static final String POSE_STACK = "com/mojang/blaze3d/vertex/PoseStack";
    private static final String VEC3 = "net/minecraft/world/phys/Vec3";
    private static final String SPHERE_METHOD = "renderTransformSphere";

    private AlienEvoTransformBubbleDiagnostics1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode render = findUnique(node, "render", RENDER_DESC);
        MethodNode sphere = findUnique(node, SPHERE_METHOD, SPHERE_DESC);
        if (render == null || sphere == null) {
            throw new IllegalStateException("AlienEvo TransformBubbleRenderer lost its inspected render methods");
        }

        int entryCalls = countCalls(render, TRACE_ENTRY);
        int headCalls = countCalls(sphere, TRACE_HEAD);
        int cameraCalls = countCalls(sphere, TRACE_CAMERA);
        if (entryCalls == 1 && headCalls == 1 && cameraCalls == 1) return 0;
        if (entryCalls != 0 || headCalls != 0 || cameraCalls != 0) {
            throw new IllegalStateException("AlienEvo TransformBubbleRenderer has a partial transform-bubble diagnostic state");
        }

        int sphereCalls = 0;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(node.name)
                    && call.name.equals(SPHERE_METHOD)
                    && call.desc.equals(SPHERE_DESC)
                    && call.getOpcode() == INVOKEVIRTUAL) {
                sphereCalls++;
            }
        }
        if (sphereCalls != 1) {
            throw new IllegalStateException("AlienEvo TransformBubbleRenderer.render expected one renderTransformSphere call; found "
                    + sphereCalls);
        }

        List<MethodInsnNode> translations = new ArrayList<>();
        for (AbstractInsnNode instruction : sphere.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call)
                    || !POSE_STACK.equals(call.owner)
                    || !"translate".equals(call.name)
                    || !TRANSLATE_DESC.equals(call.desc)) {
                continue;
            }
            if (call.getOpcode() != INVOKEVIRTUAL || call.itf) {
                throw new IllegalStateException("AlienEvo TransformBubbleRenderer has a non-virtual PoseStack.translate call");
            }
            translations.add(call);
        }
        if (translations.size() != 3) {
            throw new IllegalStateException("AlienEvo TransformBubbleRenderer.renderTransformSphere expected exactly three inspected PoseStack.translate calls; found "
                    + translations.size());
        }

        MethodInsnNode headTranslation = translations.get(0);
        requireLoads(sphere, headTranslation, "head-anchor translation",
                new int[][] {
                    {ALOAD, 3}, {ALOAD, 16}, {ALOAD, 17}, {DLOAD, 20},
                    {FLOAD, 18}, {FLOAD, 19}, {ILOAD, 6}, {FLOAD, 5}
                });
        MethodInsnNode cameraTranslation = translations.get(1);
        requireLoads(sphere, cameraTranslation, "first-person camera translation",
                new int[][] {
                    {ALOAD, 3}, {ALOAD, 31}, {ALOAD, 35}, {ALOAD, 36}, {ALOAD, 37},
                    {DLOAD, 38}, {DLOAD, 40}, {DLOAD, 42}, {FLOAD, 44}
                });

        InsnList entryTrace = new InsnList();
        entryTrace.add(new VarInsnNode(ALOAD, 1)); // player
        entryTrace.add(new VarInsnNode(ALOAD, 2)); // configured beam origin relative to the effect entity
        entryTrace.add(new VarInsnNode(ALOAD, 3)); // target relative to the effect entity
        entryTrace.add(new VarInsnNode(FLOAD, 4)); // progress
        entryTrace.add(new VarInsnNode(ALOAD, 5)); // incoming render pose
        entryTrace.add(new VarInsnNode(ILOAD, 8)); // first-person path
        entryTrace.add(new VarInsnNode(FLOAD, 9)); // partial tick
        entryTrace.add(new MethodInsnNode(INVOKESTATIC, HELPER,
                TRACE_ENTRY, TRACE_ENTRY_DESC, false));
        render.instructions.insert(entryTrace);

        InsnList headTrace = new InsnList();
        headTrace.add(new VarInsnNode(ALOAD, 1)); // player
        headTrace.add(new VarInsnNode(ALOAD, 16)); // HEAD world position
        headTrace.add(new VarInsnNode(ALOAD, 17)); // interpolated player position
        headTrace.add(new org.objectweb.asm.tree.VarInsnNode(DLOAD, 20)); // selected world Y
        headTrace.add(new VarInsnNode(FLOAD, 18)); // bounding-box height
        headTrace.add(new VarInsnNode(FLOAD, 19)); // model-height scale
        headTrace.add(new VarInsnNode(ILOAD, 6)); // first-person path
        headTrace.add(new VarInsnNode(FLOAD, 5)); // partial tick
        headTrace.add(new VarInsnNode(ALOAD, 3)); // pose after head-anchor translation
        headTrace.add(new MethodInsnNode(INVOKESTATIC, HELPER,
                TRACE_HEAD, TRACE_HEAD_DESC, false));
        sphere.instructions.insert(headTranslation, headTrace);

        InsnList cameraTrace = new InsnList();
        cameraTrace.add(new VarInsnNode(ALOAD, 1)); // player
        cameraTrace.add(new VarInsnNode(ALOAD, 31)); // camera
        cameraTrace.add(new VarInsnNode(ALOAD, 35)); // look vector
        cameraTrace.add(new VarInsnNode(ALOAD, 36)); // up vector
        cameraTrace.add(new VarInsnNode(ALOAD, 37)); // negative-left vector
        cameraTrace.add(new VarInsnNode(DLOAD, 38)); // look distance
        cameraTrace.add(new VarInsnNode(DLOAD, 40)); // tan(FOV / 2)
        cameraTrace.add(new VarInsnNode(DLOAD, 42)); // vertical camera offset
        cameraTrace.add(new VarInsnNode(FLOAD, 44)); // effect FOV scale
        cameraTrace.add(new VarInsnNode(ALOAD, 3)); // final first-person pose used by beam spikes
        cameraTrace.add(new MethodInsnNode(INVOKESTATIC, HELPER,
                TRACE_CAMERA, TRACE_CAMERA_DESC, false));
        sphere.instructions.insert(cameraTranslation, cameraTrace);

        render.maxStack = Math.max(render.maxStack, 7);
        sphere.maxStack = Math.max(sphere.maxStack, 13);
        return 3;
    }

    private static MethodNode findUnique(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name) || !descriptor.equals(method.desc)) continue;
            if (found != null) {
                throw new IllegalStateException("Duplicate AlienEvo TransformBubbleRenderer." + name + descriptor);
            }
            found = method;
        }
        return found;
    }

    private static int countCalls(MethodNode method, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && HELPER.equals(call.owner) && name.equals(call.name)
                    && call.getOpcode() == INVOKESTATIC) {
                count++;
            }
        }
        return count;
    }

    private static void requireLoads(MethodNode method, AbstractInsnNode anchor,
            String description, int[][] requiredLoads) {
        int anchorIndex = method.instructions.indexOf(anchor);
        int start = Math.max(0, anchorIndex - 120);
        boolean[] found = new boolean[requiredLoads.length];
        for (int index = start; index < anchorIndex; index++) {
            AbstractInsnNode instruction = method.instructions.get(index);
            if (!(instruction instanceof VarInsnNode load)) continue;
            for (int required = 0; required < requiredLoads.length; required++) {
                if (load.getOpcode() == requiredLoads[required][0]
                        && load.var == requiredLoads[required][1]) {
                    found[required] = true;
                }
            }
        }
        for (int index = 0; index < requiredLoads.length; index++) {
            if (!found[index]) {
                throw new IllegalStateException("AlienEvo TransformBubbleRenderer lost a required local for "
                        + description + ": opcode=" + requiredLoads[index][0]
                        + ", local=" + requiredLoads[index][1]);
            }
        }
    }
}
