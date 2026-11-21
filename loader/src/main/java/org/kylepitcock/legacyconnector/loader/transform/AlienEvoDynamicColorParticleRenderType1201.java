package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Migrates AlienEvo 1.5.0-dev.24's DynamicColorParticleRenderType anonymous class from
 * the 1.20.1 ParticleRenderType.begin(BufferBuilder, TextureManager): void contract
 * to the 1.21.1 begin(Tesselator, TextureManager): BufferBuilder contract.
 */
final class AlienEvoDynamicColorParticleRenderType1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/client/particle/DynamicColorParticleRenderType$1";
    static final String COMPAT_BUILDER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/CompatBufferBuilder";
    static final String TESSELATOR = "com/mojang/blaze3d/vertex/Tesselator";
    static final String BUFFER_BUILDER = "com/mojang/blaze3d/vertex/BufferBuilder";
    static final String TEXTURE_MANAGER = "net/minecraft/client/renderer/texture/TextureManager";
    static final String VERTEX_MODE = "com/mojang/blaze3d/vertex/VertexFormat$Mode";
    static final String VERTEX_FORMAT = "com/mojang/blaze3d/vertex/VertexFormat";
    static final String LEGACY_BEGIN_DESC = "(L" + COMPAT_BUILDER + ";L" + TEXTURE_MANAGER + ";)V";
    static final String MODERN_BEGIN_DESC = "(L" + TESSELATOR + ";L" + TEXTURE_MANAGER + ";)L" + BUFFER_BUILDER + ";";
    static final String COMPAT_BUILDER_BEGIN_DESC = "(L" + VERTEX_MODE + ";L" + VERTEX_FORMAT + ";)V";
    static final String TESSELATOR_BEGIN_DESC = "(L" + VERTEX_MODE + ";L" + VERTEX_FORMAT + ";)L" + BUFFER_BUILDER + ";";

    private AlienEvoDynamicColorParticleRenderType1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode legacy = null;
        MethodNode modern = null;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("begin")) continue;
            if (method.desc.equals(LEGACY_BEGIN_DESC)) legacy = unique(legacy, method);
            else if (method.desc.equals(MODERN_BEGIN_DESC)) modern = unique(modern, method);
        }

        if (modern != null && legacy == null) {
            validateModern(modern);
            return 0;
        }
        if (legacy == null || modern != null) {
            throw new IllegalStateException("AlienEvo DynamicColorParticleRenderType has an unknown begin method layout");
        }

        MethodInsnNode legacyBegin = null;
        int legacyCalls = 0;
        int modernCalls = 0;
        int returnCount = 0;
        AbstractInsnNode returnInstruction = null;
        for (AbstractInsnNode instruction : legacy.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals(COMPAT_BUILDER) && call.name.equals("begin")) {
                    if (call.getOpcode() != INVOKEVIRTUAL || !call.desc.equals(COMPAT_BUILDER_BEGIN_DESC)) {
                        throw new IllegalStateException("AlienEvo DynamicColorParticleRenderType CompatBufferBuilder.begin descriptor changed");
                    }
                    legacyCalls++;
                    legacyBegin = call;
                } else if (call.owner.equals(TESSELATOR) && call.name.equals("begin")
                        && call.getOpcode() == INVOKEVIRTUAL && call.desc.equals(TESSELATOR_BEGIN_DESC)) {
                    modernCalls++;
                }
            }
            if (instruction.getOpcode() == RETURN) {
                returnCount++;
                returnInstruction = instruction;
            }
        }

        if (legacyCalls != 1 || modernCalls != 0 || returnCount != 1) {
            throw new IllegalStateException("AlienEvo DynamicColorParticleRenderType expected one CompatBufferBuilder.begin call and one void return; found "
                    + legacyCalls + " legacy call(s), " + modernCalls + " modern call(s), and " + returnCount + " return(s)");
        }
        AbstractInsnNode next = legacyBegin.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        if (next != returnInstruction) {
            throw new IllegalStateException("AlienEvo DynamicColorParticleRenderType begin no longer returns immediately after starting the tessellator");
        }

        // The old receiver local was a CompatBufferBuilder. The new interface passes
        // the Tesselator directly, and its begin call returns the BufferBuilder to the engine.
        legacyBegin.owner = TESSELATOR;
        legacyBegin.name = "begin";
        legacyBegin.desc = TESSELATOR_BEGIN_DESC;
        legacyBegin.itf = false;
        legacy.desc = MODERN_BEGIN_DESC;
        legacy.instructions.set(returnInstruction, new InsnNode(ARETURN));
        return 1;
    }

    private static MethodNode unique(MethodNode existing, MethodNode candidate) {
        if (existing != null) throw new IllegalStateException("AlienEvo DynamicColorParticleRenderType declares duplicate begin methods");
        return candidate;
    }

    private static void validateModern(MethodNode method) {
        int calls = 0;
        int returns = 0;
        AbstractInsnNode returnInstruction = null;
        MethodInsnNode beginCall = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(TESSELATOR)
                    && call.name.equals("begin") && call.getOpcode() == INVOKEVIRTUAL
                    && call.desc.equals(TESSELATOR_BEGIN_DESC)) {
                calls++;
                beginCall = call;
            }
            if (instruction.getOpcode() == ARETURN) {
                returns++;
                returnInstruction = instruction;
            }
        }
        AbstractInsnNode next = beginCall == null ? null : beginCall.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        if (calls != 1 || returns != 1 || next != returnInstruction) {
            throw new IllegalStateException("AlienEvo DynamicColorParticleRenderType modern begin method is not in the expected migrated shape");
        }
    }
}
