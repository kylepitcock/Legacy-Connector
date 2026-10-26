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
    /**
     * Whether a mob of the type could spawn at the block: water creatures in water two deep, the others on a solid top that lets the
     * type spawn (not bedrock) with two blocks of non-solid, dry room.
     */
    public static boolean canCreatureTypeSpawnAtLocation(legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type, World world, int x, int y, int z) {
        if (type == null || world == null) return false;
        if (type == legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType.waterCreature) {
            return world.getBlock(x, y, z).getMaterial().isLiquid() && world.getBlock(x, y - 1, z).getMaterial().isLiquid()
                    && !world.getBlock(x, y + 1, z).isNormalCube();
        }
        if (!World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)) return false;
        var below = world.getBlock(x, y - 1, z);
        if (!below.canCreatureSpawn(type, world, x, y - 1, z) || below == legacyconnector.v1_7_10.net.minecraft.init.Blocks.bedrock) return false;
        var here = world.getBlock(x, y, z);
        return !here.isNormalCube() && !here.getMaterial().isLiquid() && !world.getBlock(x, y + 1, z).isNormalCube();
    }

    public static void performWorldGenSpawning(World world, BiomeGenBase biome, int x, int z, int width, int height, Random random) {
    }
}
