package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for the opt-in TransformBubbleRenderer coordinate diagnostics. */
class AlienEvoTransformBubbleDiagnostics1201Test implements Opcodes {
    private static ClassNode fixture() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = AlienEvoTransformBubbleDiagnostics1201.TARGET;
        node.superName = "java/lang/Object";
        node.methods.add(renderMethod());
        node.methods.add(sphereMethod());
        return node;
    }

    private static MethodNode renderMethod() {
        MethodNode method = new MethodNode(ACC_PUBLIC, "render",
                AlienEvoTransformBubbleDiagnostics1201.RENDER_DESC, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(FLOAD, 4));
        method.instructions.add(new VarInsnNode(ALOAD, 5));
        method.instructions.add(new VarInsnNode(ALOAD, 6));
        method.instructions.add(new VarInsnNode(FLOAD, 9));
        method.instructions.add(new VarInsnNode(ILOAD, 8));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoTransformBubbleDiagnostics1201.TARGET, "renderTransformSphere",
                AlienEvoTransformBubbleDiagnostics1201.SPHERE_DESC, false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 7;
        method.maxLocals = 10;
        return method;
    }

    private static MethodNode sphereMethod() {
        MethodNode method = new MethodNode(ACC_PRIVATE, "renderTransformSphere",
                AlienEvoTransformBubbleDiagnostics1201.SPHERE_DESC, null, null);

        // First call: player-position minus the resolved HEAD world position.
        addLoads(method, new int[][] {
            {ALOAD, 3}, {ALOAD, 17}, {ALOAD, 16}, {DLOAD, 20},
            {FLOAD, 18}, {FLOAD, 19}, {ILOAD, 6}, {FLOAD, 5}
        });
        addTranslate(method);
        // Second call: look * distance - up * vertical FOV offset.
        addLoads(method, new int[][] {
            {ALOAD, 3}, {ALOAD, 31}, {ALOAD, 35}, {ALOAD, 36}, {ALOAD, 37},
            {DLOAD, 38}, {DLOAD, 40}, {DLOAD, 42}, {FLOAD, 44}
        });
        addTranslate(method);
        // Third call belongs to the separate third-person rendering branch.
        method.instructions.add(new VarInsnNode(ALOAD, 3));
        method.instructions.add(new InsnNode(DCONST_0));
        method.instructions.add(new InsnNode(DCONST_0));
        method.instructions.add(new InsnNode(DCONST_0));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "com/mojang/blaze3d/vertex/PoseStack",
                "translate", AlienEvoTransformBubbleDiagnostics1201.TRANSLATE_DESC, false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 4;
        method.maxLocals = 45;
        return method;
    }

    private static void addLoads(MethodNode method, int[][] loads) {
        for (int[] load : loads) method.instructions.add(new VarInsnNode(load[0], load[1]));
    }

    private static void addTranslate(MethodNode method) {
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "com/mojang/blaze3d/vertex/PoseStack",
                "translate", AlienEvoTransformBubbleDiagnostics1201.TRANSLATE_DESC, false));
    }

    private static List<MethodInsnNode> calls(MethodNode method, String name) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(AlienEvoTransformBubbleDiagnostics1201.HELPER)
                    && call.name.equals(name)) {
                result.add(call);
            }
        }
        return result;
    }

    @Test
    void addsEntryHeadAndCameraObservationsAndIsIdempotent() {
        ClassNode node = fixture();
        assertEquals(3, AlienEvoTransformBubbleDiagnostics1201.apply(node));

        MethodNode render = node.methods.get(0);
        MethodNode sphere = node.methods.get(1);
        assertEquals(1, calls(render, AlienEvoTransformBubbleDiagnostics1201.TRACE_ENTRY).size());
        assertEquals(1, calls(sphere, AlienEvoTransformBubbleDiagnostics1201.TRACE_HEAD).size());
        assertEquals(1, calls(sphere, AlienEvoTransformBubbleDiagnostics1201.TRACE_CAMERA).size());
        assertTrue(render.maxStack >= 7);
        assertTrue(sphere.maxStack >= 13);
        assertEquals(0, AlienEvoTransformBubbleDiagnostics1201.apply(node));
    }

    @Test
    void failsClosedWhenOneOfTheInspectedPoseTranslationsIsMissing() {
        ClassNode node = fixture();
        MethodNode sphere = node.methods.get(1);
        for (AbstractInsnNode instruction : sphere.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && "translate".equals(call.name)) {
                sphere.instructions.remove(call);
                break;
            }
        }
        assertThrows(IllegalStateException.class, () -> AlienEvoTransformBubbleDiagnostics1201.apply(node));
    }

    @Test
    void failsClosedWhenTheHeadAnchorLocalLayoutChanges() {
        ClassNode node = fixture();
        MethodNode sphere = node.methods.get(1);
        for (AbstractInsnNode instruction : sphere.instructions.toArray()) {
            if (instruction instanceof VarInsnNode load && load.getOpcode() == DLOAD && load.var == 20) {
                sphere.instructions.remove(load);
                break;
            }
        }
        assertThrows(IllegalStateException.class, () -> AlienEvoTransformBubbleDiagnostics1201.apply(node));
    }

    @Test
    void failsClosedOnPartialInstrumentation() {
        ClassNode node = fixture();
        MethodNode render = node.methods.get(0);
        render.instructions.insert(new MethodInsnNode(INVOKESTATIC,
                AlienEvoTransformBubbleDiagnostics1201.HELPER,
                AlienEvoTransformBubbleDiagnostics1201.TRACE_ENTRY,
                AlienEvoTransformBubbleDiagnostics1201.TRACE_ENTRY_DESC, false));
        assertThrows(IllegalStateException.class, () -> AlienEvoTransformBubbleDiagnostics1201.apply(node));
    }

    @Test
    void leavesOtherClassesUntouched() {
        ClassNode node = fixture();
        node.name = "example/UnrelatedRenderer";
        assertEquals(0, AlienEvoTransformBubbleDiagnostics1201.apply(node));
    }
}
