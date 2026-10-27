package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * Legacy stacks standing for a modern slot (a hand, an equipment slot, a player inventory slot, an item entity's stack). 1.12.2
 * stacks are plain objects converted at the boundary, but mods change the stack they get from a slot in place
 * ({@code player.getHeldItem(hand).shrink(1)}, {@code entityItem.getItem().setCount(0)}): such a stack is remembered with a snapshot
 * of what it was, and its changes are written back to the slot the next time the slot is read through the layer, when a bridge
 * calls {@link #flushAll()} after handing control to legacy code, and at the end of every tick.
 *
 * <p>A change that only alters the count is applied to the modern stack in place (no round trip through the legacy form, which
 * could lose modern components); other changes replace the slot's stack with the converted legacy one. When vanilla changed the
 * slot in the meantime, a count change is applied as a difference and anything else is dropped.</p>
 */
public final class LiveStacks1122 {
    /** A modern slot. */
    public interface Slot {
        net.minecraft.world.item.ItemStack get();

        void set(net.minecraft.world.item.ItemStack stack);
    }

    private static final class Entry {
        final Slot slot;
        final ItemStack legacy;
        net.minecraft.world.item.ItemStack modern;
        net.minecraft.world.item.ItemStack modernSnapshot;
        legacyconnector.v1_12_2.net.minecraft.item.Item item;
        int count;
        int meta;
        net.minecraft.nbt.CompoundTag tag;

        Entry(Slot slot, ItemStack legacy, net.minecraft.world.item.ItemStack modern) {
            this.slot = slot;
            this.legacy = legacy;
            snapshot(modern);
        }

        void snapshot(net.minecraft.world.item.ItemStack now) {
            modern = now;
            modernSnapshot = now.copy();
            item = legacy.isEmpty() ? null : legacy.getItem();
            count = legacy.getCount();
            meta = legacy.getMetadata();
            NBTTagCompound t = legacy.getTagCompound();
            tag = t == null ? null : t.mcc$modern().copy();
        }

        boolean legacyChanged() {
            Object nowItem = legacy.isEmpty() ? null : legacy.getItem();
            if (nowItem != item || legacy.getMetadata() != meta) return true;
            if (legacy.getCount() != count && !(legacy.isEmpty() && item == null)) return true;
            return tagChanged();
        }

        boolean tagChanged() {
            NBTTagCompound t = legacy.getTagCompound();
            net.minecraft.nbt.CompoundTag now = t == null ? null : t.mcc$modern();
            if (now == null || now.isEmpty()) return tag != null && !tag.isEmpty();
            return !now.equals(tag);
        }

        boolean onlyCountChanged() {
            Object nowItem = legacy.isEmpty() ? null : legacy.getItem();
            return (nowItem == item || legacy.getCount() <= 0) && legacy.getMetadata() == meta && !tagChanged();
        }
    }

    /** owner (identity) -> slot key -> entry, kept apart per side (the integrated server and the client run on two threads). */
    private static final Map<Object, Map<Object, Entry>> SERVER = new IdentityHashMap<>();
    private static final Map<Object, Map<Object, Entry>> CLIENT = new IdentityHashMap<>();
    /** Lock for both maps. */
    private static final Object ENTRIES = new Object();

    private static Map<Object, Map<Object, Entry>> side(Object owner) {
        return owner instanceof net.minecraft.world.entity.Entity e && e.level().isClientSide() ? CLIENT : SERVER;
    }

    private LiveStacks1122() {}

    /** The legacy stack for a modern slot, writing back the changes of the stack handed out for it before. */
    public static ItemStack view(Object owner, Object key, Slot slot) {
        synchronized (ENTRIES) {
            var map = side(owner);
            Map<Object, Entry> slots = map.get(owner);
            Entry e = slots == null ? null : slots.get(key);
            if (e != null) {
                flush(e);
                var now = e.slot.get();
                if (now == e.modern && net.minecraft.world.item.ItemStack.matches(now, e.modernSnapshot)) return e.legacy;
            }
            var current = slot.get();
            ItemStack legacy = Stacks1122.fromModern(current);
            if (legacy == ItemStack.EMPTY) {
                // the shared empty stack is never changed in place; an empty slot needs no tracking
                if (slots != null) slots.remove(key);
                return legacy;
            }
            if (slots == null) map.put(owner, slots = new java.util.HashMap<>());
            slots.put(key, new Entry(slot, legacy, current));
            return legacy;
        }
    }

    /** Puts a legacy stack into a modern slot and keeps it bound, so later changes to the same object reach the slot too. */
    public static void put(Object owner, Object key, Slot slot, ItemStack stack) {
        var modern = Stacks1122.toModern(stack);
        synchronized (ENTRIES) {
            slot.set(modern);
            var map = side(owner);
            Map<Object, Entry> slots = map.get(owner);
            if (stack == null || stack == ItemStack.EMPTY || stack.isEmpty()) {
                if (slots != null) slots.remove(key);
                return;
            }
            if (slots == null) map.put(owner, slots = new java.util.HashMap<>());
            slots.put(key, new Entry(slot, stack, slot.get()));
        }
    }

    /** Forgets the stack handed out for a slot (after the slot was set by other means). */
    public static void forget(Object owner, Object key) {
        synchronized (ENTRIES) {
            Map<Object, Entry> slots = side(owner).get(owner);
            if (slots != null) slots.remove(key);
        }
    }

    private static void flush(Entry e) {
        if (!e.legacyChanged()) return;
        var current = e.slot.get();
        boolean untouched = current == e.modern && net.minecraft.world.item.ItemStack.matches(current, e.modernSnapshot);
        try {
            if (e.onlyCountChanged() && !current.isEmpty()) {
                int target = Math.max(0, e.legacy.getCount());
                int now = untouched ? target : current.getCount() + (target - e.count);
                if (now <= 0) e.slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                else current.setCount(Math.min(now, current.getMaxStackSize()));
            } else if (untouched) {
                e.slot.set(Stacks1122.toModern(e.legacy));
            }
        } catch (RuntimeException | LinkageError ex) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Stacks").debug("Writing a legacy stack back failed: {}", ex.toString());
        }
        e.snapshot(e.slot.get());
    }

    /** Writes every stack handed out on the calling side back to its slot and forgets them (after legacy code ran). */
    public static void flushAll() {
        flushAll(SERVER);
        flushAll(CLIENT);
    }

    private static void flushAll(Map<Object, Map<Object, Entry>> map) {
        List<Entry> all = new ArrayList<>();
        synchronized (ENTRIES) {
            if (map.isEmpty()) return;
            for (var slots : map.values()) all.addAll(slots.values());
            map.clear();
            for (Entry e : all) flush(e);
        }
    }

    /** Flushes each side at the end of each of its level ticks. */
    public static void register(net.neoforged.bus.api.IEventBus game) {
        game.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post e) -> flushAll(e.getLevel().isClientSide() ? CLIENT : SERVER));
    }
}
