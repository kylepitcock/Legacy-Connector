package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Migrates AlienEvo 1.5.0-dev.24's ParticleArgument.readParticle call from
 * the old HolderLookup parameter to Minecraft 1.21.1's HolderLookup.Provider.
 */
final class AlienEvoBeamAbilityParticles1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/ability/BeamAbility";
    static final String PARTICLE_ARGUMENT = "net/minecraft/commands/arguments/ParticleArgument";
    static final String METHOD = "readParticle";
    static final String LEGACY_DESC =
            "(Lcom/mojang/brigadier/StringReader;Lnet/minecraft/core/HolderLookup;)Lnet/minecraft/core/particles/ParticleOptions;";
    static final String MODERN_DESC =
            "(Lcom/mojang/brigadier/StringReader;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/core/particles/ParticleOptions;";

    private AlienEvoBeamAbilityParticles1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        int legacyCalls = 0;
        int modernCalls = 0;
        MethodInsnNode legacyCall = null;
        for (var method : node.methods) {
            if (!"parseParticleSpec".equals(method.name) || method.instructions == null) continue;
            for (var instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !PARTICLE_ARGUMENT.equals(call.owner)
                        || !METHOD.equals(call.name)) {
                    continue;
                }
                if (call.getOpcode() != INVOKESTATIC || call.itf) {
                    throw new IllegalStateException(
                            "AlienEvo BeamAbility ParticleArgument.readParticle call changed invocation kind");
                }
                if (LEGACY_DESC.equals(call.desc)) {
                    legacyCalls++;
                    legacyCall = call;
                } else if (MODERN_DESC.equals(call.desc)) {
                    modernCalls++;
                } else {
                    throw new IllegalStateException(
                            "AlienEvo BeamAbility ParticleArgument.readParticle has an unknown descriptor: " + call.desc);
                }
            }
        }

        if (legacyCalls == 0 && modernCalls == 1) return 0;
        if (legacyCalls != 1 || modernCalls != 0) {
            throw new IllegalStateException(
                    "AlienEvo BeamAbility.parseParticleSpec expected exactly one legacy ParticleArgument.readParticle call; found "
                            + legacyCalls + " legacy and " + modernCalls + " migrated");
        }

        legacyCall.desc = MODERN_DESC;
        return 1;
    }
}
