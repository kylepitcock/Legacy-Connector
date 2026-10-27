package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootEntry;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootPool;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * 1.12.2 loot in the modern loot system. A legacy entry becomes a modern pool entry with its own weight and quality that runs the
 * legacy entry (its conditions, item and functions: mods' own function and condition classes run unchanged) against a legacy view
 * of the modern loot context; a legacy pool becomes a modern pool of such entries; the modern {@code LootTableLoadEvent} is posted
 * as 1.12's with a view of the modern table.
 */
public final class Loot1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Loot1122");
    private static final java.util.concurrent.atomic.AtomicInteger FAILURES = new java.util.concurrent.atomic.AtomicInteger();
    private static boolean registered;

    private Loot1122() {}

    /** A modern entry that runs a legacy one. */
    public static final class LegacyEntry extends LootPoolSingletonContainer {
        final LootEntry legacy;

        LegacyEntry(LootEntry legacy, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
            super(weight, quality, conditions, functions);
            this.legacy = legacy;
        }

        static Builder<?> builder(LootEntry legacy) {
            int weight = fieldInt(legacy, "weight", 1);
            int quality = fieldInt(legacy, "quality", 0);
            var builder = simpleBuilder((w, q, c, f) -> new LegacyEntry(legacy, w, q, c, f));
            builder.setWeight(weight).setQuality(quality);
            LootCondition[] conditions = conditionsOf(legacy);
            if (conditions.length > 0) builder.when(() -> condition(conditions));
            return builder;
        }

        @Override
        public LootPoolEntryType getType() {
            return net.minecraft.world.level.storage.loot.entries.LootPoolEntries.ITEM;
        }

        @Override
        protected void createItemStack(Consumer<ItemStack> out, LootContext context) {
            try {
                List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> stacks = new ArrayList<>();
                legacy.addLoot(stacks, random(context), legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext.mcc$of(context));
                for (var s : stacks) {
                    var modern = Stacks1122.toModern(s);
                    if (!modern.isEmpty()) out.accept(modern);
                }
            } catch (RuntimeException | LinkageError e) {
                failed(e);
            }
        }
    }

    private static java.util.Random random(LootContext context) {
        return new java.util.Random(context.getRandom().nextLong());
    }

    private static void failed(Throwable e) {
        int n = FAILURES.incrementAndGet();
        if (n <= 3 || n % 200 == 0) LOG.warn("A 1.12.2 loot entry failed ({} failures so far)", n, e);
    }

    private static int fieldInt(LootEntry entry, String name, int fallback) {
        try {
            var f = LootEntry.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(entry);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return fallback;
        }
    }

    private static LootCondition[] conditionsOf(LootEntry entry) {
        try {
            var f = LootEntry.class.getDeclaredField("conditions");
            f.setAccessible(true);
            LootCondition[] c = (LootCondition[]) f.get(entry);
            return c == null ? new LootCondition[0] : c;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new LootCondition[0];
        }
    }

    /** A modern condition testing legacy conditions. */
    public static LootItemCondition condition(LootCondition[] conditions) {
        return new LootItemCondition() {
            @Override
            public net.minecraft.world.level.storage.loot.predicates.LootItemConditionType getType() {
                return net.minecraft.world.level.storage.loot.predicates.LootItemConditions.ALL_OF;
            }

            @Override
            public boolean test(LootContext context) {
                try {
                    return LootConditionManager.testAllConditions(conditions, random(context),
                            legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext.mcc$of(context));
                } catch (RuntimeException | LinkageError e) {
                    failed(e);
                    return false;
                }
            }
        };
    }

    /** The modern pool of a pool a mod built. */
    public static net.minecraft.world.level.storage.loot.LootPool modernPool(LootPool legacy) {
        var b = net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(legacy.getRolls().mcc$modern())
                .setBonusRolls(legacy.getBonusRolls().mcc$modern());
        if (legacy.getName() != null) b.name(legacy.getName());
        for (LootEntry e : legacy.mcc$entries()) b.add(LegacyEntry.builder(e));
        LootCondition[] conditions = legacy.mcc$conditions().toArray(new LootCondition[0]);
        if (conditions.length > 0) b.when(() -> condition(conditions));
        return b.build();
    }

    @SuppressWarnings("unchecked")
    private static List<LootPoolEntryContainer> entries(net.minecraft.world.level.storage.loot.LootPool pool) throws ReflectiveOperationException {
        var f = net.minecraft.world.level.storage.loot.LootPool.class.getDeclaredField("entries");
        f.setAccessible(true);
        return (List<LootPoolEntryContainer>) f.get(pool);
    }

    private static void setEntries(net.minecraft.world.level.storage.loot.LootPool pool, List<LootPoolEntryContainer> entries) throws ReflectiveOperationException {
        var f = net.minecraft.world.level.storage.loot.LootPool.class.getDeclaredField("entries");
        f.setAccessible(true);
        try {
            f.set(pool, entries);
        } catch (IllegalAccessException e) {
            // final field: through Unsafe
            var u = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            u.setAccessible(true);
            var unsafe = (sun.misc.Unsafe) u.get(null);
            unsafe.putObject(pool, unsafe.objectFieldOffset(f), entries);
        }
    }

    /** Adds a legacy entry to a modern pool (a mod editing a vanilla or data pack table in the load event). */
    public static void appendEntry(net.minecraft.world.level.storage.loot.LootPool pool, LootEntry entry) {
        try {
            List<LootPoolEntryContainer> list = new ArrayList<>(entries(pool));
            list.add(LegacyEntry.builder(entry).build());
            setEntries(pool, com.google.common.collect.ImmutableList.copyOf(list));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Cannot add a 1.12.2 loot entry {} to pool {}: {}", entry.getEntryName(), pool.getName(), e.toString());
        }
    }

    public static void removeEntry(net.minecraft.world.level.storage.loot.LootPool pool, LootEntry entry) {
        try {
            List<LootPoolEntryContainer> list = new ArrayList<>(entries(pool));
            list.removeIf(e -> e instanceof LegacyEntry l && l.legacy == entry);
            setEntries(pool, com.google.common.collect.ImmutableList.copyOf(list));
        } catch (ReflectiveOperationException | RuntimeException e) {
            // stays
        }
    }

    /** Posts 1.12's LootTableLoadEvent for each modern table load (only when a legacy handler listens). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOW, (net.neoforged.neoforge.event.LootTableLoadEvent e) -> {
            var bus = legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
            if (!bus.mcc$hasListeners(legacyconnector.v1_12_2.net.minecraftforge.event.LootTableLoadEvent.class)) return;
            var view = legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootTable.mcc$view(e.getTable());
            var name = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(e.getName().getNamespace(), e.getName().getPath());
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.LootTableLoadEvent(name, view,
                    legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootTableManager.mcc$of(net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()));
            try {
                if (bus.post(event)) {
                    e.setTable(net.minecraft.world.level.storage.loot.LootTable.EMPTY);
                    return;
                }
            } catch (RuntimeException | LinkageError ex) {
                failed(ex);
                return;
            }
            if (event.getTable() != view) {
                e.setTable(event.getTable() == null ? net.minecraft.world.level.storage.loot.LootTable.EMPTY : event.getTable().mcc$modern());
            }
        });
    }
}
