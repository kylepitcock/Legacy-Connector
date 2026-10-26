package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.stats.StatBase;
import legacyconnector.v1_7_10.net.minecraft.stats.StatList;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomFishable;

/**
 * Forge 1.7.10 fishing loot lists. Additions are recorded and served to legacy code that draws from them
 * ({@link #getRandomFishable}); modern fishing uses its own loot table and is not changed.
 */
public class FishingHooks {
    private static final List<WeightedRandomFishable> junk = new ArrayList<>();
    private static final List<WeightedRandomFishable> treasure = new ArrayList<>();
    private static final List<WeightedRandomFishable> fish = new ArrayList<>();
    private static boolean defaults;

    private static synchronized void ensureDefaults() {
        if (defaults) return;
        defaults = true;
        junk.add(new WeightedRandomFishable(new ItemStack(Items.leather_boots), 10).setMaxDamagePercent(0.9F));
        junk.add(new WeightedRandomFishable(new ItemStack(Items.leather), 10));
        junk.add(new WeightedRandomFishable(new ItemStack(Items.bone), 10));
        junk.add(new WeightedRandomFishable(new ItemStack(Items.string), 5));
        junk.add(new WeightedRandomFishable(new ItemStack(Items.fishing_rod), 2).setMaxDamagePercent(0.9F));
        junk.add(new WeightedRandomFishable(new ItemStack(Items.bowl), 10));
        junk.add(new WeightedRandomFishable(new ItemStack(Items.stick), 5));
        junk.add(new WeightedRandomFishable(new ItemStack(Items.dye, 10, 0), 1));
        junk.add(new WeightedRandomFishable(new ItemStack(Items.rotten_flesh), 10));
        treasure.add(new WeightedRandomFishable(new ItemStack(Items.bow), 1).setMaxDamagePercent(0.25F).setEnchantable());
        treasure.add(new WeightedRandomFishable(new ItemStack(Items.fishing_rod), 1).setMaxDamagePercent(0.25F).setEnchantable());
        treasure.add(new WeightedRandomFishable(new ItemStack(Items.name_tag), 1));
        treasure.add(new WeightedRandomFishable(new ItemStack(Items.saddle), 1));
        fish.add(new WeightedRandomFishable(new ItemStack(Items.fish, 1, 0), 60));
        fish.add(new WeightedRandomFishable(new ItemStack(Items.fish, 1, 1), 25));
        fish.add(new WeightedRandomFishable(new ItemStack(Items.fish, 1, 2), 2));
        fish.add(new WeightedRandomFishable(new ItemStack(Items.fish, 1, 3), 13));
    }

    public static ItemStack getRandomFishable(Random rand, float chance) {
        return getRandomFishable(rand, chance, 0, 0);
    }

    public static ItemStack getRandomFishable(Random rand, float chance, int luck, int speed) {
        ensureDefaults();
        FishableCategory category = getFishableCategory(chance, luck, speed);
        List<WeightedRandomFishable> list = category.list();
        synchronized (FishingHooks.class) {
            if (list.isEmpty()) return null;
            return ((WeightedRandomFishable) WeightedRandom.getRandomItem(rand, list)).getItemStack(rand);
        }
    }

    public static FishableCategory getFishableCategory(float chance, int luck, int speed) {
        float junkChance = clamp(0.1F - luck * 0.025F - speed * 0.01F);
        float treasureChance = clamp(0.05F + luck * 0.01F - speed * 0.01F);
        if (chance < junkChance) return FishableCategory.JUNK;
        if (chance < junkChance + treasureChance) return FishableCategory.TREASURE;
        return FishableCategory.FISH;
    }

    private static float clamp(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }

    public static synchronized void removeJunk(ItemStack stack) {
        ensureDefaults();
        remove(junk, stack);
    }

    public static synchronized void addJunk(WeightedRandomFishable entry) {
        ensureDefaults();
        junk.add(entry);
    }

    public static synchronized void removeTreasure(ItemStack stack) {
        ensureDefaults();
        remove(treasure, stack);
    }

    public static synchronized void addTreasure(WeightedRandomFishable entry) {
        ensureDefaults();
        treasure.add(entry);
    }

    public static synchronized void removeFish(ItemStack stack) {
        ensureDefaults();
        remove(fish, stack);
    }

    public static synchronized void addFish(WeightedRandomFishable entry) {
        ensureDefaults();
        fish.add(entry);
    }

    private static void remove(List<WeightedRandomFishable> list, ItemStack stack) {
        for (Iterator<WeightedRandomFishable> it = list.iterator(); it.hasNext();) {
            WeightedRandomFishable w = it.next();
            ItemStack s = w.getItemStack(new Random(0));
            if (ItemStack.areItemStacksEqual(s, stack) || s != null && stack != null && s.getItem() == stack.getItem() && s.getItemDamage() == stack.getItemDamage()) {
                it.remove();
            }
        }
    }

    public enum FishableCategory {
        JUNK(null),
        TREASURE(null),
        FISH(StatList.fishCaughtStat);

        public final StatBase stat;

        FishableCategory(StatBase stat) {
            this.stat = stat;
        }

        List<WeightedRandomFishable> list() {
            ensureDefaults();
            return switch (this) {
                case JUNK -> junk;
                case TREASURE -> treasure;
                case FISH -> fish;
            };
        }
    }
}
