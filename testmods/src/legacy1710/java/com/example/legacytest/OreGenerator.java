package com.example.legacytest;

import cpw.mods.fml.common.IWorldGenerator;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

/** Classic 1.7.10 ore generator: random single blocks replacing stone, overworld only. */
public class OreGenerator implements IWorldGenerator {
    public static int placed;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator, IChunkProvider provider) {
        if (world.provider.dimensionId != 0) return;
        for (int i = 0; i < LegacyTestMod.oreVeinsPerChunk; i++) {
            int x = chunkX * 16 + random.nextInt(16);
            int y = 5 + random.nextInt(60);
            int z = chunkZ * 16 + random.nextInt(16);
            if (world.getBlock(x, y, z) == Blocks.stone) {
                world.setBlock(x, y, z, LegacyTestMod.ore, random.nextInt(2), 2);
                placed++;
            }
        }
    }
}
