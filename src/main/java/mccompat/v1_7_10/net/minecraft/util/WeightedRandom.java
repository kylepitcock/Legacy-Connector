package mccompat.v1_7_10.net.minecraft.util;

import java.util.Collection;
import java.util.Random;

public class WeightedRandom {
    public static int getTotalWeight(Collection<? extends Item> items) {
        int total = 0;
        for (Item i : items) total += i.itemWeight;
        return total;
    }

    public static Item getRandomItem(Random random, Collection<? extends Item> items, int totalWeight) {
        if (totalWeight <= 0) throw new IllegalArgumentException();
        int pick = random.nextInt(totalWeight);
        for (Item i : items) {
            pick -= i.itemWeight;
            if (pick < 0) return i;
        }
        return null;
    }

    public static Item getRandomItem(Random random, Collection<? extends Item> items) {
        return getRandomItem(random, items, getTotalWeight(items));
    }

    public static int getTotalWeight(Item[] items) {
        int total = 0;
        for (Item i : items) total += i.itemWeight;
        return total;
    }

    public static Item getRandomItem(Random random, Item[] items, int totalWeight) {
        return getRandomItem(random, java.util.Arrays.asList(items), totalWeight);
    }

    public static Item getRandomItem(Random random, Item[] items) {
        return getRandomItem(random, items, getTotalWeight(items));
    }

    public static class Item {
        public int itemWeight;

        public Item(int weight) {
            this.itemWeight = weight;
        }
    }
}
