package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicVerifier;

/** Tests that opt-in AlienEvo traces are attached to the intended runtime boundaries. */
class AlienEvoRuntimeDiagnostics1201Test implements Opcodes {
    private static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoRuntimeDiagnostics1201Runtime";

    @Test
    void tracesEveryKineceleranRunAnimationExitAndIsIdempotent() throws AnalyzerException {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/client/model/animation/aliens/kineceleran/KineceleranRunAnimation";
        MethodNode animate = new MethodNode(ACC_PUBLIC, "animate",
                "(Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$Builder;"
                        + "Lnet/minecraft/client/player/AbstractClientPlayer;"
                        + "Lnet/minecraft/client/model/HumanoidModel;"
                        + "Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$FirstPersonContext;F)V",
                null, null);
        animate.instructions.add(new InsnNode(RETURN));
        animate.instructions.add(new InsnNode(RETURN));
        animate.instructions.add(new InsnNode(RETURN));
        animate.maxLocals = 6;
        node.methods.add(animate);

        assertEquals(3, AlienEvoRuntimeDiagnostics1201.apply(node));
        assertEquals(3, countCalls(animate, "traceKineceleranRun"));
        new Analyzer<>(new BasicVerifier()).analyze(node.name, animate);
        assertEquals(0, AlienEvoRuntimeDiagnostics1201.apply(node));
    }

    @Test
    void tracesCannonboltSpinGateAfterThePalladiumAbilityCheckAndIsIdempotent() throws AnalyzerException {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/client/model/animation/aliens/arburian_pelarota/ChestSpinAnimation";
        MethodNode animate = new MethodNode(ACC_PUBLIC, "animate",
                "(Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$Builder;"
                        + "Lnet/minecraft/client/player/AbstractClientPlayer;"
                        + "Lnet/minecraft/client/model/HumanoidModel;"
                        + "Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$FirstPersonContext;F)V",
                null, null);
        animate.instructions.add(new InsnNode(ACONST_NULL));
        animate.instructions.add(new VarInsnNode(ASTORE, 6)); // animation state
        animate.instructions.add(new VarInsnNode(ALOAD, 2));
        animate.instructions.add(new InsnNode(ACONST_NULL)); // ability superpower id
        animate.instructions.add(new org.objectweb.asm.tree.LdcInsnNode("ball_roll"));
        animate.instructions.add(new MethodInsnNode(INVOKESTATIC,
                "net/threetag/palladium/power/ability/AbilityUtil", "isEnabled",
                "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/String;)Z",
                false));
        animate.instructions.add(new VarInsnNode(ISTORE, 7));
        animate.instructions.add(new VarInsnNode(ILOAD, 7));
        animate.instructions.add(new InsnNode(POP));
        animate.instructions.add(new InsnNode(RETURN));
        animate.maxLocals = 8;
        animate.maxStack = 3;
        node.methods.add(animate);

        assertEquals(1, AlienEvoRuntimeDiagnostics1201.apply(node));
        MethodInsnNode trace = findCall(animate, "traceCannonboltSpinGate");
        assertEquals("(Ljava/lang/Object;ZLjava/lang/Object;)V", trace.desc);
        assertEquals(ALOAD, trace.getPrevious().getOpcode());
        assertEquals(6, ((VarInsnNode) trace.getPrevious()).var);
        assertEquals(ILOAD, trace.getPrevious().getPrevious().getOpcode());
        assertEquals(7, ((VarInsnNode) trace.getPrevious().getPrevious()).var);
        assertEquals(ALOAD, trace.getPrevious().getPrevious().getPrevious().getOpcode());
        assertEquals(2, ((VarInsnNode) trace.getPrevious().getPrevious().getPrevious()).var);
        new Analyzer<>(new BasicVerifier()).analyze(node.name, animate);
        assertEquals(0, AlienEvoRuntimeDiagnostics1201.apply(node));
    }

    @Test
    void tracesTheAppliedFirstPersonArmPoseAfterTheModelPartIsUpdated() throws AnalyzerException {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/client/model/animation/FirstPersonArmAnimator";
        MethodNode apply = new MethodNode(ACC_PUBLIC | ACC_STATIC, "apply",
                "(Lnet/minecraft/client/model/geom/ModelPart;Z)V", null, null);
        apply.instructions.add(new InsnNode(ACONST_NULL));
        apply.instructions.add(new VarInsnNode(ASTORE, 2));
        apply.instructions.add(new InsnNode(ACONST_NULL));
        apply.instructions.add(new VarInsnNode(ASTORE, 3));
        apply.instructions.add(new InsnNode(RETURN));
        apply.maxLocals = 4;
        node.methods.add(apply);

        assertEquals(1, AlienEvoRuntimeDiagnostics1201.apply(node));
        MethodInsnNode trace = findCall(apply, "traceFirstPersonArmApply");
        assertEquals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", trace.desc);
        // apply(ModelPart, boolean) is static: local 0 is the ModelPart,
        // local 1 is the boolean flag, and locals 2/3 hold the animation data.
        assertEquals(ALOAD, trace.getPrevious().getOpcode());
        assertEquals(0, ((VarInsnNode) trace.getPrevious()).var);
        assertEquals(ALOAD, trace.getPrevious().getPrevious().getOpcode());
        assertEquals(3, ((VarInsnNode) trace.getPrevious().getPrevious()).var);
        assertEquals(ALOAD, trace.getPrevious().getPrevious().getPrevious().getOpcode());
        assertEquals(2, ((VarInsnNode) trace.getPrevious().getPrevious().getPrevious()).var);

        // Verify the transformed descriptor/local types. This catches an ALOAD
        // of local 1 (the boolean argument), which caused the runtime VerifyError.
        new Analyzer<>(new BasicVerifier()).analyze(node.name, apply);

        assertEquals(0, AlienEvoRuntimeDiagnostics1201.apply(node));
    }

