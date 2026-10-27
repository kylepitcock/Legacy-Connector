package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/**
 * 1.12.2 loot table. A table a mod builds holds legacy pools; a table from the server (the loot table manager, the load event)
 * is a view of the modern table: pools added to it become modern pools, {@code getPool} gives views of the modern pools, and
 * rolling it rolls the modern table (with every legacy entry added to it).
 */
public class LootTable {
    public static final LootTable EMPTY_LOOT_TABLE = new LootTable(new LootPool[0]);

    private final List<LootPool> pools;
    private boolean isFrozen;
    private final net.minecraft.world.level.storage.loot.LootTable mcc$modern;

    public LootTable(LootPool[] pools) {
        this.pools = new ArrayList<>(java.util.Arrays.asList(pools == null ? new LootPool[0] : pools));
        this.mcc$modern = null;
    }

    private LootTable(net.minecraft.world.level.storage.loot.LootTable modern) {
        this.pools = new ArrayList<>();
        this.mcc$modern = modern;
    }

    public static LootTable mcc$view(net.minecraft.world.level.storage.loot.LootTable modern) {
        if (modern == null || modern == net.minecraft.world.level.storage.loot.LootTable.EMPTY) return EMPTY_LOOT_TABLE;
        return new LootTable(modern);
    }

    /** The modern table: the one this views, or one built from the legacy pools. */
    public net.minecraft.world.level.storage.loot.LootTable mcc$modern() {
        if (mcc$modern != null) return mcc$modern;
        var table = net.minecraft.world.level.storage.loot.LootTable.lootTable().build();
        for (LootPool p : pools) table.addPool(p.mcc$modern());
        return table;
    }

    public List<ItemStack> generateLootForPools(Random rand, LootContext context) {
        List<ItemStack> out = new ArrayList<>();
        if (mcc$modern != null) {
            var params = context.mcc$params();
            if (params == null) return out;
            for (var s : mcc$modern.getRandomItems(params, rand.nextLong())) out.add(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(s));
            return out;
        }
        if (context.addLootTable(this)) {
            for (LootPool pool : pools) pool.generateLoot(out, rand, context);
            context.removeLootTable(this);
        }
        return out;
    }

    /** Spreads the generated loot over random empty slots, splitting stacks into the free space (as 1.12 did). */
    public void fillInventory(IInventory inventory, Random rand, LootContext context) {
        List<ItemStack> loot = generateLootForPools(rand, context);
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s == null || s.isEmpty()) empty.add(i);
        }
        Collections.shuffle(empty, rand);
        shuffleItems(loot, empty.size(), rand);
        for (ItemStack stack : loot) {
            if (empty.isEmpty()) return;
            int slot = empty.remove(empty.size() - 1);
            inventory.setInventorySlotContents(slot, stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }

    private static void shuffleItems(List<ItemStack> stacks, int emptySlots, Random rand) {
        List<ItemStack> splittable = new ArrayList<>();
        stacks.removeIf(s -> s == null || s.isEmpty());
        for (var it = stacks.iterator(); it.hasNext();) {
            ItemStack s = it.next();
            if (s.getCount() > 1) {
                splittable.add(s);
                it.remove();
            }
        }
        emptySlots -= stacks.size();
        while (emptySlots > 0 && !splittable.isEmpty()) {
            ItemStack s = splittable.remove(rand.nextInt(splittable.size()));
            int n = 1 + rand.nextInt(Math.max(1, s.getCount() / 2));
            ItemStack part = s.splitStack(n);
            if (s.getCount() > 1 && rand.nextBoolean()) splittable.add(s);
            else stacks.add(s);
            if (part.getCount() > 1 && rand.nextBoolean()) splittable.add(part);
            else stacks.add(part);
            emptySlots--;
        }
        stacks.addAll(splittable);
        Collections.shuffle(stacks, rand);
    }

    public void freeze() {
        this.isFrozen = true;
        for (LootPool p : pools) p.freeze();
    }

    public boolean isFrozen() {
        return isFrozen || mcc$modern != null && mcc$modern.isFrozen();
    }

    private void checkFrozen() {
        if (isFrozen()) throw new RuntimeException("Attempted to modify LootTable after being finalized!");
    }

    public LootPool getPool(String name) {
        if (mcc$modern != null) return LootPool.mcc$view(mcc$modern.getPool(name));
        for (LootPool p : pools) if (name != null && name.equals(p.getName())) return p;
        return null;
    }

    public LootPool removePool(String name) {
        checkFrozen();
        if (mcc$modern != null) return LootPool.mcc$view(mcc$modern.removePool(name));
        for (LootPool p : pools) {
            if (name != null && name.equals(p.getName())) {
                pools.remove(p);
                return p;
            }
        }
        return null;
    }

    public void addPool(LootPool pool) {
        checkFrozen();
        if (pool == null) return;
        if (mcc$modern != null) {
            mcc$modern.addPool(pool.mcc$modern());
            return;
        }
        for (LootPool p : pools) {
            if (p == pool || p.getName() != null && p.getName().equals(pool.getName())) {
                throw new RuntimeException("Attempted to add a duplicate pool to loot table: " + pool.getName());
            }
        }
        pools.add(pool);
    }

    /** Gson adapter for loot table JSON (its pools). */
    public static class Serializer implements com.google.gson.JsonDeserializer<LootTable>, com.google.gson.JsonSerializer<LootTable> {
        @Override
        public LootTable deserialize(com.google.gson.JsonElement element, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            var json = element.getAsJsonObject();
            LootPool[] pools = json.has("pools") ? context.deserialize(json.get("pools"), LootPool[].class) : new LootPool[0];
            return new LootTable(pools);
        }

        @Override
        public com.google.gson.JsonElement serialize(LootTable table, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {
            var json = new com.google.gson.JsonObject();
            json.add("pools", context.serialize(table.pools.toArray(new LootPool[0])));
            return json;
        }
    }
}
