package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Implemented by blocks that hold a fluid in the world (and can be drained by pumps and buckets). */
public interface IFluidBlock {
    Fluid getFluid();

    FluidStack drain(World world, int x, int y, int z, boolean doDrain);

    boolean canDrain(World world, int x, int y, int z);

    float getFilledPercentage(World world, int x, int y, int z);
}
