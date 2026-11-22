package org.kylepitcock.legacyconnector.v1_20_1.compat;

import net.minecraft.client.Minecraft;

/** Client-side adapters for AlienEvo 1.20.1 bytecode (called from the rewrites of AlienEvoBeamAbilityParticles1201). */
public final class AlienEvoClientCompat {
    private AlienEvoClientCompat() {
    }

    /**
     * Adapter for AlienEvo 1.20.1 bytecode. Its HolderLookup argument is registry-specific;
     * Minecraft 1.21.1 requires a full HolderLookup.Provider, supplied by the active ClientLevel.
     */
    public static net.minecraft.core.particles.ParticleOptions readParticle(
            com.mojang.brigadier.StringReader reader, net.minecraft.core.HolderLookup<?> ignoredLookup)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            throw new IllegalStateException(
                    "AlienEvo attempted to parse a beam particle without an active client level");
        }
        return net.minecraft.commands.arguments.ParticleArgument.readParticle(reader, level.registryAccess());
    }

}
