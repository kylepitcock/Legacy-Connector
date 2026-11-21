package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Migrates AlienEvo LateEffectBuffers' one legacy RenderType.end call to the 1.21.1 draw path. */
final class AlienEvoLateEffectBuffers1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/client/renderer/LateEffectBuffers";
    static final String RENDER_TYPE = "net/minecraft/client/renderer/RenderType";
    static final String BUILDER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/CompatBufferBuilder";
    static final String VERTEX_SORTING = "com/mojang/blaze3d/vertex/VertexSorting";
    static final String LEGACY_DESC = "(L" + BUILDER + ";L" + VERTEX_SORTING + ";)V";
    static final String DRAW_NAME = "drawWithRenderType";
    static final String DRAW_DESC = "(L" + RENDER_TYPE + ";L" + BUILDER + ";L" + VERTEX_SORTING + ";)V";

    private AlienEvoLateEffectBuffers1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode flush = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("flush") && method.desc.equals("()V")) {
                if (flush != null) throw new IllegalStateException("AlienEvo LateEffectBuffers has duplicate flush() methods");
                flush = method;
            }
        }
        if (flush == null) throw new IllegalStateException("AlienEvo LateEffectBuffers no longer declares flush()V");

        int legacyCalls = 0;
        int migratedCalls = 0;
        MethodInsnNode legacy = null;
        for (AbstractInsnNode instruction : flush.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call) || !call.owner.equals(RENDER_TYPE)) continue;
            if (call.name.equals("end")) {
                legacyCalls++;
                if (call.getOpcode() != INVOKEVIRTUAL || !call.desc.equals(LEGACY_DESC)) {
                    throw new IllegalStateException("AlienEvo LateEffectBuffers RenderType.end call has an unknown descriptor");
                }
                legacy = call;
            } else if (call.name.equals(DRAW_NAME) && call.getOpcode() == INVOKESTATIC
                    && call.owner.equals(BUILDER) && call.desc.equals(DRAW_DESC)) {
                migratedCalls++;
            }
        }

        if (legacyCalls == 0 && migratedCalls == 1) return 0;
        if (legacyCalls != 1 || migratedCalls != 0) {
            throw new IllegalStateException("AlienEvo LateEffectBuffers expected exactly one unmigrated RenderType.end call; found "
                    + legacyCalls + " legacy and " + migratedCalls + " migrated");
        }

        // The existing operand stack is [RenderType, CompatBufferBuilder, VertexSorting].
        // The replacement static helper consumes precisely those same three values.
        flush.instructions.set(legacy, new MethodInsnNode(INVOKESTATIC, BUILDER, DRAW_NAME, DRAW_DESC, false));
        return 1;
    }
}
