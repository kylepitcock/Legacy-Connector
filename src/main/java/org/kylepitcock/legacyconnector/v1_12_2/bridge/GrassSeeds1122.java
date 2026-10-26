package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Forge 1.12's grass seed list ({@code MinecraftForge.addGrassSeed}): broken tall grass dropped, one time in eight, a seed picked by
 * weight from the list, which starts with wheat seeds of weight 10. The modern short grass and fern keep their own wheat seed loot;
 * the mods' seeds are rolled alongside with their share of the list's weight, through the block drops event.
 */
public final class GrassSeeds1122 {
    private static final int WHEAT_WEIGHT = 10;

    private record Seed(ItemStack stack, int weight) {}

    private static final List<Seed> SEEDS = new ArrayList<>();
    private static boolean listening;

    private GrassSeeds1122() {}

    public static synchronized void add(ItemStack seed, int weight) {
        if (seed == null || seed.isEmpty() || weight <= 0) return;
        SEEDS.add(new Seed(seed.copy(), weight));
        if (!listening) {
            listening = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BlockDropsEvent.class, GrassSeeds1122::onDrops);
        }
    }

    private static void onDrops(BlockDropsEvent event) {
        var state = event.getState();
        if (!state.is(Blocks.SHORT_GRASS) && !state.is(Blocks.FERN)) return;
        if (event.getTool().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_DIG)) return; // shears take the grass itself
        var random = ThreadLocalRandom.current();
        if (random.nextInt(8) != 0) return;
        ItemStack picked = null;
        synchronized (GrassSeeds1122.class) {
            int total = WHEAT_WEIGHT;
            for (Seed s : SEEDS) total += s.weight();
            int roll = random.nextInt(total) - WHEAT_WEIGHT;
            if (roll < 0) return; // wheat seeds: the modern loot table's own roll
            for (Seed s : SEEDS) {
                roll -= s.weight();
                if (roll < 0) {
                    picked = s.stack().copy();
                    break;
                }
            }
        }
        if (picked == null) return;
        var modern = Stacks1122.toModern(picked);
        if (modern.isEmpty()) return;
        var pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, modern));
    }
}
