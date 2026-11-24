package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression coverage for opt-in possession hand render observations. */
class AlienEvoPossessionHandDiagnostics1201Test implements Opcodes {
    private static ClassNode fixture() {
        ClassNode node = new ClassNode();
        node.name = AlienEvoPossessionHandDiagnostics1201.TARGET;
        addHook(node, "alienevo_beforeRender", "alienevo_swapIn");
        addHook(node, "alienevo_afterRender", "alienevo_swapOut");
        return node;
    }

    private static void addHook(ClassNode node, String handler, String swap) {
        MethodNode method = new MethodNode(ACC_PRIVATE, handler,
                AlienEvoPossessionHandDiagnostics1201.HANDLER_DESC, null, null);
        node.methods.add(method);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoPossessionHandDiagnostics1201.TARGET, swap, "()V", false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 1;
        method.maxLocals = 7;
    }

    @Test
    void tracesBothRenderHooksAfterTheirItemSwap() {
        ClassNode node = fixture();

        assertEquals(2, AlienEvoPossessionHandDiagnostics1201.apply(node));
        for (MethodNode method : node.methods) {
            int traces = 0;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && "traceHandRenderState".equals(call.name)) {
                    traces++;
                    assertEquals(INVOKESTATIC, call.getOpcode());
                    assertEquals(AlienEvoPossessionHandDiagnostics1201.HELPER, call.owner);
                    assertEquals(AlienEvoPossessionHandDiagnostics1201.TRACE_DESC, call.desc);
                }
            }
            assertEquals(1, traces);
        }
        assertEquals(0, AlienEvoPossessionHandDiagnostics1201.apply(node));
    }

    @Test
    void failsClosedWhenAnExpectedSwapHookIsMissing() {
        ClassNode node = fixture();
        MethodNode before = node.methods.get(0);
        before.instructions.remove(before.instructions.get(1));
        assertThrows(IllegalStateException.class, () -> AlienEvoPossessionHandDiagnostics1201.apply(node));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode node = fixture();
        node.name = "example/OtherMixin";
        assertEquals(0, AlienEvoPossessionHandDiagnostics1201.apply(node));
    }
}
