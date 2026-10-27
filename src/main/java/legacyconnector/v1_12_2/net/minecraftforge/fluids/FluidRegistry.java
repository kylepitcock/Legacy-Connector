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
    private static final java.util.Set<Fluid> BUCKET_FLUIDS = new java.util.LinkedHashSet<>();

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

    /** The fluid whose block this is (vanilla water and lava, flowing or still, included); null for other blocks. */
    public static synchronized Fluid lookupFluidForBlock(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        if (block == null) return null;
        var name = block.getRegistryName();
        if (name != null && "minecraft".equals(name.getNamespace())) {
            String path = name.getPath();
            if (path.equals("water") || path.equals("flowing_water")) return WATER;
            if (path.equals("lava") || path.equals("flowing_lava")) return LAVA;
        }
        for (Fluid f : FLUIDS.values()) if (f.getBlock() == block) return f;
        return null;
    }

    /** The registered name of the fluid ("modid:name" in Forge's default-fluid table; the plain name here). */
    public static synchronized String getDefaultFluidName(Fluid fluid) {
        return getFluidName(fluid);
    }

    /** The mod that registered the fluid of a stack (minecraft for water and lava). */
    public static synchronized String getModId(FluidStack stack) {
        if (stack == null || stack.getFluid() == null) return null;
        Fluid f = stack.getFluid();
        if (f == WATER || f == LAVA) return "minecraft";
        var block = f.getBlock();
        return block != null && block.getRegistryName() != null ? block.getRegistryName().getNamespace() : null;
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

    /** Gives the fluid a universal bucket variant (registering the fluid if needed). */
    public static synchronized boolean addBucketForFluid(Fluid fluid) {
        if (fluid == null) return false;
        if (!isFluidRegistered(fluid.getName())) registerFluid(fluid);
        return BUCKET_FLUIDS.add(fluid);
    }

    public static synchronized boolean hasBucket(Fluid fluid) {
        return fluid != null && BUCKET_FLUIDS.contains(fluid);
    }

    /** The fluids that have a universal bucket. */
    public static synchronized java.util.Set<Fluid> getBucketFluids() {
        return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(BUCKET_FLUIDS));
    }

    public static synchronized void registerFluidName(String name) {
    }
}
