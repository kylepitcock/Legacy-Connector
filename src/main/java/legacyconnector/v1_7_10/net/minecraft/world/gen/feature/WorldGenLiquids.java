package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A spring: a liquid source set into a stone wall with exactly one open side (vanilla's water and lava springs). */
public class WorldGenLiquids extends WorldGenerator {
    private Block field_150521_a;
    /** The block of a 1.6.4 id constructor, looked up when generating (1.6.4 read {@code blocksList[id]} then). */
    private java.util.function.Supplier<Block> mcc$byId;

    public WorldGenLiquids(Block liquid) {
        this.field_150521_a = liquid;
    }

    /** 1.6.4: {@code new WorldGenLiquids(blockId)}. */
    public WorldGenLiquids(int blockId) {
        this.mcc$byId = legacyconnector.v1_6_4.net.minecraft.util.Ids.lazyBlock(blockId);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (field_150521_a == null && mcc$byId != null) field_150521_a = mcc$byId.get();
        if (field_150521_a == null) return false;
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
