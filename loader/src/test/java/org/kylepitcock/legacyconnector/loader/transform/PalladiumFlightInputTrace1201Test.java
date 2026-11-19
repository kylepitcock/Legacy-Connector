package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class PalladiumFlightInputTrace1201Test implements Opcodes {
    @Test
    void tracesPalladiumCoreKeyboardSubscriberAndIsIdempotent() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = "net/threetag/palladiumcore/event/forge/PalladiumCoreClientEventHandler";
        node.superName = "java/lang/Object";

        MethodNode input = new MethodNode(ACC_PUBLIC | ACC_STATIC, "onKeyInput",
                "(Lnet/neoforged/neoforge/client/event/InputEvent$Key;)V", null, null);
        input.instructions.add(new InsnNode(RETURN));
        input.maxLocals = 1;
        node.methods.add(input);

        assertEquals(1, PalladiumFlightInputTrace1201.apply(node));
        MethodInsnNode call = null;
        for (var instruction : input.instructions) {
            if (instruction instanceof MethodInsnNode methodCall
                    && methodCall.owner.equals("legacyconnector/v1_20_1/PalladiumFlightDebugCompat")
                    && methodCall.name.equals("logKeyEvent")) {
                call = methodCall;
                break;
            }
        }
        assertTrue(call != null, "the InputEvent.Key callback must get a trace at entry");
        assertEquals("(Ljava/lang/String;Ljava/lang/Object;)V", call.desc);
        assertEquals(0, PalladiumFlightInputTrace1201.apply(node),
                "the input trace must be idempotent");
    }

    @Test
    void ignoresOtherClassesAndHandlersWithoutKeyboardEvents() {
        ClassNode unrelated = new ClassNode();
        unrelated.name = "net/threetag/palladiumcore/event/forge/OtherHandler";
        unrelated.methods.add(new MethodNode(ACC_PUBLIC | ACC_STATIC, "onTick", "()V", null, null));
        assertEquals(0, PalladiumFlightInputTrace1201.apply(unrelated));

        ClassNode target = new ClassNode();
        target.name = "net/threetag/palladiumcore/event/forge/PalladiumCoreClientEventHandler";
        target.methods.add(new MethodNode(ACC_PUBLIC | ACC_STATIC, "onTick", "()V", null, null));
        assertEquals(0, PalladiumFlightInputTrace1201.apply(target));
    }
}
