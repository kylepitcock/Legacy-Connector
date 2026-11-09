package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievements;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.FurnaceOutputSlot;
import legacyconnector.vb1_7_3.net.minecraft.stat.Stat;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class FurnaceOutputSlotB173Test {
    private static final class Player extends PlayerEntity {
        final List<String> events;
        final List<Stat> awards = new ArrayList<>();
        Player(List<String> events) { super(null); this.events = events; }
        @Override public void increaseStat(Stat stat, int amount) {
            assertEquals(1, amount);
            awards.add(stat);
            events.add("award");
        }
    }

    private static final class Output implements Inventory {
        final List<String> events;
        final ItemStack[] stacks = new ItemStack[4];
        Output(List<String> events) { this.events = events; }
        public int size() { return stacks.length; }
        public ItemStack getStack(int slot) { return stacks[slot]; }
        public ItemStack removeStack(int slot, int amount) { throw new AssertionError("Pickup must not consume another input"); }
        public void setStack(int slot, ItemStack stack) { throw new AssertionError("Pickup must not replace another input"); }
        public String getName() { return "Output"; }
        public int getMaxCountPerStack() { return 64; }
        public void markDirty() { events.add("dirty"); }
        public boolean canPlayerUse(PlayerEntity player) { return true; }
    }

    private static final class Result extends ItemStack {
        final Player expected;
        final List<String> events;
        Result(int id, Player expected, List<String> events) { super(id, 3, 0); this.expected = expected; this.events = events; }
        @Override public void onCraft(World world, PlayerEntity player) {
            assertSame(expected, player);
            assertSame(expected.world, world);
            events.add("craft");
        }
    }

    @Test void bindsActualInventoryCoordinatesAndRejectsEveryInsertion() {
        var events = new ArrayList<String>();
        var inventory = new Output(events);
        var player = new Player(events);
        var slot = new FurnaceOutputSlot(player, inventory, 3, 116, 44);
        assertSame(inventory, slot.inventory);
        assertEquals(3, slot.index);
        assertEquals(116, slot.x);
        assertEquals(44, slot.y);
        assertFalse(slot.canInsert(new ItemStack(265, 1, 0)));
        assertFalse(slot.canInsert(null));
    }

    @Test void pickupRunsCraftThenOriginalAchievementsThenDirtyWithoutConsumingInputs() {
        for (int id : new int[] {265, 350, 1}) {
            var events = new ArrayList<String>();
            var player = new Player(events);
            var inventory = new Output(events);
            var result = new Result(id, player, events);
            new FurnaceOutputSlot(player, inventory, 2, 116, 35).onTakeItem(result);
            assertEquals(3, result.count);
            assertEquals(id == 1 ? List.of("craft", "dirty") : List.of("craft", "award", "dirty"), events);
            assertEquals(id == 265 ? List.of(Achievements.ACQUIRE_IRON)
                    : id == 350 ? List.of(Achievements.COOK_FISH) : List.of(), player.awards);
        }
    }
}
