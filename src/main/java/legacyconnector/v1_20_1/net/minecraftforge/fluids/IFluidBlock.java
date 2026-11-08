package legacyconnector.v1_20_1.net.minecraftforge.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Forge 1.20.1's {@code IFluidBlock} (removed in NeoForge 1.21.1): implemented by a mod's own fluid blocks. */
public interface IFluidBlock {
    Fluid getFluid();

    int place(Level level, BlockPos pos, FluidStack fluidStack, IFluidHandler.FluidAction action);

    FluidStack drain(Level level, BlockPos pos, IFluidHandler.FluidAction action);

    boolean canDrain(Level level, BlockPos pos);

    float getFilledPercentage(Level level, BlockPos pos);
}
