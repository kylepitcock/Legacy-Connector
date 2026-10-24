package legacyconnector.v1_7_10.net.minecraft.world;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Mob spawning. Legacy world generators call {@link #performWorldGenSpawning} to place a chunk's initial animals. */
public final class SpawnerAnimals {
    private SpawnerAnimals() {}

    /**
     * Places the initial animals of a freshly generated area. The modern world generator spawns the passive mobs of a chunk's
     * biome itself, and entities cannot be added to a world that is still being generated, so this does nothing.
     */
    public static void performWorldGenSpawning(World world, BiomeGenBase biome, int x, int z, int width, int height, Random random) {
    }
}
