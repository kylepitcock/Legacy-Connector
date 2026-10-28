package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Forge's fluid events, posted by tanks when fluid moves into, out of or spills from them. */
public class FluidEvent extends Event {
    private final FluidStack fluid;
    private final World world;
    private final BlockPos pos;

    public FluidEvent(FluidStack fluid, World world, BlockPos pos) {
        this.fluid = fluid;
        this.world = world;
        this.pos = pos;
    }

    public FluidStack getFluid() {
        return fluid;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    /** Fluid moved somewhere (a pipe, a flow). */
    public static class FluidMotionEvent extends FluidEvent {
        public FluidMotionEvent(FluidStack fluid, World world, BlockPos pos) {
            super(fluid, world, pos);
        }
    }

    /** A tank was filled with an amount. */
    public static class FluidFillingEvent extends FluidEvent {
        private final IFluidTank tank;
        private final int amount;

        public FluidFillingEvent(FluidStack fluid, World world, BlockPos pos, IFluidTank tank, int amount) {
            super(fluid, world, pos);
            this.tank = tank;
            this.amount = amount;
        }

        public IFluidTank getTank() {
            return tank;
        }

        public int getAmount() {
            return amount;
        }
    }

    /** A tank was drained of an amount. */
    public static class FluidDrainingEvent extends FluidEvent {
        private final IFluidTank tank;
        private final int amount;

        public FluidDrainingEvent(FluidStack fluid, World world, BlockPos pos, IFluidTank tank, int amount) {
            super(fluid, world, pos);
            this.amount = amount;
            this.tank = tank;
        }

        public IFluidTank getTank() {
            return tank;
        }

        public int getAmount() {
            return amount;
        }
    }

    /** Fluid spilled into the world. */
    public static class FluidSpilledEvent extends FluidEvent {
        public FluidSpilledEvent(FluidStack fluid, World world, BlockPos pos) {
            super(fluid, world, pos);
        }
    }

    public static void fireEvent(FluidEvent event) {
        MinecraftForge.EVENT_BUS.post(event);
    }
}
