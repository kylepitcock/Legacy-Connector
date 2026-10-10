package mccompat.v1_7_10.cpw.mods.fml.common;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

public interface IWorldGenerator {
    void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider);
}
