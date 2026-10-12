package mccompat.v1_7_10.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.util.WeightedRandom;
import mccompat.v1_7_10.net.minecraft.util.WeightedRandomChestContent;

/**
 * Forge 1.7.10 structure-chest loot categories. Mod additions are recorded and served to legacy code; they are not
 * injected into modern loot tables yet.
 */
public class ChestGenHooks {
    public static final String MINESHAFT_CORRIDOR = "mineshaftCorridor";
    public static final String PYRAMID_DESERT_CHEST = "pyramidDesertyChest";
    public static final String PYRAMID_JUNGLE_CHEST = "pyramidJungleChest";
    public static final String PYRAMID_JUNGLE_DISPENSER = "pyramidJungleDispenser";
    public static final String STRONGHOLD_CORRIDOR = "strongholdCorridor";
    public static final String STRONGHOLD_LIBRARY = "strongholdLibrary";
    public static final String STRONGHOLD_CROSSING = "strongholdCrossing";
    public static final String VILLAGE_BLACKSMITH = "villageBlacksmith";
    public static final String BONUS_CHEST = "bonusChest";
    public static final String DUNGEON_CHEST = "dungeonChest";

    private static final HashMap<String, ChestGenHooks> chestInfo = new HashMap<>();

    private final String category;
    private int countMin;
    private int countMax;
    private final ArrayList<WeightedRandomChestContent> contents = new ArrayList<>();

    public ChestGenHooks(String category) {
        this.category = category;
    }

    public ChestGenHooks(String category, WeightedRandomChestContent[] items, int min, int max) {
        this(category);
        for (WeightedRandomChestContent i : items) contents.add(i);
        this.countMin = min;
        this.countMax = max;
    }

    public static synchronized ChestGenHooks getInfo(String category) {
        return chestInfo.computeIfAbsent(category, c -> new ChestGenHooks(c, new WeightedRandomChestContent[0], 3, 8));
    }

    public static ItemStack[] generateStacks(Random rand, ItemStack source, int min, int max) {
        int count = min + (max > min ? rand.nextInt(max - min + 1) : 0);
        ItemStack s = source.copy();
        ItemStack.mcc$set$stackSize(s, count);
        return new ItemStack[] {s};
    }

    public static WeightedRandomChestContent[] getItems(String category, Random rnd) {
        return getInfo(category).getItems(rnd);
    }

    public static int getCount(String category, Random rand) {
        return getInfo(category).getCount(rand);
    }

    public static void addItem(String category, WeightedRandomChestContent item) {
        getInfo(category).addItem(item);
    }

    public static void removeItem(String category, ItemStack item) {
        getInfo(category).removeItem(item);
    }

    public static ItemStack getOneItem(String category, Random rand) {
        return getInfo(category).getOneItem(rand);
    }

    public void addItem(WeightedRandomChestContent item) {
        contents.add(item);
    }

    public void removeItem(ItemStack item) {
        contents.removeIf(c -> c.theItemId != null && c.theItemId.isItemEqual(item));
    }

    public WeightedRandomChestContent[] getItems(Random rnd) {
        return contents.toArray(new WeightedRandomChestContent[0]);
    }

    public int getCount(Random rand) {
        return countMin < countMax ? countMin + rand.nextInt(countMax - countMin) : countMin;
    }

    public ItemStack getOneItem(Random rand) {
        WeightedRandomChestContent[] items = getItems(rand);
        if (items.length == 0) return null;
        WeightedRandomChestContent item = (WeightedRandomChestContent) WeightedRandom.getRandomItem(rand, items);
        ItemStack[] stacks = item.generateChestContent(rand, null);
        return stacks.length > 0 ? stacks[0] : null;
    }

    public int getMin() {
        return countMin;
    }

    public int getMax() {
        return countMax;
    }

    public void setMin(int value) {
        countMin = value;
    }

    public void setMax(int value) {
        countMax = value;
    }

    @Override
    public String toString() {
        return "ChestGenHooks{" + category + ", " + contents.size() + " entries}";
    }
}
