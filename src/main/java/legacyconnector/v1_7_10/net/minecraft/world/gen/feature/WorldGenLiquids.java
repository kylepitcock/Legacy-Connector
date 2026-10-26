package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A spring: a liquid source set into a stone wall with exactly one open side (vanilla's water and lava springs). */
public class WorldGenLiquids extends WorldGenerator {
    private final Block field_150521_a;

    public WorldGenLiquids(Block liquid) {
        this.field_150521_a = liquid;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (world.getBlock(x, y + 1, z) != Blocks.stone) return false;
        if (world.getBlock(x, y - 1, z) != Blocks.stone) return false;
        Block here = world.getBlock(x, y, z);
        if (here.getMaterial() != Material.air && here != Blocks.stone) return false;
        int stone = 0;
        if (world.getBlock(x - 1, y, z) == Blocks.stone) stone++;
        if (world.getBlock(x + 1, y, z) == Blocks.stone) stone++;
        if (world.getBlock(x, y, z - 1) == Blocks.stone) stone++;
        if (world.getBlock(x, y, z + 1) == Blocks.stone) stone++;
        int air = 0;
        if (world.isAirBlock(x - 1, y, z)) air++;
        if (world.isAirBlock(x + 1, y, z)) air++;
        if (world.isAirBlock(x, y, z - 1)) air++;
        if (world.isAirBlock(x, y, z + 1)) air++;
        if (stone == 3 && air == 1) LegacyFeatures.spring(world, field_150521_a, x, y, z, random);
        return true;
    }
}
