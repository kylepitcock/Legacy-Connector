package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Fluids by name. Water and lava are the vanilla ones. */
public abstract class FluidRegistry {
    private static final Map<String, Fluid> FLUIDS = new LinkedHashMap<>();
    private static final Map<Fluid, Integer> IDS = new HashMap<>();
    private static boolean universalBucket;

    public static final Fluid WATER = new Fluid("water", new ResourceLocation("blocks/water_still"), new ResourceLocation("blocks/water_flow"));
    public static final Fluid LAVA = new Fluid("lava", new ResourceLocation("blocks/lava_still"), new ResourceLocation("blocks/lava_flow"))
            .setLuminosity(15).setDensity(3000).setViscosity(6000).setTemperature(1300);

    static {
        registerFluid(WATER);
        registerFluid(LAVA);
    }

    public static synchronized boolean registerFluid(Fluid fluid) {
        if (FLUIDS.containsKey(fluid.getName())) return false;
        FLUIDS.put(fluid.getName(), fluid);
        IDS.put(fluid, IDS.size());
        return true;
    }

    public static synchronized boolean isFluidRegistered(Fluid fluid) {
        return fluid != null && FLUIDS.containsKey(fluid.getName());
    }

    public static synchronized boolean isFluidRegistered(String fluidName) {
        return FLUIDS.containsKey(fluidName);
    }

    public static synchronized Fluid getFluid(String fluidName) {
        return fluidName == null ? null : FLUIDS.get(fluidName);
    }

    public static synchronized String getFluidName(Fluid fluid) {
        return fluid == null ? null : fluid.getName();
    }

    public static synchronized String getFluidName(FluidStack stack) {
        return stack == null ? null : stack.getFluid().getName();
    }

    public static FluidStack getFluidStack(String fluidName, int amount) {
        Fluid fluid = getFluid(fluidName);
        return fluid == null ? null : new FluidStack(fluid, amount);
    }

    public static synchronized Map<String, Fluid> getRegisteredFluids() {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(FLUIDS));
    }

    public static synchronized int getFluidID(Fluid fluid) {
        Integer id = IDS.get(fluid);
        return id == null ? -1 : id;
    }

    public static void enableUniversalBucket() {
        universalBucket = true;
    }

    public static boolean isUniversalBucketEnabled() {
        return universalBucket;
    }

    public static boolean addBucketForFluid(Fluid fluid) {
        return true;
    }

    public static boolean hasBucket(Fluid fluid) {
        return false;
    }

    public static synchronized void registerFluidName(String name) {
    }
}
