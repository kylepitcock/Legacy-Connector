package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.List;
import java.util.Random;

/** Picks entries of a list with probability proportional to their weight. */
public class WeightedRandom {
    public static int getTotalWeight(List<? extends Item> collection) {
        int total = 0;
        for (Item item : collection) total += item.itemWeight;
        return total;
    }

    public static <T extends Item> T getRandomItem(Random random, List<T> collection, int totalWeight) {
        if (totalWeight <= 0) throw new IllegalArgumentException();
        return getRandomItem(collection, random.nextInt(totalWeight));
    }

    public static <T extends Item> T getRandomItem(List<T> collection, int weight) {
        for (T item : collection) {
            weight -= item.itemWeight;
            if (weight < 0) return item;
        }
        return null;
    }

    public static <T extends Item> T getRandomItem(Random random, List<T> collection) {
        return getRandomItem(random, collection, getTotalWeight(collection));
    }

    public static class Item {
        public int itemWeight;

        public Item(int itemWeight) {
            this.itemWeight = itemWeight;
        }
    }
}
