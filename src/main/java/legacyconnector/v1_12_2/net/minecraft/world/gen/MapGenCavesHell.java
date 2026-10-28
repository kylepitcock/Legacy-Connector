package legacyconnector.v1_12_2.net.minecraft.world.gen;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;

/**
 * 1.12.2 nether cave carver: fewer, wider tunnels than the overworld's, cut through netherrack (and the dirt/grass a mod's nether
 * may use) and kept away from lava; carved blocks become air.
 */
public class MapGenCavesHell extends MapGenCaves {
    @Override
    protected void recursiveGenerate(World worldIn, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer chunkPrimerIn) {
        int count = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(10) + 1) + 1);
        if (this.rand.nextInt(5) != 0) count = 0;
        for (int j = 0; j < count; ++j) {
            double x = chunkX * 16 + this.rand.nextInt(16);
            double y = this.rand.nextInt(128);
            double z = chunkZ * 16 + this.rand.nextInt(16);
            int tunnels = 1;
            if (this.rand.nextInt(4) == 0) {
                this.addRoom(this.rand.nextLong(), originalX, originalZ, chunkPrimerIn, x, y, z);
                tunnels += this.rand.nextInt(4);
            }
            for (int l = 0; l < tunnels; ++l) {
                float yaw = this.rand.nextFloat() * ((float) Math.PI * 2F);
                float pitch = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float width = this.rand.nextFloat() * 2.0F + this.rand.nextFloat();
                this.addTunnel(this.rand.nextLong(), originalX, originalZ, chunkPrimerIn, x, y, z, width * 2.0F, yaw, pitch, 0, 0, 0.5D);
            }
        }
    }

    @Override
    protected boolean canReplaceBlock(IBlockState state, IBlockState above) {
        var m = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state);
        return m.is(net.minecraft.world.level.block.Blocks.NETHERRACK) || m.is(net.minecraft.world.level.block.Blocks.DIRT)
                || m.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
    }

    @Override
    protected boolean isOceanBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
        var m = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(data.getBlockState(x, y, z));
        return m.getFluidState().is(net.minecraft.tags.FluidTags.LAVA);
    }

    @Override
    protected void digBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop, IBlockState state, IBlockState up) {
        if (canReplaceBlock(state, up)) data.setBlockState(x, y, z, BLK_AIR);
    }
}
