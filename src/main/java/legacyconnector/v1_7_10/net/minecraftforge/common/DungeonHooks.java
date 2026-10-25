package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom;

/** The mob spawner types a dungeon (monster room) can contain, by name and weight. */
public class DungeonHooks {
    private static final List<DungeonMob> DUNGEON_MOBS = new ArrayList<>();

    static {
        addDungeonMob("Skeleton", 100);
        addDungeonMob("Zombie", 200);
        addDungeonMob("Spider", 100);
    }

    public static class DungeonMob extends WeightedRandom.Item {
        public String type;

        public DungeonMob(int weight, String type) {
            super(weight);
            this.type = type;
        }

        @Override
        public boolean equals(Object target) {
            return target instanceof DungeonMob other && type.equals(other.type);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }
    }

    public static synchronized float addDungeonMob(String name, int rarity) {
        if (rarity <= 0) throw new IllegalArgumentException("Rarity must be greater then zero");
        for (DungeonMob mob : DUNGEON_MOBS) {
            if (name.equals(mob.type)) {
                mob.itemWeight += rarity;
                return mob.itemWeight;
            }
        }
        DUNGEON_MOBS.add(new DungeonMob(rarity, name));
        return rarity;
    }

    public static synchronized int removeDungeonMob(String name) {
        for (DungeonMob mob : DUNGEON_MOBS) {
            if (name.equals(mob.type)) {
                DUNGEON_MOBS.remove(mob);
                return mob.itemWeight;
            }
        }
        return 0;
    }

    public static synchronized String getRandomDungeonMob(Random rand) {
        if (DUNGEON_MOBS.isEmpty()) return "Zombie";
        DungeonMob mob = (DungeonMob) WeightedRandom.getRandomItem(rand, DUNGEON_MOBS);
        return mob.type;
    }
}
