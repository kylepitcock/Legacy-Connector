package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;

/** Forge 1.7.10 fluid registry: names and numeric ids (water 1, lava 2, then registration order). */
public abstract class FluidRegistry {
    private static final Map<String, Fluid> FLUIDS = new LinkedHashMap<>();
    private static final Map<String, Integer> IDS = new LinkedHashMap<>();
    private static final Map<Integer, Fluid> BY_ID = new HashMap<>();
    private static final Map<Block, Fluid> BY_BLOCK = new HashMap<>();
    private static int maxID = 0;

    public static final Fluid WATER = new Fluid("water").setUnlocalizedName("water");
    public static final Fluid LAVA = new Fluid("lava").setUnlocalizedName("lava").setLuminosity(15).setDensity(3000).setViscosity(6000).setTemperature(1300);
    /** The render id of fluid blocks (Forge assigned it at client start; the handler is {@link RenderBlockFluid}). */
    public static int renderIdFluid = legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.getNextAvailableRenderId();

    static {
        legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.registerBlockHandler(new RenderBlockFluid());
        registerFluid(WATER);
        registerFluid(LAVA);
    }

    private FluidRegistry() {}

    public static synchronized boolean registerFluid(Fluid fluid) {
        if (fluid == null || IDS.containsKey(fluid.getName())) return false;
        int id = ++maxID;
        FLUIDS.put(fluid.getName(), fluid);
        IDS.put(fluid.getName(), id);
        BY_ID.put(id, fluid);
        if (fluid.getBlock() != null) BY_BLOCK.put(fluid.getBlock(), fluid);
        if (fluid != WATER && fluid != LAVA) MinecraftForge.EVENT_BUS.post(new FluidRegisterEvent(fluid.getName(), id));
        return true;
    }

    /** Fluid.setBlock after registration still links the block. */
    public static synchronized void mcc$blockSet(Fluid fluid, Block block) {
        if (block != null && IDS.containsKey(fluid.getName())) BY_BLOCK.put(block, fluid);
    }

    public static boolean isFluidDefault(Fluid fluid) {
        return true;
    }

    public static synchronized boolean isFluidRegistered(Fluid fluid) {
        return fluid != null && IDS.containsKey(fluid.getName());
    }

    public static synchronized boolean isFluidRegistered(String fluidName) {
        return fluidName != null && IDS.containsKey(fluidName);
    }

    public static synchronized Fluid getFluid(String fluidName) {
        return fluidName == null ? null : FLUIDS.get(fluidName);
    }

    public static synchronized Fluid getFluid(int fluidID) {
        return BY_ID.get(fluidID);
    }

    public static synchronized int getFluidID(Fluid fluid) {
        return fluid == null ? -1 : getFluidID(fluid.getName());
    }

    public static synchronized int getFluidID(String fluidName) {
        Integer id = IDS.get(fluidName);
        return id == null ? -1 : id;
    }

    public static synchronized String getFluidName(int fluidID) {
        Fluid f = BY_ID.get(fluidID);
        return f == null ? null : f.getName();
    }

    public static String getFluidName(Fluid fluid) {
        return fluid == null ? null : fluid.getName();
    }

    public static String getFluidName(FluidStack stack) {
        return stack == null || stack.getFluid() == null ? null : stack.getFluid().getName();
    }

    public static FluidStack getFluidStack(String fluidName, int amount) {
        Fluid f = getFluid(fluidName);
        return f == null ? null : new FluidStack(f, amount);
    }

    public static synchronized Map<String, Fluid> getRegisteredFluids() {
        return new LinkedHashMap<>(FLUIDS);
    }

    public static synchronized Map<String, Integer> getRegisteredFluidIDs() {
        return new LinkedHashMap<>(IDS);
    }

    public static synchronized Map<Fluid, Integer> getRegisteredFluidIDsByFluid() {
        Map<Fluid, Integer> out = new LinkedHashMap<>();
        FLUIDS.forEach((n, f) -> out.put(f, IDS.get(n)));
        return out;
    }

    public static synchronized Fluid lookupFluidForBlock(Block block) {
        if (block == null) return null;
        if (block == Blocks.water || block == Blocks.flowing_water) return WATER;
        if (block == Blocks.lava || block == Blocks.flowing_lava) return LAVA;
        return BY_BLOCK.get(block);
    }

    public static int getMaxID() {
        return maxID;
    }

    public static String getDefaultFluidName(Fluid key) {
        return key == null ? null : key.getName();
    }

    public static class FluidRegisterEvent extends Event {
        public final String fluidName;
        public final int fluidID;

        public FluidRegisterEvent(String fluidName, int fluidID) {
            this.fluidName = fluidName;
            this.fluidID = fluidID;
        }
    }
}
