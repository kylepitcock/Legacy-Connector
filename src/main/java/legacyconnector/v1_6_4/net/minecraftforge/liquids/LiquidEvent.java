package legacyconnector.v1_6_4.net.minecraftforge.liquids;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Forge 1.4.7/1.5.2 events a liquid container posts on the Forge event bus (tanks filling, draining and liquids moving). */
public class LiquidEvent extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
    public final LiquidStack liquid;
    public final int x;
    public final int y;
    public final int z;
    public final World world;

    public LiquidEvent(LiquidStack liquid, World world, int x, int y, int z) {
        this.liquid = liquid;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** A liquid moved through a pipe or a block. */
    public static class LiquidMotionEvent extends LiquidEvent {
        public LiquidMotionEvent(LiquidStack liquid, World world, int x, int y, int z) {
            super(liquid, world, x, y, z);
        }
    }

    /** A liquid was drained from a tank. */
    public static class LiquidDrainingEvent extends LiquidEvent {
        public final ILiquidTank tank;

        public LiquidDrainingEvent(LiquidStack liquid, World world, int x, int y, int z, ILiquidTank tank) {
            super(liquid, world, x, y, z);
            this.tank = tank;
        }
    }

    /** A liquid was filled into a tank. */
    public static class LiquidFillingEvent extends LiquidEvent {
        public final ILiquidTank tank;

        public LiquidFillingEvent(LiquidStack liquid, World world, int x, int y, int z, ILiquidTank tank) {
            super(liquid, world, x, y, z);
            this.tank = tank;
        }
    }

    public static final void fireEvent(LiquidEvent event) {
        try {
            legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Liquids").warn("A handler of a liquid event failed: {}", e.toString());
        }
    }
}
