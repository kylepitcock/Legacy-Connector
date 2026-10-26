package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A nether lava spring: a source under netherrack with one open side (or, {@code insideRock}, sealed in netherrack on all five
 * sides, the hidden lava pockets).
 */
public class WorldGenHellLava extends WorldGenerator {
    private final Block field_150553_a;
    private final boolean field_94524_b;

    public WorldGenHellLava(Block liquid, boolean insideRock) {
        this.field_150553_a = liquid;
        this.field_94524_b = insideRock;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (world.getBlock(x, y + 1, z) != Blocks.netherrack) return false;
        Block here = world.getBlock(x, y, z);
        if (here.getMaterial() != Material.air && here != Blocks.netherrack) return false;
        int rock = 0;
        if (world.getBlock(x - 1, y, z) == Blocks.netherrack) rock++;
        if (world.getBlock(x + 1, y, z) == Blocks.netherrack) rock++;
        if (world.getBlock(x, y, z - 1) == Blocks.netherrack) rock++;
        if (world.getBlock(x, y, z + 1) == Blocks.netherrack) rock++;
        if (world.getBlock(x, y - 1, z) == Blocks.netherrack) rock++;
        int air = 0;
        if (world.isAirBlock(x - 1, y, z)) air++;
        if (world.isAirBlock(x + 1, y, z)) air++;
        if (world.isAirBlock(x, y, z - 1)) air++;
        if (world.isAirBlock(x, y, z + 1)) air++;
        if (world.isAirBlock(x, y - 1, z)) air++;
        if (!field_94524_b && rock == 4 && air == 1 || rock == 5) LegacyFeatures.spring(world, field_150553_a, x, y, z, random);
        return true;
    }
}
