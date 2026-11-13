package legacyconnector.vb1_7_3.net.minecraft.block;

import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta's {@code BlockStationary}: a still fluid, which turns back into the flowing block (the id below it) when a neighbour changes. */
public class StillLiquidBlock extends LiquidBlock {
    protected StillLiquidBlock(int id, Material material) {
        super(id, material);
    }

    @Override public void neighborUpdate(World world, int x, int y, int z, int neighborId) {
        super.neighborUpdate(world, x, y, z, neighborId);
        if (world.getBlockId(x, y, z) == id) convertToFlowing(world, x, y, z);
    }

    private void convertToFlowing(World world, int x, int y, int z) {
        int meta = world.getBlockMeta(x, y, z);
        world.setBlockWithoutNotifyingNeighbors(x, y, z, id - 1, meta);
        world.scheduleBlockUpdate(x, y, z, id - 1, getTickRate());
    }

    @Override public void onTick(World world, int x, int y, int z, Random random) {}
}
