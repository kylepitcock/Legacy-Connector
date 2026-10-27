package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootConditionManager;

/**
 * 1.12.2 loot pool: rolls (plus luck-scaled bonus rolls) weighted picks among its entries when its conditions pass. A pool a mod
 * builds is legacy data that becomes one modern pool when added to a table; a pool taken from a modern table
 * ({@code LootTable.getPool}) is a view of it, and entries added to it become modern entries of that pool.
 */
public class LootPool {
    private final List<LootEntry> lootEntries;
    private final List<LootCondition> poolConditions;
    private RandomValueRange rolls;
    private RandomValueRange bonusRolls;
    private final String name;
    private boolean isFrozen;
    /** The modern pool this is a view of, or null for a pool a mod built. */
    private final net.minecraft.world.level.storage.loot.LootPool mcc$modern;

    public LootPool(LootEntry[] entries, LootCondition[] conditions, RandomValueRange rolls, RandomValueRange bonusRolls, String name) {
        this.lootEntries = new ArrayList<>(java.util.Arrays.asList(entries == null ? new LootEntry[0] : entries));
        this.poolConditions = new ArrayList<>(java.util.Arrays.asList(conditions == null ? new LootCondition[0] : conditions));
        this.rolls = rolls == null ? new RandomValueRange(1) : rolls;
        this.bonusRolls = bonusRolls == null ? new RandomValueRange(0) : bonusRolls;
        this.name = name;
        this.mcc$modern = null;
    }

    private LootPool(net.minecraft.world.level.storage.loot.LootPool modern) {
        this.lootEntries = new ArrayList<>();
        this.poolConditions = new ArrayList<>();
        this.rolls = new RandomValueRange(1);
        this.bonusRolls = new RandomValueRange(0);
        this.name = modern.getName();
        this.mcc$modern = modern;
    }

    public static LootPool mcc$view(net.minecraft.world.level.storage.loot.LootPool modern) {
        return modern == null ? null : new LootPool(modern);
    }

    public net.minecraft.world.level.storage.loot.LootPool mcc$modern() {
        return mcc$modern != null ? mcc$modern : org.kylepitcock.legacyconnector.v1_12_2.bridge.Loot1122.modernPool(this);
    }

    public boolean mcc$isView() {
        return mcc$modern != null;
    }

    protected void createLootRoll(Collection<ItemStack> stacks, Random rand, LootContext context) {
        List<LootEntry> candidates = new ArrayList<>();
        int total = 0;
        for (LootEntry entry : lootEntries) {
            if (LootConditionManager.testAllConditions(entry.conditions, rand, context)) {
                int w = entry.getEffectiveWeight(context.getLuck());
                if (w > 0) {
                    candidates.add(entry);
                    total += w;
                }
            }
        }
        if (total == 0 || candidates.isEmpty()) return;
        int pick = rand.nextInt(total);
        for (LootEntry entry : candidates) {
            pick -= entry.getEffectiveWeight(context.getLuck());
            if (pick < 0) {
                entry.addLoot(stacks, rand, context);
                return;
            }
        }
    }

    public void generateLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
        if (mcc$modern != null) {
            var params = context.mcc$params();
            if (params == null) return;
            var modernContext = new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(java.util.Optional.empty());
            mcc$modern.addRandomItems(s -> stacks.add(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(s)), modernContext);
            return;
        }
        if (!LootConditionManager.testAllConditions(poolConditions.toArray(new LootCondition[0]), rand, context)) return;
        int n = rolls.generateInt(rand) + (int) Math.floor(bonusRolls.generateFloat(rand) * context.getLuck());
        for (int i = 0; i < n; i++) createLootRoll(stacks, rand, context);
    }

    public List<LootEntry> mcc$entries() {
        return lootEntries;
    }

    public List<LootCondition> mcc$conditions() {
        return poolConditions;
    }

    public void freeze() {
        this.isFrozen = true;
    }

    public boolean isFrozen() {
        return isFrozen || mcc$modern != null && mcc$modern.isFrozen();
    }

    private void checkFrozen() {
        if (isFrozen()) throw new RuntimeException("Attempted to modify LootPool after being frozen!");
    }

    public String getName() {
        return name;
    }

    public RandomValueRange getRolls() {
        return rolls;
    }

    public RandomValueRange getBonusRolls() {
        return bonusRolls;
    }

    public void setRolls(RandomValueRange rolls) {
        checkFrozen();
        this.rolls = rolls;
        if (mcc$modern != null) mcc$modern.setRolls(rolls.mcc$modern());
    }

    public void setBonusRolls(RandomValueRange bonusRolls) {
        checkFrozen();
        this.bonusRolls = bonusRolls;
        if (mcc$modern != null) mcc$modern.setBonusRolls(bonusRolls.mcc$modern());
    }

    public LootEntry getEntry(String name) {
        for (LootEntry e : lootEntries) if (name != null && name.equals(e.getEntryName())) return e;
        return null;
    }

    public LootEntry removeEntry(String name) {
        checkFrozen();
        for (LootEntry e : lootEntries) {
            if (name != null && name.equals(e.getEntryName())) {
                lootEntries.remove(e);
                if (mcc$modern != null) org.kylepitcock.legacyconnector.v1_12_2.bridge.Loot1122.removeEntry(mcc$modern, e);
                return e;
            }
        }
        return null;
    }

    public void addEntry(LootEntry entry) {
        checkFrozen();
        for (LootEntry e : lootEntries) {
            if (e == entry || entry.getEntryName() != null && entry.getEntryName().equals(e.getEntryName())) {
                throw new RuntimeException("Attempted to add a duplicate entry to pool: " + entry.getEntryName());
            }
        }
        lootEntries.add(entry);
        if (mcc$modern != null) org.kylepitcock.legacyconnector.v1_12_2.bridge.Loot1122.appendEntry(mcc$modern, entry);
    }

    /** Gson adapter for pools in loot table JSON (rolls and bonus rolls; entries through {@link LootEntry.Serializer}). */
    public static class Serializer implements com.google.gson.JsonDeserializer<LootPool>, com.google.gson.JsonSerializer<LootPool> {
        @Override
        public LootPool deserialize(com.google.gson.JsonElement element, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            var json = element.getAsJsonObject();
            String name = json.has("name") ? json.get("name").getAsString() : "pool";
            LootEntry[] entries = json.has("entries") ? context.deserialize(json.get("entries"), LootEntry[].class) : new LootEntry[0];
            RandomValueRange rolls = json.has("rolls") ? context.deserialize(json.get("rolls"), RandomValueRange.class) : new RandomValueRange(1);
            RandomValueRange bonus = json.has("bonus_rolls") ? context.deserialize(json.get("bonus_rolls"), RandomValueRange.class) : new RandomValueRange(0);
            return new LootPool(entries, new LootCondition[0], rolls, bonus, name);
        }

        @Override
        public com.google.gson.JsonElement serialize(LootPool pool, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {
            var json = new com.google.gson.JsonObject();
            if (pool.name != null) json.addProperty("name", pool.name);
            json.add("rolls", context.serialize(pool.rolls));
            json.add("bonus_rolls", context.serialize(pool.bonusRolls));
            json.add("entries", context.serialize(pool.lootEntries.toArray(new LootEntry[0])));
            return json;
        }
    }
}
