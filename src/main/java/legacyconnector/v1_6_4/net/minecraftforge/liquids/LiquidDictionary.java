package legacyconnector.v1_6_4.net.minecraftforge.liquids;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

/** Forge 1.5.2/1.6.4's name registry of liquids ("Water", "Lava", "Oil"): a name stands for one canonical stack. */
public final class LiquidDictionary {
    private static final Map<String, LiquidStack> LIQUIDS = new HashMap<>();

    static {
        // Forge seeds the dictionary with the two vanilla liquids (Forestry registers containers for "Water" and "Lava" at once)
        LIQUIDS.put("Water", new LiquidStack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.water, LiquidContainerRegistry.BUCKET_VOLUME));
        LIQUIDS.put("Lava", new LiquidStack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.lava, LiquidContainerRegistry.BUCKET_VOLUME));
    }

    public LiquidDictionary() {}

    /** Posted on Forge's event bus when a liquid is registered under a new name (mods keep their own tables of liquids by it). */
    public static class LiquidRegisterEvent extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
        public final String Name;
        public final LiquidStack Liquid;

        public LiquidRegisterEvent(String name, LiquidStack liquid) {
            this.Name = name;
            this.Liquid = liquid.copy();
        }
    }

    /** The liquid called {@code name}, registered with {@code liquid} when it is new; the canonical stack otherwise. */
    public static synchronized LiquidStack getOrCreateLiquid(String name, LiquidStack liquid) {
        LiquidStack existing = LIQUIDS.get(name);
        if (existing != null) return existing;
        if (liquid == null) return null;
        LIQUIDS.put(name, liquid);
        try {
            legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new LiquidRegisterEvent(name, liquid));
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Liquids").warn("A handler of the registration of the liquid {} failed: {}", name, e.toString());
        }
        org.kylepitcock.legacyconnector.v1_6_4.bridge.Liquids164.liquidNamed(name, liquid);
        return liquid;
    }

    /** A copy of the named liquid with the given amount, or null when no liquid has that name. */
    public static synchronized LiquidStack getLiquid(String name, int amount) {
        LiquidStack canonical = LIQUIDS.get(name);
        if (canonical == null) return null;
        LiquidStack copy = canonical.copy();
        copy.amount = amount;
        return copy;
    }

    public static synchronized LiquidStack getCanonicalLiquid(String name) {
        return LIQUIDS.get(name);
    }

    /** The canonical stack of the liquid {@code stack} is (same id and metadata), or null. */
    public static synchronized LiquidStack getCanonicalLiquid(LiquidStack stack) {
        if (stack == null) return null;
        for (LiquidStack s : LIQUIDS.values()) if (s.isLiquidEqual(stack)) return s;
        return null;
    }

    /** The dictionary name of a liquid (same id and metadata), or null. */
    public static synchronized String liquidName(LiquidStack stack) {
        return findLiquidName(stack);
    }

    static synchronized String findLiquidName(LiquidStack stack) {
        for (Map.Entry<String, LiquidStack> e : LIQUIDS.entrySet()) if (e.getValue().isLiquidEqual(stack)) return e.getKey();
        return null;
    }

    public static synchronized Map<String, LiquidStack> getLiquids() {
        return ImmutableMap.copyOf(LIQUIDS);
    }
}
