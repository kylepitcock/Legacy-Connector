package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Forge: a block that is a placed fluid (it can be drained into a container). */
public interface IFluidBlock {
    Fluid getFluid();

    /** Places the fluid at a position; the amount placed. Forge 14.23 added it; older fluid blocks cannot be placed this way. */
    default int place(World world, BlockPos pos, FluidStack fluidStack, boolean doPlace) {
        return 0;
    }

    /** Removes the fluid from the block when doDrain; what it held (null when nothing can be drained). */
    FluidStack drain(World world, BlockPos pos, boolean doDrain);

    boolean canDrain(World world, BlockPos pos);

    /** How full the block is: 0..1, negative for gases. */
    float getFilledPercentage(World world, BlockPos pos);
}
