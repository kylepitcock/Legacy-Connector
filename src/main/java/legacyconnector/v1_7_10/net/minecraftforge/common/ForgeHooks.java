package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom;

/**
 * Forge 1.7.10's static hook collection. Only the state mods reach into directly lives here (the seed list tall grass
 * drops from); behaviour hooks the engine drives are not part of the shim.
 */
public class ForgeHooks {
    /** Seeds tall grass can drop (mods add their own through {@code MinecraftForge.addGrassSeed}). */
    static final List<SeedEntry> seedList = new ArrayList<>();

    public static class SeedEntry extends WeightedRandom.Item {
        public final ItemStack seed;

        public SeedEntry(ItemStack seed, int weight) {
            super(weight);
            this.seed = seed;
        }

        public ItemStack getStack(java.util.Random rand) {
            return seed.copy();
        }
    }
}
