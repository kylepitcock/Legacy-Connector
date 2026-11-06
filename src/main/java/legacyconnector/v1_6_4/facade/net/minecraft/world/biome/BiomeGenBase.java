package legacyconnector.v1_6_4.facade.net.minecraft.world.biome;

/**
 * Facade of the 1.7.10 shim's {@code BiomeGenBase} for 1.6.4 mods: the biome colours had no position in 1.6.4
 * ({@code getBiomeGrassColor()}, {@code getBiomeFoliageColor()}); 1.7.10 asks per block position, which a subclass may override.
 * See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class BiomeGenBase {
    private BiomeGenBase() {}

    public static int getBiomeGrassColor(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self) {
        return self.getBiomeGrassColor(0, 64, 0);
    }

    public static int getBiomeFoliageColor(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self) {
        return self.getBiomeFoliageColor(0, 64, 0);
    }
}
