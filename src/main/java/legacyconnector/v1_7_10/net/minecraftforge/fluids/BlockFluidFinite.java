package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Forge 1.7.10 fluid with a conserved amount: it falls whole, and levels out between neighbours. */
public class BlockFluidFinite extends BlockFluidBase {
    public BlockFluidFinite(Fluid fluid, Material material) {
        super(fluid, material);
    }

    @Override
    public int getQuantaValue(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block == Blocks.air || block.isAir(world, x, y, z)) return 0;
        if (block != this) return -1;
        return world.getBlockMetadata(x, y, z) + 1;
    }

    @Override
    public boolean canCollideCheck(int meta, boolean fullHit) {
        return fullHit;
    }

    @Override
    public int getMaxRenderHeightMeta(IBlockAccess world, int x, int y, int z) {
        return quantaPerBlock - 1;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand) {
        boolean changed = false;
        int quanta = world.getBlockMetadata(x, y, z) + 1;

        // Flow below first
        Block below = world.getBlock(x, y + densityDir, z);
        if (below == this || canDisplace(world, x, y + densityDir, z)) {
            int belowQuanta = below == this ? world.getBlockMetadata(x, y + densityDir, z) + 1 : 0;
            int moved = Math.min(quantaPerBlock - belowQuanta, quanta);
            if (moved > 0) {
                if (below != this) displaceIfPossible(world, x, y + densityDir, z);
                world.setBlock(x, y + densityDir, z, this, belowQuanta + moved - 1, 3);
                quanta -= moved;
                changed = true;
                if (quanta <= 0) {
                    world.setBlockToAir(x, y, z);
                    return;
                }
                world.setBlockMetadataWithNotify(x, y, z, quanta - 1, 3);
            }
        }

        // Level out with the four neighbours
        if (quanta > 1) {
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] d : dirs) {
                int nx = x + d[0], nz = z + d[1];
                Block b = world.getBlock(nx, y, nz);
                int other;
                if (b == this) other = world.getBlockMetadata(nx, y, nz) + 1;
                else if (canDisplace(world, nx, y, nz)) other = 0;
                else continue;
                if (quanta - other >= 2) {
                    if (b != this) displaceIfPossible(world, nx, y, nz);
                    world.setBlock(nx, y, nz, this, other, 3);
                    quanta--;
                    world.setBlockMetadataWithNotify(x, y, z, quanta - 1, 3);
                    changed = true;
                    if (quanta <= 1) break;
                }
            }
        }
        if (changed) world.scheduleBlockUpdate(x, y, z, this, tickRate);
    }

    @Override
    public FluidStack drain(World world, int x, int y, int z, boolean doDrain) {
        Fluid fluid = getFluid();
        if (fluid == null) return null;
        int quanta = world.getBlockMetadata(x, y, z) + 1;
        if (quanta < quantaPerBlock) return null;
        if (doDrain) world.setBlockToAir(x, y, z);
        return new FluidStack(fluid, FluidContainerRegistry.BUCKET_VOLUME);
    }

    @Override
    public boolean canDrain(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) == this && world.getBlockMetadata(x, y, z) + 1 >= quantaPerBlock;
    }

    @Override
    public float getFilledPercentage(World world, int x, int y, int z) {
        int quantaRemaining = getQuantaValue(world, x, y, z);
        return quantaRemaining <= 0 ? 0 : quantaRemaining / quantaPerBlockFloat * (density > 0 ? 1 : -1);
    }
}
