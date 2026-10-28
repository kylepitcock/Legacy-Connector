package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * Forge: a block that a full bucket of fluid places (a fluid's block that is not an {@link legacyconnector.v1_12_2.net.minecraftforge.fluids.IFluidBlock}):
 * filling with at least a bucket puts the block's default state at the position. It cannot be drained.
 */
public class BlockWrapper implements IFluidHandler {
    protected final Block block;
    protected final World world;
    protected final BlockPos blockPos;

    public BlockWrapper(Block block, World world, BlockPos blockPos) {
        this.block = block;
        this.world = world;
        this.blockPos = blockPos;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new FluidTankProperties[] {new FluidTankProperties(null, Fluid.BUCKET_VOLUME, true, false)};
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount < Fluid.BUCKET_VOLUME) return 0;
        if (doFill) world.setBlockState(blockPos, block.getDefaultState(), 11);
        return Fluid.BUCKET_VOLUME;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return null;
    }

    /** The legacy fluid a modern fluid stands for (water, lava, or a fluid of the same name). */
    public static Fluid mcc$legacy(net.minecraft.world.level.material.Fluid modern) {
        if (modern == null) return null;
        if (modern.isSame(net.minecraft.world.level.material.Fluids.WATER)) return FluidRegistry.WATER;
        if (modern.isSame(net.minecraft.world.level.material.Fluids.LAVA)) return FluidRegistry.LAVA;
        var key = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(modern);
        return key == null ? null : FluidRegistry.getFluid(key.getPath());
    }

    /** A source block of a game fluid (water, lava, a modern mod's fluid) as a fluid handler: a bucket's worth to drain, picked up as a bucket would. */
    public static final class ModernLiquid implements IFluidHandler {
        private final net.minecraft.world.level.Level level;
        private final net.minecraft.core.BlockPos pos;

        public ModernLiquid(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        private FluidStack contents() {
            var state = level.getFluidState(pos);
            if (state.isEmpty() || !state.isSource()) return null;
            Fluid fluid = mcc$legacy(state.getType());
            return fluid == null ? null : new FluidStack(fluid, Fluid.BUCKET_VOLUME);
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new FluidTankProperties[] {new FluidTankProperties(contents(), Fluid.BUCKET_VOLUME, false, true)};
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            FluidStack contents = contents();
            if (resource == null || contents == null || !contents.isFluidEqual(resource) || resource.amount < contents.amount) return null;
            return drain(contents.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidStack contents = contents();
            if (contents == null || maxDrain < contents.amount) return null;
            if (doDrain) {
                var state = level.getBlockState(pos);
                if (state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup) {
                    if (pickup.pickupBlock(null, level, pos, state).isEmpty()) return null;
                } else {
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
                }
            }
            return contents;
        }
    }
}
