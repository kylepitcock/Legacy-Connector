package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Lifecycle;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/**
 * Common-side helpers of gap round 11 (1.20.1 mods on 1.21.1): loot tables, registries, portals. Reached from {@code mc-calls.tsv} and {@code mc-classes.tsv}.
 */
public final class Round11Compat {
    private Round11Compat() {}

    // ---- loot: looting and bonus functions --------------------------------------------------------------------------------------------------

    /** {@code LootingEnchantFunction.lootingMultiplier(count)}: the class is {@code EnchantedCountIncreaseFunction} now, which also needs the registries. */
    public static EnchantedCountIncreaseFunction.Builder lootingMultiplier(NumberProvider count) {
        return EnchantedCountIncreaseFunction.lootingMultiplier(MigrateContext.provider(), count);
    }

    /** {@code LootItemRandomChanceWithLootingCondition.randomChanceAndLootingBoost(base, perLevel)}. */
    public static LootItemCondition.Builder randomChanceAndLootingBoost(float base, float perLevel) {
        return LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(MigrateContext.provider(), base, perLevel);
    }

    public static LootItemConditionalFunction.Builder<?> addOreBonusCount(LegacyEnchantment e) {
        return ApplyBonusCount.addOreBonusCount(e.holder());
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(LegacyEnchantment e) {
        return ApplyBonusCount.addUniformBonusCount(e.holder());
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(LegacyEnchantment e, int multiplier) {
        return ApplyBonusCount.addUniformBonusCount(e.holder(), multiplier);
    }

    public static LootItemConditionalFunction.Builder<?> addBonusBinomialDistributionCount(LegacyEnchantment e, float probability, int extraRounds) {
        return ApplyBonusCount.addBonusBinomialDistributionCount(e.holder(), probability, extraRounds);
    }

    public static LootItemCondition.Builder bonusLevelFlatChance(LegacyEnchantment e, float[] chances) {
        return BonusLevelTableCondition.bonusLevelFlatChance(e.holder(), chances);
    }

    /** {@code EnchantWithLevelsFunction.enchantWithLevels(levels)} needs the registries in 1.21.1. */
    public static EnchantWithLevelsFunction.Builder enchantWithLevels(NumberProvider levels) {
        return EnchantWithLevelsFunction.enchantWithLevels(MigrateContext.provider(), levels);
    }

    /**
     * {@code EnchantWithLevelsFunction.Builder.allowTreasure()}. APPROXIMATION: 1.21.1 has no treasure switch (the options set decides: none = every enchantment, treasure
     * ones included), so the builder is returned unchanged; without {@code allowTreasure()} a 1.20.1 function excluded treasure enchantments.
     */
    public static EnchantWithLevelsFunction.Builder allowTreasure(EnchantWithLevelsFunction.Builder b) {
        return b;
    }

    // ---- loot: reads of fields whose type changed ---------------------------------------------------------------------------------------------

    /** {@code LootTable.pools} was an array. */
    public static LootPool[] tablePools(LootTable table) {
        return table.pools.toArray(new LootPool[0]);
    }

    /** {@code LootPool.conditions} was an array. */
    public static LootItemCondition[] poolConditions(LootPool pool) {
        return pool.conditions.toArray(new LootItemCondition[0]);
    }

    /** {@code LootPoolEntryContainer.conditions} (read through any entry class) was an array. */
    public static LootItemCondition[] entryConditions(LootPoolEntryContainer entry) {
        return entry.conditions.toArray(new LootItemCondition[0]);
    }

    /** {@code LootPoolSingletonContainer.functions} was an array. */
    public static LootItemFunction[] entryFunctions(LootPoolSingletonContainer entry) {
        return entry.functions.toArray(new LootItemFunction[0]);
    }

    /** {@code LootItem.item} is a holder now. */
    public static Item lootItemItem(LootItem entry) {
        return entry.item.value();
    }

    /**
     * {@code LootTableReference.name} (the id of the referenced table). A reference that carries a table inline (1.21.1 only) answers the empty table id:
     * APPROXIMATION, 1.20.1 had no such entries.
     */
    public static ResourceLocation nestedName(NestedLootTable entry) {
        Either<ResourceKey<LootTable>, LootTable> c = entry.contents;
        return c.left().map(ResourceKey::location).orElse(ResourceLocation.withDefaultNamespace("empty"));
    }

    /**
     * {@code LootItemRandomChanceCondition.probability}. APPROXIMATION: the chance is a number provider now; a constant answers its value, any other provider 0
     * (a context is needed to roll it).
     */
    public static float chanceProbability(LootItemRandomChanceCondition c) {
        return c.chance() instanceof ConstantValue v ? v.value() : 0f;
    }

    /** {@code new ServerPacksSource()}: the validator of 1.21.1 decides which symbolic links a pack may contain; none are allowed here. */
    public static ServerPacksSource serverPacksSource() {
        return new ServerPacksSource(new net.minecraft.world.level.validation.DirectoryValidator(path -> false));
    }

    /** {@code BlockBehaviour.Properties.drops} (the loot table id set with {@code lootFrom}/{@code noLootTable}); a resource key in 1.21.1. */
    public static ResourceLocation propsDrops(net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        return props.drops == null ? null : props.drops.location();
    }

    /** The entry array of {@code CompositeEntryBase}'s constructor as the list 1.21.1 takes. */
    public static List<LootPoolEntryContainer> entryList(LootPoolEntryContainer[] entries) {
        return entries == null ? List.of() : List.of(entries);
    }

    // ---- ingredients -------------------------------------------------------------------------------------------------------------------------

    /** {@code Ingredient.TagValue.serialize()}: the JSON of a tag ingredient entry, {@code {"tag": "<id>"}}. */
    public static JsonObject tagValueSerialize(Ingredient.TagValue value) {
        JsonObject o = new JsonObject();
        o.addProperty("tag", value.tag().location().toString());
        return o;
    }

    // ---- registries --------------------------------------------------------------------------------------------------------------------------

    /** {@code WritableRegistry.register(key, value, lifecycle)}: the lifecycle is part of the registration info now. */
    public static <T> Holder.Reference<T> register(WritableRegistry<T> registry, ResourceKey<T> key, T value, Lifecycle lifecycle) {
        return registry.register(key, value, new RegistrationInfo(Optional.empty(), lifecycle));
    }

    /** {@code BuiltInRegistries.ROOT_REGISTRY_NAME} (the id of the registry of registries). */
    public static ResourceLocation rootRegistryName() {
        return net.minecraft.core.registries.Registries.ROOT_REGISTRY_NAME;
    }

    /** {@code Util.OS.openUrl(URL)}: {@code openUri(URI)} since 1.20.2. */
    public static void openUrl(Util.OS os, URL url) {
        try {
            os.openUri(url.toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** {@code TagManager.getTagDir(registryKey)}: {@code Registries.tagsDirPath}. */
    public static String tagDir(ResourceKey<? extends Registry<?>> key) {
        return net.minecraft.core.registries.Registries.tagsDirPath(key);
    }

    // ---- blocks and portals ------------------------------------------------------------------------------------------------------------------

    /** {@code BlockItem.updateState(state, property, value)}: a private helper of 1.20.1 ({@code BlockItem} state tags); 1.21.1 reads components instead. */
    public static <T extends Comparable<T>> BlockState blockItemUpdateState(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    /**
     * {@code PortalForcer.findPortalAround(pos, isNether, border)}: 1.20.1 returned the largest rectangle of portal blocks around the closest portal position
     * ({@code findClosestPortalPosition} in 1.21.1 returns only the position).
     */
    public static Optional<BlockUtil.FoundRectangle> findPortalAround(PortalForcer forcer, BlockPos pos, boolean isNether, WorldBorder border) {
        return forcer.findClosestPortalPosition(pos, isNether, border).map(found -> {
            BlockState state = forcer.level.getBlockState(found);
            return BlockUtil.getLargestRectangleAround(found, state.getValue(NetherPortalBlock.AXIS), 21, Direction.Axis.Y, 21,
                    p -> forcer.level.getBlockState(p) == state);
        });
    }

    // ---- mixin bodies and small members ---------------------------------------------------------------------------------------------------

    /** {@code ChunkMap.isChunkInRange(x1, z1, x2, z2, distance)}: the 1.20.1 distance test of the view area (a circle-ish square), static in the old class. */
    public static boolean isChunkInRange(int x1, int z1, int x2, int z2, int distance) {
        int i = Math.max(0, Math.abs(x1 - x2) - 1);
        int j = Math.max(0, Math.abs(z1 - z2) - 1);
        long k = Math.max(0, Math.max(i, j) - 1);
        long l = Math.min(i, j);
        long sum = l * l + k * k;
        int d = distance - 1;
        return sum <= (long) d * d;
    }

    /** {@code ClientboundLoginPacket.seed()}: part of the spawn info record in 1.21.1. */
    public static long loginSeed(net.minecraft.network.protocol.game.ClientboundLoginPacket p) {
        return p.commonPlayerSpawnInfo().seed();
    }

    /** {@code ClientboundRespawnPacket.getSeed()}. */
    public static long respawnSeed(net.minecraft.network.protocol.game.ClientboundRespawnPacket p) {
        return p.commonPlayerSpawnInfo().seed();
    }

    /** {@code ArmorStand.brokenByAnything(source)}: takes the level now (private, access transformer). */
    public static void armorStandBroken(net.minecraft.world.entity.decoration.ArmorStand stand, net.minecraft.world.damagesource.DamageSource source) {
        if (stand.level() instanceof net.minecraft.server.level.ServerLevel level) stand.brokenByAnything(level, source);
    }

    /** {@code AbstractHorse.equipSaddle(sound)}: 1.21.1 takes the saddle stack. */
    public static void equipSaddle(net.minecraft.world.entity.animal.horse.AbstractHorse horse, net.minecraft.sounds.SoundSource sound) {
        horse.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), sound);
    }

    /** {@code LootTableTrigger.trigger(player, tableId)}: tables are resource keys now. */
    public static void lootTableTrigger(net.minecraft.advancements.critereon.LootTableTrigger t, net.minecraft.server.level.ServerPlayer player, ResourceLocation id) {
        t.trigger(player, ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id));
    }

    /**
     * {@code new ClientboundMapItemDataPacket(id, scale, locked, decorations, patch)}: the map id is a component, decorations and patch are optional.
     */
    public static net.minecraft.network.protocol.game.ClientboundMapItemDataPacket mapItemPacket(int id, byte scale, boolean locked,
            java.util.Collection<net.minecraft.world.level.saveddata.maps.MapDecoration> decorations,
            net.minecraft.world.level.saveddata.maps.MapItemSavedData.MapPatch patch) {
        return new net.minecraft.network.protocol.game.ClientboundMapItemDataPacket(new net.minecraft.world.level.saveddata.maps.MapId(id), scale, locked,
                decorations == null ? Optional.empty() : Optional.of(List.copyOf(decorations)), Optional.ofNullable(patch));
    }

    /** {@code scoreboard.addObjective(name, criteria, displayName, renderType)}: auto-update off and no number format, as 1.20.1 had neither. */
    public static net.minecraft.world.scores.Objective scoreboardAddObjective(net.minecraft.world.scores.Scoreboard board, String name,
            net.minecraft.world.scores.criteria.ObjectiveCriteria criteria, net.minecraft.network.chat.Component displayName,
            net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType renderType) {
        return board.addObjective(name, criteria, displayName, renderType, false, null);
    }

    /** {@code GsonHelper.convertToItem(json, member)}: a holder in 1.21.1. */
    public static Item convertToItem(com.google.gson.JsonElement json, String member) {
        return net.minecraft.util.GsonHelper.convertToItem(json, member).value();
    }

    /** {@code LootContextParamSets.getKey(set)}: the id a set is registered under (null for an unregistered one). */
    public static ResourceLocation paramSetKey(net.minecraft.world.level.storage.loot.parameters.LootContextParamSet set) {
        return net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.REGISTRY.inverse().get(set);
    }
}
