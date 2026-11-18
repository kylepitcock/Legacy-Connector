package legacyconnector.v1_6_4.facade.net.minecraft.world.biome;

/** Facade of the 1.7.10 shim's {@code WorldChunkManager} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class WorldChunkManager {
    private WorldChunkManager() {}

    /**
     * 1.6.4 {@code getTemperatures(out, x, z, width, depth)}: the temperature of each column of the area (the biome's, as 1.6.4 read it: the integer
     * temperature over 65536, at most 1.0), written into {@code out} when it is big enough, else into a new array.
     */
    public static float[] getTemperatures(legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManager self, float[] out, int x, int z, int width, int depth) {
        if (out == null || out.length < width * depth) out = new float[width * depth];
        legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase[] biomes = self.getBiomesForGeneration(null, x, z, width, depth);
        for (int i = 0; i < width * depth; i++) {
            float temperature = biomes[i].getIntTemperature() / 65536.0F;
            out[i] = temperature > 1.0F ? 1.0F : temperature;
        }
        return out;
    }
}
