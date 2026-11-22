package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.transform.compat.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression tests for the legacy AlienEvo LateEffectBuffers flush call. */
class AlienEvoLateEffectBuffers1201Test implements Opcodes {
    private static ClassNode lateEffectBuffers() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = AlienEvoLateEffectBuffers1201.TARGET;
        node.superName = "java/lang/Object";

        MethodNode flush = new MethodNode(ACC_PUBLIC | ACC_STATIC, "flush", "()V", null, null);
        flush.instructions.add(new InsnNode(ACONST_NULL)); // RenderType receiver
        flush.instructions.add(new InsnNode(ACONST_NULL)); // CompatBufferBuilder
        flush.instructions.add(new InsnNode(ACONST_NULL)); // VertexSorting
        flush.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoLateEffectBuffers1201.RENDER_TYPE, "end",
                AlienEvoLateEffectBuffers1201.LEGACY_DESC, false));
        flush.instructions.add(new InsnNode(RETURN));
        flush.maxStack = 3;
        node.methods.add(flush);
        return node;
    }

    @Test
    void replacesOnlyTheInspectedRenderTypeEndInvocation() {
        ClassNode node = lateEffectBuffers();

        assertEquals(1, AlienEvoLateEffectBuffers1201.apply(node));

        MethodNode flush = node.methods.get(0);
        MethodInsnNode call = null;
        int helperCalls = 0;
        int legacyCalls = 0;
        for (var instruction : flush.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode method)) continue;
            if (method.owner.equals(AlienEvoLateEffectBuffers1201.BUILDER)
                    && method.name.equals(AlienEvoLateEffectBuffers1201.DRAW_NAME)
                    && method.desc.equals(AlienEvoLateEffectBuffers1201.DRAW_DESC)) {
                helperCalls++;
                call = method;
            }
            if (method.owner.equals(AlienEvoLateEffectBuffers1201.RENDER_TYPE) && method.name.equals("end")) legacyCalls++;
        }

        assertEquals(1, helperCalls);
        assertEquals(0, legacyCalls);
        assertEquals(INVOKESTATIC, call.getOpcode());
        assertEquals(0, AlienEvoLateEffectBuffers1201.apply(node));
    }

    @Test
    void failsClosedWhenTheLegacyEndDescriptorChanges() {
        ClassNode node = lateEffectBuffers();
        MethodNode flush = node.methods.get(0);
        for (var instruction : flush.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("end")) {
                call.desc = "()V";
            }
        }
        assertThrows(IllegalStateException.class, () -> AlienEvoLateEffectBuffers1201.apply(node));
    }

    @Test
    void failsClosedWhenTheInspectedFlushCallIsMissing() {
        ClassNode node = lateEffectBuffers();
        node.methods.get(0).instructions.clear();
        node.methods.get(0).instructions.add(new InsnNode(RETURN));
        assertThrows(IllegalStateException.class, () -> AlienEvoLateEffectBuffers1201.apply(node));
    }

    @Test
    void leavesOtherClassesUntouched() {
        ClassNode node = lateEffectBuffers();
        node.name = "example/OtherRenderer";
        assertEquals(0, AlienEvoLateEffectBuffers1201.apply(node));
    }
}
