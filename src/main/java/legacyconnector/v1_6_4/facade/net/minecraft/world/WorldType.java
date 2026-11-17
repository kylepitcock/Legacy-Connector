package legacyconnector.v1_6_4.facade.net.minecraft.world;

import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraftforge.common.BiomeManager;

/** Facade of the 1.7.10 shim's {@code WorldType} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class WorldType {
    private WorldType() {}

    private static final java.util.Map<legacyconnector.v1_7_10.net.minecraft.world.WorldType, BiomeGenBase[]> ASSIGNED =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static java.util.Set<BiomeGenBase> forgeLists() {
        java.util.LinkedHashSet<BiomeGenBase> out = new java.util.LinkedHashSet<>();
        for (BiomeManager.BiomeType type : BiomeManager.BiomeType.values()) {
            for (BiomeManager.BiomeEntry e : BiomeManager.getBiomes(type)) out.add(e.biome);
        }
        return out;
    }

    /**
     * 1.6.4 {@code biomesForWorldType = biomes}: mod world types assign the biomes their generator draws from. The array is kept for reads, and
     * every biome the Forge lists lack joins them (as {@code addNewBiome}), which is where the pack's biome placement reads.
     */
    public static void mcc$set$biomesForWorldType(legacyconnector.v1_7_10.net.minecraft.world.WorldType self, BiomeGenBase[] biomes) {
        java.util.Set<BiomeGenBase> known = forgeLists();
        ASSIGNED.put(self, biomes);
        if (biomes == null) return;
        for (BiomeGenBase b : biomes) {
            if (b != null && known.add(b)) self.addNewBiome(b);
        }
    }

    /** 1.6.4 {@code biomesForWorldType}: the biomes the land layers draw from (what the world type was given, else the Forge biome lists of every climate). */
    public static BiomeGenBase[] mcc$get$biomesForWorldType(legacyconnector.v1_7_10.net.minecraft.world.WorldType self) {
        BiomeGenBase[] assigned = ASSIGNED.get(self);
        return assigned != null ? assigned : forgeLists().toArray(new BiomeGenBase[0]);
    }
}
