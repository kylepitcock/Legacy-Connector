package legacyconnector.v1_6_4.net.minecraftforge.liquids;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Forge 1.5.2/1.6.4's table of liquid containers (bucket, bottle, cell...): which item holds how much of which liquid. */
public final class LiquidContainerRegistry {
    public static final int BUCKET_VOLUME = 1000;
    public static final ItemStack EMPTY_BUCKET = new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.bucket);
    public static final ItemStack EMPTY_BOTTLE = new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.glass_bottle);

    private static final List<LiquidContainerData> DATA = new ArrayList<>();

    static {
        // vanilla buckets (Forge registered these itself)
        DATA.add(new LiquidContainerData(new LiquidStack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.water, BUCKET_VOLUME),
                new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.water_bucket), EMPTY_BUCKET));
        DATA.add(new LiquidContainerData(new LiquidStack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.lava, BUCKET_VOLUME),
                new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.lava_bucket), EMPTY_BUCKET));
    }

    private LiquidContainerRegistry() {}

    public static synchronized void registerLiquid(LiquidContainerData data) {
        DATA.add(data);
        org.kylepitcock.legacyconnector.v1_6_4.bridge.Liquids164.containerRegistered(data);
    }

    public static synchronized LiquidContainerData[] getRegisteredLiquidContainerData() {
        return DATA.toArray(new LiquidContainerData[0]);
    }

    /** The filled container made by filling {@code empty} from {@code liquid}, or null when the liquid does not fit (too little, no such container). */
    public static synchronized ItemStack fillLiquidContainer(LiquidStack liquid, ItemStack empty) {
        if (liquid == null || empty == null) return null;
        for (LiquidContainerData d : DATA) {
            if (d.stillLiquid.isLiquidEqual(liquid) && d.stillLiquid.amount <= liquid.amount && same(d.container, empty)) return d.filled.copy();
        }
        return null;
    }

    /** The liquid a filled container holds (a copy), or null for anything else. */
    public static synchronized LiquidStack getLiquidForFilledItem(ItemStack filled) {
        if (filled == null) return null;
        for (LiquidContainerData d : DATA) if (same(d.filled, filled)) return d.stillLiquid.copy();
        return null;
    }

    /** The filled container of {@code container} with the given liquid, or null. */
    public static synchronized ItemStack getFilledItemForLiquid(LiquidStack liquid) {
        if (liquid == null) return null;
        for (LiquidContainerData d : DATA) if (d.stillLiquid.isLiquidEqual(liquid)) return d.filled.copy();
        return null;
    }

    public static synchronized boolean containsLiquid(ItemStack filled, LiquidStack liquid) {
        LiquidStack held = getLiquidForFilledItem(filled);
        return held != null && held.isLiquidEqual(liquid);
    }

    public static synchronized boolean isLiquid(ItemStack stack) {
        return getLiquidForFilledItem(stack) != null;
    }

    public static synchronized boolean isBucket(ItemStack stack) {
        if (stack == null) return false;
        if (same(EMPTY_BUCKET, stack)) return true;
        for (LiquidContainerData d : DATA) if (same(d.filled, stack) && same(d.container, EMPTY_BUCKET)) return true;
        return false;
    }

    public static synchronized boolean isContainer(ItemStack stack) {
        return isEmptyContainer(stack) || isFilledContainer(stack);
    }

    public static synchronized boolean isEmptyContainer(ItemStack stack) {
        if (stack == null) return false;
        for (LiquidContainerData d : DATA) if (same(d.container, stack)) return true;
        return false;
    }

    public static synchronized boolean isFilledContainer(ItemStack stack) {
        return getLiquidForFilledItem(stack) != null;
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem() && (a.getItemDamage() == b.getItemDamage() || a.getItemDamage() == 32767 || b.getItemDamage() == 32767);
    }
}
