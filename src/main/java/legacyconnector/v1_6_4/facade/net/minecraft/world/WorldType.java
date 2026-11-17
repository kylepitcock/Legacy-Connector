package legacyconnector.v1_6_4.facade.net.minecraft.world;

/** Facade of the 1.7.10 shim's {@code WorldType} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class WorldType {
    private WorldType() {}

    /** 1.6.4 {@code biomesForWorldType}: the biomes the land layers draw from (a snapshot of the Forge biome lists of every climate). */
    public static legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase[] mcc$get$biomesForWorldType(legacyconnector.v1_7_10.net.minecraft.world.WorldType self) {
        java.util.LinkedHashSet<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase> out = new java.util.LinkedHashSet<>();
        for (legacyconnector.v1_7_10.net.minecraftforge.common.BiomeManager.BiomeType type : legacyconnector.v1_7_10.net.minecraftforge.common.BiomeManager.BiomeType.values()) {
            for (legacyconnector.v1_7_10.net.minecraftforge.common.BiomeManager.BiomeEntry e : legacyconnector.v1_7_10.net.minecraftforge.common.BiomeManager.getBiomes(type)) out.add(e.biome);
        }
        return out.toArray(new legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase[0]);
    }
}
