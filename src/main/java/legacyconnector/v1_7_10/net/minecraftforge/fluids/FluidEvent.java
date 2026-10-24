package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;

/** Base of the events fluid containers and tanks post when their contents change. */
public class FluidEvent extends Event {
    public final FluidStack fluid;
    public final int x;
    public final int y;
    public final int z;
    public final legacyconnector.v1_7_10.net.minecraft.world.World world;

    public FluidEvent(FluidStack fluid, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        this.fluid = fluid;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void fireEvent(FluidEvent event) {
        MinecraftForge.EVENT_BUS.post(event);
    }

    public static class FluidMotionEvent extends FluidEvent {
        public FluidMotionEvent(FluidStack fluid, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
            super(fluid, world, x, y, z);
        }
    }

    public static class FluidFillingEvent extends FluidEvent {
        public final IFluidTank tank;
        public final int amount;

        public FluidFillingEvent(FluidStack fluid, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, IFluidTank tank, int amount) {
            super(fluid, world, x, y, z);
            this.tank = tank;
            this.amount = amount;
        }
    }

    public static class FluidDrainingEvent extends FluidEvent {
        public final IFluidTank tank;
        public final int amount;

        public FluidDrainingEvent(FluidStack fluid, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, IFluidTank tank, int amount) {
            super(fluid, world, x, y, z);
            this.tank = tank;
            this.amount = amount;
        }
    }
}
