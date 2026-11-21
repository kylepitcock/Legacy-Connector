package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        MethodInsnNode callbackTrace = null;
        for (var instruction : input.instructions) {
            if (instruction instanceof MethodInsnNode methodCall
                    && methodCall.owner.equals("legacyconnector/v1_20_1/PalladiumFlightDebugCompat")
                    && methodCall.name.equals("logKeyEvent")) {
                callbackTrace = methodCall;
                break;
            }
        }
        assertTrue(callbackTrace != null, "the InputEvent.Key callback must get a trace at entry");
        assertEquals("(Ljava/lang/String;Ljava/lang/Object;)V", callbackTrace.desc);

        MethodNode initializer = node.methods.stream()
                .filter(method -> "<clinit>".equals(method.name) && "()V".equals(method.desc))
                .findFirst().orElse(null);
        assertNotNull(initializer, "instrumentation must report when PalladiumCore loads the transformed class");
        boolean loadMarker = false;
        for (var instruction : initializer.instructions) {
            if (instruction instanceof MethodInsnNode methodCall
                    && methodCall.owner.equals("legacyconnector/v1_20_1/PalladiumFlightDebugCompat")
                    && methodCall.name.equals("logInputTraceInstalled")) {
                loadMarker = true;
                break;
            }
        }
        assertTrue(loadMarker, "the transformed subscriber must log its instrumentation when the class loads");

        assertEquals(0, PalladiumFlightInputTrace1201.apply(node),
                "the input trace and class-load marker must be idempotent");
    }

    @Test
    void preservesAnExistingClassInitializerAndItsInstructions() {
        ClassNode node = new ClassNode();
        node.name = "net/threetag/palladiumcore/event/forge/PalladiumCoreClientEventHandler";
        node.superName = "java/lang/Object";

        MethodNode initializer = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
        initializer.instructions.add(new InsnNode(RETURN));
        node.methods.add(initializer);

        MethodNode input = new MethodNode(ACC_PUBLIC | ACC_STATIC, "onKeyInput",
                "(Lnet/neoforged/neoforge/client/event/InputEvent$Key;)V", null, null);
        input.instructions.add(new InsnNode(RETURN));
        input.maxLocals = 1;
        node.methods.add(input);

        assertEquals(1, PalladiumFlightInputTrace1201.apply(node));
        assertTrue(initializer.instructions.getFirst() instanceof org.objectweb.asm.tree.LdcInsnNode,
                "the load marker must be inserted before, not replace, the original initializer");
        assertEquals(0, PalladiumFlightInputTrace1201.apply(node),
                "the input callback must not be instrumented twice");
        assertTrue(initializer.instructions.getLast() instanceof InsnNode);
        assertEquals(RETURN, initializer.instructions.getLast().getOpcode());
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
