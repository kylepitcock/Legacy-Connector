package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * The weighted list of mobs dungeon spawners get (vanilla's skeleton, zombie, zombie, spider by default). Mods add and remove
 * their mobs; the list is what {@link #getRandomDungeonMob} picks from. The modern dungeon feature picks its own mob.
 */
public class DungeonHooks {
    private static final List<DungeonMob> dungeonMobs = new ArrayList<>();

    static {
        addDungeonMob(new ResourceLocation("skeleton"), 100);
        addDungeonMob(new ResourceLocation("zombie"), 200);
        addDungeonMob(new ResourceLocation("spider"), 100);
    }

    /** Adds weight to the mob's entry (a new entry if absent); returns the entry's weight. */
    public static float addDungeonMob(ResourceLocation name, int rarity) {
        if (rarity <= 0) throw new IllegalArgumentException("Rarity must be greater then zero");
        synchronized (dungeonMobs) {
            for (DungeonMob mob : dungeonMobs) {
                if (name.equals(mob.type)) return mob.weight += rarity;
            }
            dungeonMobs.add(new DungeonMob(rarity, name));
            return rarity;
        }
    }

    public static int removeDungeonMob(ResourceLocation name) {
        synchronized (dungeonMobs) {
            for (DungeonMob mob : dungeonMobs) {
                if (name.equals(mob.type)) {
                    dungeonMobs.remove(mob);
                    return mob.weight;
                }
            }
        }
        return 0;
    }

    public static ResourceLocation getRandomDungeonMob(Random rand) {
        synchronized (dungeonMobs) {
            int total = 0;
            for (DungeonMob mob : dungeonMobs) total += mob.weight;
            if (total <= 0) return new ResourceLocation("pig");
            int pick = rand.nextInt(total);
            for (DungeonMob mob : dungeonMobs) {
                pick -= mob.weight;
                if (pick < 0) return mob.type;
            }
            return dungeonMobs.get(0).type;
        }
    }

    public static class DungeonMob {
        public int weight;
        public final ResourceLocation type;

        public DungeonMob(int weight, ResourceLocation type) {
            this.weight = weight;
            this.type = type;
        }

        @Override
        public boolean equals(Object target) {
            return target instanceof DungeonMob m && type.equals(m.type);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }
    }
}
