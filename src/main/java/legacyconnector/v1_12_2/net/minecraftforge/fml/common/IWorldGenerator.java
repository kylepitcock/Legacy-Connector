package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.IChunkProvider;
import legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator;

/**
 * A mod's chunk populator, run once per chunk as it is decorated (see {@code WorldGenBridge1122}); generators are kept by
 * {@code GameRegistry} in weight order.
 */
public interface IWorldGenerator {
    void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider);
}
