package org.kylepitcock.mccompat.core.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

/** Hook for the overworld biome source (installed by a legacy family that places legacy biomes). */
public final class BiomeOverride {
    public interface Provider {
        /** Replacement for {@code modern} at quart position (x, y, z) of {@code source}, or null to keep it. */
        Holder<Biome> apply(BiomeSource source, Holder<Biome> modern, int x, int y, int z);
    }

    private static volatile Provider provider;
    /** Biomes the overworld may contain besides its preset's (legacy biomes): their features must be scheduled. */
    private static volatile java.util.function.Supplier<java.util.List<Holder<Biome>>> extraBiomes = java.util.List::of;

    public static void setExtraBiomes(java.util.function.Supplier<java.util.List<Holder<Biome>>> supplier) {
        extraBiomes = supplier;
    }

    public static java.util.List<Holder<Biome>> extraBiomes() {
        try {
            return extraBiomes.get();
        } catch (RuntimeException e) {
            return java.util.List.of();
        }
    }

    private BiomeOverride() {}

    public static void set(Provider p) {
        provider = p;
    }

    public static Holder<Biome> apply(BiomeSource source, Holder<Biome> modern, int x, int y, int z) {
        Provider p = provider;
        if (p == null) return null;
        try {
            return p.apply(source, modern, x, y, z);
        } catch (RuntimeException | LinkageError e) {
            // Never break chunk generation: fall back to modern biomes for the rest of the session.
            provider = null;
            org.slf4j.LoggerFactory.getLogger("Retrofit/Biomes").error("Legacy biome layers failed; using modern biomes only", e);
            return null;
        }
    }
}
