package legacyconnector.v1_6_4.net.minecraftforge.liquids;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

/** Forge 1.5.2/1.6.4's name registry of liquids ("Water", "Lava", "Oil"): a name stands for one canonical stack. */
public final class LiquidDictionary {
    private static final Map<String, LiquidStack> LIQUIDS = new HashMap<>();

    private LiquidDictionary() {}

    /** The liquid called {@code name}, registered with {@code liquid} when it is new; the canonical stack otherwise. */
    public static synchronized LiquidStack getOrCreateLiquid(String name, LiquidStack liquid) {
        LiquidStack existing = LIQUIDS.get(name);
        if (existing != null) return existing;
        if (liquid == null) return null;
        LIQUIDS.put(name, liquid);
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
