package org.kylepitcock.legacyconnector.loader.transform.compat;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Redirects AlienEvo's legacy particle parser to a client-registry bridge.
 * A descriptor-only change is unsafe: its second argument is a registry-specific
 * HolderLookup, not the 1.21.1 HolderLookup.Provider required by ParticleArgument.
 */
public final class AlienEvoBeamAbilityParticles1201 implements Opcodes {
    public static final String TARGET = "net/habb/alienevo/ability/BeamAbility";
    public static final String PARTICLE_ARGUMENT = "net/minecraft/commands/arguments/ParticleArgument";
    public static final String METHOD = "readParticle";
    public static final String BRIDGE_OWNER =
            "org/kylepitcock/legacyconnector/v1_20_1/compat/AlienEvoClientCompat";
    public static final String BRIDGE_NAME = "readParticle";
    public static final String LEGACY_DESC =
            "(Lcom/mojang/brigadier/StringReader;Lnet/minecraft/core/HolderLookup;)Lnet/minecraft/core/particles/ParticleOptions;";

    private AlienEvoBeamAbilityParticles1201() {}

    public static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        int legacyCalls = 0;
        int bridgeCalls = 0;
        MethodInsnNode legacyCall = null;
        for (var method : node.methods) {
            if (!"parseParticleSpec".equals(method.name) || method.instructions == null) continue;
            for (var instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                boolean particleCall = PARTICLE_ARGUMENT.equals(call.owner) && METHOD.equals(call.name);
                boolean bridgeCall = BRIDGE_OWNER.equals(call.owner) && BRIDGE_NAME.equals(call.name);
                if (!particleCall && !bridgeCall) continue;
                if (call.getOpcode() != INVOKESTATIC || call.itf) {
                    throw new IllegalStateException("AlienEvo BeamAbility particle parser call changed invocation kind");
                }
                if (!LEGACY_DESC.equals(call.desc)) {
                    throw new IllegalStateException(
                            "AlienEvo BeamAbility particle parser has an unexpected descriptor: " + call.desc);
                }
                if (particleCall) {
                    legacyCalls++;
                    legacyCall = call;
                } else {
                    bridgeCalls++;
                }
            }
        }

        if (legacyCalls == 0 && bridgeCalls == 1) return 0;
        if (legacyCalls != 1 || bridgeCalls != 0) {
            throw new IllegalStateException(
                    "AlienEvo BeamAbility.parseParticleSpec expected exactly one legacy ParticleArgument.readParticle call; found "
                            + legacyCalls + " legacy and " + bridgeCalls + " bridged");
        }

        legacyCall.owner = BRIDGE_OWNER;
        legacyCall.name = BRIDGE_NAME;
        // Keep LEGACY_DESC unchanged. The bridge converts the old registry object
        // into the current level's full RegistryAccess / HolderLookup.Provider.
        return 1;
    }
}
