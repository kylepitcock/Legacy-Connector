package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adds an opt-in trace to PalladiumCore's real keyboard subscriber. This distinguishes
 * "NeoForge registered the subscriber" from "the key callback actually received input".
 */
final class PalladiumFlightInputTrace1201 implements Opcodes {
    private static final String HANDLER =
            "net/threetag/palladiumcore/event/forge/PalladiumCoreClientEventHandler";
    private static final String KEY_EVENT =
            "net/neoforged/neoforge/client/event/InputEvent$Key";
    private static final String HELPER =
            "legacyconnector/v1_20_1/PalladiumFlightDebugCompat";

    private PalladiumFlightInputTrace1201() {
    }

    static int apply(ClassNode node) {
        if (!HANDLER.equals(node.name)) return 0;

        int changed = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null || method.instructions.size() == 0) continue;
            if (hasTrace(method)) continue;

            int local = (method.access & ACC_STATIC) == 0 ? 1 : 0;
            int keyLocal = -1;
            for (Type argument : Type.getArgumentTypes(method.desc)) {
                if (argument.getSort() == Type.OBJECT && KEY_EVENT.equals(argument.getInternalName())) {
                    keyLocal = local;
                    break;
                }
                local += argument.getSize();
            }
            if (keyLocal < 0) continue;

            InsnList trace = new InsnList();
            trace.add(new LdcInsnNode(method.name + method.desc));
            trace.add(new VarInsnNode(ALOAD, keyLocal));
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "logKeyEvent",
                    "(Ljava/lang/String;Ljava/lang/Object;)V", false));
            method.instructions.insert(trace);
            method.maxStack = Math.max(method.maxStack, 2);
            changed++;
        }

        if (changed > 0) installClassLoadMarker(node);
        return changed;
    }

    private static void installClassLoadMarker(ClassNode node) {
        if (hasClassLoadMarker(node)) return;

        MethodNode initializer = null;
        for (MethodNode method : node.methods) {
            if ("<clinit>".equals(method.name) && "()V".equals(method.desc)) {
                initializer = method;
                break;
            }
        }
        if (initializer == null) {
            initializer = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
            initializer.instructions.add(new InsnNode(RETURN));
            node.methods.add(initializer);
        }

        InsnList marker = new InsnList();
        marker.add(new LdcInsnNode(HANDLER.replace('/', '.')));
        marker.add(new MethodInsnNode(INVOKESTATIC, HELPER, "logInputTraceInstalled", "(Ljava/lang/String;)V", false));
        initializer.instructions.insert(marker);
        initializer.maxStack = Math.max(initializer.maxStack, 1);
    }

    private static boolean hasClassLoadMarker(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!"<clinit>".equals(method.name) || !"()V".equals(method.desc) || method.instructions == null) continue;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKESTATIC
                        && HELPER.equals(call.owner)
                        && "logInputTraceInstalled".equals(call.name)) return true;
            }
        }
        return false;
    }

    private static boolean hasTrace(MethodNode method) {
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode methodCall
                    && methodCall.getOpcode() == INVOKESTATIC
                    && HELPER.equals(methodCall.owner)
                    && "logKeyEvent".equals(methodCall.name)) return true;
        }
        return false;
    }
}
