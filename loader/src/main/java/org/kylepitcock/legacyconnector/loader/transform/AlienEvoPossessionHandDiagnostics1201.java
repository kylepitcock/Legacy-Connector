package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adds opt-in observations to AlienEvo's possession first-person hand swap hooks. */
final class AlienEvoPossessionHandDiagnostics1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/mixin/client/PossessionHandRenderMixin";
    static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoPossessionDiagnostics1201Runtime";
    static final String PLAYER = "net/minecraft/client/player/LocalPlayer";
    static final String CALLBACK = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    static final String BUFFER =
            "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;";
    static final String POSE = "Lcom/mojang/blaze3d/vertex/PoseStack;";
    static final String HANDLER_DESC = "(F" + POSE + BUFFER + "L" + PLAYER + ";I" + CALLBACK + ")V";
    static final String TRACE_DESC = "(L" + PLAYER + ";Ljava/lang/String;)V";

    private AlienEvoPossessionHandDiagnostics1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;
        int changed = trace(node, "alienevo_beforeRender", "alienevo_swapIn", "before");
        changed += trace(node, "alienevo_afterRender", "alienevo_swapOut", "after");
        return changed;
    }

    private static int trace(ClassNode node, String handlerName, String swapMethod, String phase) {
        MethodNode handler = null;
        for (MethodNode method : node.methods) {
            if (!handlerName.equals(method.name) || !HANDLER_DESC.equals(method.desc)) continue;
            if (handler != null) throw new IllegalStateException("AlienEvo possession hand hook is duplicated: " + handlerName);
            handler = method;
        }
        if (handler == null || handler.instructions == null) {
            throw new IllegalStateException("AlienEvo possession hand hook changed shape: " + handlerName);
        }

        int swapCalls = 0;
        int traceCalls = 0;
        MethodInsnNode swap = null;
        for (AbstractInsnNode instruction : handler.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.getOpcode() == INVOKEVIRTUAL && TARGET.equals(call.owner)
                    && swapMethod.equals(call.name) && "()V".equals(call.desc)) {
                swapCalls++;
                swap = call;
            } else if (call.getOpcode() == INVOKESTATIC && HELPER.equals(call.owner)
                    && "traceHandRenderState".equals(call.name) && TRACE_DESC.equals(call.desc)) {
                traceCalls++;
            }
        }

        if (swapCalls == 1 && traceCalls == 1) return 0;
        if (swapCalls != 1 || traceCalls != 0) {
            throw new IllegalStateException("AlienEvo possession hand hook expected one " + swapMethod
                    + " call and no diagnostic; found " + swapCalls + " swap and " + traceCalls + " trace");
        }

        InsnList probe = new InsnList();
        probe.add(new VarInsnNode(ALOAD, 4)); // LocalPlayer handler argument.
        probe.add(new LdcInsnNode(phase));
        probe.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceHandRenderState", TRACE_DESC, false));
        handler.instructions.insert(swap, probe);
        handler.maxStack = Math.max(handler.maxStack, 2);
        return 1;
    }
}
