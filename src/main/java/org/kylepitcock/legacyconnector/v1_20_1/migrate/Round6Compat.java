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

    /**
     * Argument producer for {@code new SimpleCookingSerializer(baker, time)} and {@code super(baker, time)} of a mod's own serializer class: the 1.21.1
     * factory has no recipe id; the 1.20.1 constructor gets {@link RoundCompat}'s unknown id.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.world.item.crafting.AbstractCookingRecipe.Factory cookingFactory(LegacyCookieBaker baker) {
        return (group, category, ingredient, result, xp, time) -> baker.create(RoundCompat.UNKNOWN_RECIPE, (String) group,
                (net.minecraft.world.item.crafting.CookingBookCategory) category, (net.minecraft.world.item.crafting.Ingredient) ingredient,
                (net.minecraft.world.item.ItemStack) result, (Float) xp, (Integer) time);
    }

    /** {@code Util.isBlank(string)}: gone (use the JDK). */
    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    /** {@code Painting.storeVariant(tag, variant)}: the variant's registry id under {@code "variant"}, as 1.20.1 wrote it. */
    public static void storePaintingVariant(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.Holder<net.minecraft.world.entity.decoration.PaintingVariant> variant) {
        variant.unwrapKey().ifPresent(key -> tag.putString("variant", key.location().toString()));
    }

    /** {@code Entity.portalEntrancePos} (field read): where the entity entered the portal it is in, null outside one. */
    public static net.minecraft.core.BlockPos portalEntrancePos(net.minecraft.world.entity.Entity entity) {
        return entity.portalProcess == null ? null : entity.portalProcess.getEntryPosition();
    }

    /**
     * {@code entity.portalEntrancePos = pos} (field write). Approximation: only an entity that is inside a portal this tick has a portal process to
     * update; the 1.20.1 code could also set the field ahead of the teleport, which has no 1.21.1 counterpart.
     */
    public static void setPortalEntrancePos(net.minecraft.world.entity.Entity entity, net.minecraft.core.BlockPos pos) {
        if (entity.portalProcess != null && pos != null) entity.portalProcess.updateEntryPosition(pos);
    }

    /** {@code ArmorItem.knockbackResistance} (protected field of 1.20.1): the material's value. */
    public static float armorKnockbackResistance(net.minecraft.world.item.ArmorItem item) {
        return item.getMaterial().value().knockbackResistance();
    }

    /** {@code LootPool.entries} (field read, an array in 1.20.1): a copy of the pool's entry list. */
    public static net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[] poolEntries(net.minecraft.world.level.storage.loot.LootPool pool) {
        return pool.entries.toArray(new net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[0]);
    }

    /** {@code LootPool.entries = array} (field write): the pool draws from the new entries (it reads its list on every roll). */
    public static void setPoolEntries(net.minecraft.world.level.storage.loot.LootPool pool, net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[] entries) {
        pool.entries = new java.util.ArrayList<>(java.util.Arrays.asList(entries));
    }

    /** {@code Block.appendHoverText(stack, level, tooltip, flag)} called on a block: the 1.21.1 method with an empty tooltip context. */
    public static void blockHover(net.minecraft.world.level.block.Block block, net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.BlockGetter level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        block.appendHoverText(stack, net.minecraft.world.item.Item.TooltipContext.EMPTY, tooltip, flag);
    }
}
