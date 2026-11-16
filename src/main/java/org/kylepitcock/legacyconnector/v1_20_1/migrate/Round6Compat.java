package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/**
 * Signature changes of Minecraft 1.20.1 to 1.21.1 found by the ATM 9 ranking of 2026-11-15, round 6 (see {@code mc-calls.tsv}).
 */
public final class Round6Compat {
    private Round6Compat() {}

    /**
     * {@code InventoryChangeTrigger.TriggerInstance.hasItems(ItemLike...)} returned the condition object; 1.21.1 returns a {@code Criterion} holding
     * it. The same condition is built directly (any slot counts, no player predicate).
     */
    public static InventoryChangeTrigger.TriggerInstance hasItems(ItemLike[] items) {
        ItemPredicate[] predicates = new ItemPredicate[items.length];
        for (int i = 0; i < items.length; i++) predicates[i] = ItemPredicate.Builder.item().of(items[i]).build();
        return hasItems(predicates);
    }

    /** {@code InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate...)}. */
    public static InventoryChangeTrigger.TriggerInstance hasItems(ItemPredicate[] predicates) {
        return new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(predicates));
    }

    /**
     * Argument producer for {@code new RegionFileStorage(folder, sync)}: 1.21.1 also wants a {@code RegionStorageInfo} (level name, dimension, kind of
     * data) that only names the storage in log lines and error reports. Approximation: the folder name stands for the level, the dimension is the
     * overworld's key.
     */
    public static net.minecraft.world.level.chunk.storage.RegionStorageInfo regionInfo(java.nio.file.Path folder) {
        java.nio.file.Path name = folder.getFileName();
        return new net.minecraft.world.level.chunk.storage.RegionStorageInfo(name == null ? "legacy" : name.toString(), net.minecraft.world.level.Level.OVERWORLD, "chunk");
    }

    /**
     * {@code new Criterion(instance)}: 1.21.1's record also holds the trigger, which a condition no longer knows. Found through the id of a mod's own
     * condition ({@link LegacyCriterionInstance}) or, for an inventory condition, the vanilla trigger; other conditions are an error.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.advancements.Criterion<?> newCriterion(CriterionTriggerInstance instance) {
        net.minecraft.advancements.CriterionTrigger trigger = null;
        ResourceLocation id = criterionId(instance);
        if (id != null) trigger = net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES.get(id);
        if (trigger == null) throw new IllegalArgumentException("No advancement trigger known for the condition " + instance);
        return new net.minecraft.advancements.Criterion(trigger, instance);
    }

    /** {@code WorldGenRegion.writeRadiusCutoff} (field read): the radius around the centre chunk blocks may be written in, now held by the generation step. */
    public static int writeRadius(net.minecraft.server.level.WorldGenRegion region) {
        return region.generatingStep.blockStateWriteRadius();
    }

    /** {@code ServerLevel.addDuringPortalTeleport(player)} / {@code addDuringCommandTeleport(player)}: one method for every teleport now. */
    public static void addDuringTeleport(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        level.addDuringTeleport(player);
    }

    /**
     * {@code CriterionTriggerInstance.getCriterion()}: the id of the trigger the condition belongs to. 1.21.1 conditions do not know their trigger; a
     * mod's own condition ({@link LegacyCriterionInstance}) carries the id it was built with, an inventory condition answers the vanilla trigger's id, the rest
     * answer null.
     */
    public static ResourceLocation criterionId(CriterionTriggerInstance instance) {
        if (instance instanceof LegacyCriterionInstance legacy) return legacy.getCriterion();
        if (instance instanceof InventoryChangeTrigger.TriggerInstance) return net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES.getKey(net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED);
        return null;
    }
}