    @Test
    void recordsRawAndSuppressedVanillaSwingProgressWithoutChangingTheReturnValue() throws AnalyzerException {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/mixin/client/CancelFirstPersonSwingMixin";
        MethodNode swing = new MethodNode(ACC_PRIVATE, "alienevo$cancelSwingProgress",
                "(F)F", null, null);
        swing.instructions.add(new VarInsnNode(FLOAD, 1));
        swing.instructions.add(new InsnNode(FRETURN));
        swing.maxLocals = 2;
        swing.maxStack = 1;
        node.methods.add(swing);

        assertEquals(1, AlienEvoRuntimeDiagnostics1201.apply(node));
        MethodInsnNode trace = findCall(swing, "traceVanillaSwing");
        assertEquals("(FF)V", trace.desc);
        assertTrue(trace.getPrevious() instanceof VarInsnNode);
        assertEquals(FLOAD, trace.getPrevious().getOpcode());
        assertEquals(DUP, trace.getPrevious().getPrevious().getOpcode());
        assertTrue(swing.maxStack >= 3, "swing trace must reserve the duplicated result and original progress");
        new Analyzer<>(new BasicVerifier()).analyze(node.name, swing);
        assertEquals(0, AlienEvoRuntimeDiagnostics1201.apply(node));
    }

    @Test
    void tracesEarlyMissingAnchorWithoutReadingUninitializedLocalsAndIsIdempotent() throws AnalyzerException {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/client/BoneAnchors";
        MethodNode boneWorld = new MethodNode(ACC_PUBLIC | ACC_STATIC, "boneWorld",
                "(ILjava/lang/String;ZFLorg/joml/Matrix3f;Lnet/minecraft/world/phys/Vec3;)"
                        + "Lnet/minecraft/world/phys/Vec3;", null, null);
        LabelNode nonNullOrigin = new LabelNode();
        boneWorld.instructions.add(new InsnNode(ICONST_0));
        boneWorld.instructions.add(new JumpInsnNode(IFEQ, nonNullOrigin));
        boneWorld.instructions.add(new InsnNode(ACONST_NULL));
        AbstractInsnNode earlyReturn = new InsnNode(ARETURN);
        boneWorld.instructions.add(earlyReturn);
        boneWorld.instructions.add(nonNullOrigin);
        boneWorld.instructions.add(new InsnNode(ACONST_NULL));
        boneWorld.instructions.add(new VarInsnNode(ASTORE, 6)); // captured pose is initialized only after early return
        boneWorld.instructions.add(new InsnNode(ACONST_NULL));
        boneWorld.instructions.add(new VarInsnNode(ASTORE, 7)); // transformed local translation
        boneWorld.instructions.add(new VarInsnNode(ALOAD, 5)); // returned camera-relative origin
        boneWorld.instructions.add(new InsnNode(ARETURN));
        boneWorld.maxLocals = 8;
        boneWorld.maxStack = 1;
        node.methods.add(boneWorld);

        assertEquals(2, AlienEvoRuntimeDiagnostics1201.apply(node));
        assertEquals(1, countCalls(boneWorld, "traceBeamAnchor"));
        assertEquals(2, countCalls(boneWorld, "traceMissingBeamAnchor"));

        MethodInsnNode earlyMissingTrace = null;
        for (var instruction : boneWorld.instructions) {
            if (instruction == earlyReturn) break;
            if (instruction instanceof MethodInsnNode call
                    && HELPER.equals(call.owner)
                    && "traceMissingBeamAnchor".equals(call.name)) {
                earlyMissingTrace = call;
            }
        }
        assertTrue(earlyMissingTrace != null, "early null exit must get a missing-anchor trace");
        assertEquals(ILOAD, earlyMissingTrace.getPrevious().getOpcode());
        assertEquals(2, ((VarInsnNode) earlyMissingTrace.getPrevious()).var);
        assertEquals(ALOAD, earlyMissingTrace.getPrevious().getPrevious().getOpcode());
        assertEquals(1, ((VarInsnNode) earlyMissingTrace.getPrevious().getPrevious()).var);
        assertEquals(ILOAD, earlyMissingTrace.getPrevious().getPrevious().getPrevious().getOpcode());
        assertEquals(0, ((VarInsnNode) earlyMissingTrace.getPrevious().getPrevious().getPrevious()).var);

        // Local 7 is TOP on the early-null branch: analyze the transformed
        // method to ensure the diagnostic never reads that uninitialized slot.
        new Analyzer<>(new BasicVerifier()).analyze(node.name, boneWorld);

        assertEquals(9, boneWorld.maxLocals);
        assertTrue(boneWorld.maxStack >= 9);
        assertEquals(0, AlienEvoRuntimeDiagnostics1201.apply(node));
    }

    @Test
    void tracesMobPortraitFitBoundsForBadgeDiagnostics() throws AnalyzerException {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/client/screen/MobPortrait";
        String drawDesc = "(Lnet/minecraft/client/gui/GuiGraphics;"
                + "Lnet/minecraft/world/entity/LivingEntity;FFFFLjava/lang/Object;Z)V";
        String fitDesc = "(Lnet/minecraft/client/gui/GuiGraphics;[FFFFF"
                + "Lnet/habb/alienevo/client/screen/MobPortrait$Painter;)V";
        MethodNode draw = new MethodNode(ACC_PUBLIC | ACC_STATIC, "draw", drawDesc, null, null);
        // The original method computes/stores its outline in local 8 before fitting.
        draw.instructions.add(new InsnNode(ACONST_NULL));
        draw.instructions.add(new VarInsnNode(ASTORE, 8));
        draw.instructions.add(new VarInsnNode(ALOAD, 0));
        draw.instructions.add(new VarInsnNode(ALOAD, 8));
        draw.instructions.add(new VarInsnNode(FLOAD, 2));
        draw.instructions.add(new VarInsnNode(FLOAD, 3));
        draw.instructions.add(new VarInsnNode(FLOAD, 4));
        draw.instructions.add(new VarInsnNode(FLOAD, 5));
        draw.instructions.add(new InsnNode(ACONST_NULL));
        MethodInsnNode fit = new MethodInsnNode(INVOKESTATIC,
                node.name, "fit", fitDesc, false);
        draw.instructions.add(fit);
        draw.instructions.add(new InsnNode(RETURN));
        draw.maxLocals = 9;
        draw.maxStack = 7;
        node.methods.add(draw);

        assertEquals(1, AlienEvoRuntimeDiagnostics1201.apply(node));
        MethodInsnNode trace = findCall(draw, "traceBadgePortraitDraw");
        assertEquals("(Ljava/lang/Object;Ljava/lang/Object;[FFFFFZ)V", trace.desc);
        assertEquals(INVOKESTATIC, trace.getOpcode());
        assertEquals(trace, fit.getPrevious(), "trace must run immediately before the original fit call");
        new Analyzer<>(new BasicVerifier()).analyze(node.name, draw);
        assertTrue(draw.maxStack >= 16);
        assertEquals(0, AlienEvoRuntimeDiagnostics1201.apply(node));
    }

    @Test
    void tracesConcreteBadgePagePreparationAndBehindRendering() throws AnalyzerException {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/client/screen/badge/DataPage";
        String pageDesc = "(Lnet/minecraft/client/gui/GuiGraphics;II)V";
        MethodNode prepare = new MethodNode(ACC_PUBLIC, "prepare", pageDesc, null, null);
        prepare.instructions.add(new InsnNode(RETURN));
        prepare.maxLocals = 4;
        MethodNode renderBehind = new MethodNode(ACC_PUBLIC, "renderBehind", pageDesc, null, null);
        renderBehind.instructions.add(new InsnNode(RETURN));
        renderBehind.maxLocals = 4;
        node.methods.add(prepare);
        node.methods.add(renderBehind);

        assertEquals(2, AlienEvoRuntimeDiagnostics1201.apply(node));
        assertEquals(1, countCalls(prepare, "traceBadgePageRender"));
        assertEquals(1, countCalls(renderBehind, "traceBadgePageRender"));
        MethodInsnNode trace = findCall(prepare, "traceBadgePageRender");
        assertEquals("(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/String;)V", trace.desc);
        assertEquals("prepare", ((org.objectweb.asm.tree.LdcInsnNode) trace.getPrevious()).cst);
        new Analyzer<>(new BasicVerifier()).analyze(node.name, prepare);
        new Analyzer<>(new BasicVerifier()).analyze(node.name, renderBehind);
        assertEquals(0, AlienEvoRuntimeDiagnostics1201.apply(node));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode node = new ClassNode();
        node.name = "example/UnrelatedRenderer";
        MethodNode method = new MethodNode(ACC_PUBLIC, "run", "()V", null, null);
        method.instructions.add(new InsnNode(RETURN));
        node.methods.add(method);

        assertEquals(0, AlienEvoRuntimeDiagnostics1201.apply(node));
        assertEquals(0, countCalls(method, "traceKineceleranRun"));
    }

    private static int countCalls(MethodNode method, String name) {
        int count = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && HELPER.equals(call.owner)
                    && name.equals(call.name)) count++;
        }
        return count;
    }

    private static MethodInsnNode findCall(MethodNode method, String name) {
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && HELPER.equals(call.owner)
                    && name.equals(call.name)) return call;
        }
        throw new AssertionError("Diagnostic call not found: " + name);
    }
}
