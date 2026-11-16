package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.EffectCures;

/**
 * Signature changes of Minecraft 1.20.1 to 1.21.1 found by the ATM 9 ranking of 2026-11-15 (see {@code mc-calls.tsv}, {@code mc-fields.tsv}): small queries
 * whose answer 1.21.1 still has, in another form.
 */
public final class Round4Compat {
    private Round4Compat() {}

    /** {@code IForgeBlockGetter.getExistingBlockEntity}: the block entity of a loaded chunk, never loading or creating one. */
    public static BlockEntity existingBlockEntity(BlockGetter getter, BlockPos pos) {
        if (getter instanceof Level level) {
            if (!level.hasChunkAt(pos)) return null;
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        }
        return getter.getBlockEntity(pos);
    }

    /** {@code MapItem.getMapId(stack)}: the map number, or null when the stack carries none (the id is a component in 1.21.1). */
    public static Integer mapId(ItemStack stack) {
        net.minecraft.world.level.saveddata.maps.MapId id = stack.get(DataComponents.MAP_ID);
        return id == null ? null : id.id();
    }

    /** {@code Sheep.getColorArray(color)}: the sheep wool color as float RGB (1.21.1 stores it as an int). */
    public static float[] sheepColorArray(DyeColor color) {
        int rgb = net.minecraft.world.entity.animal.Sheep.getColor(color);
        return new float[] {((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F};
    }

    /** {@code new NbtAccounter(long)}: 1.21.1 builds the quota through a factory. */
    public static NbtAccounter newNbtAccounter(long quota) {
        return NbtAccounter.create(quota);
    }

    /** {@code new ClientboundAddEntityPacket(entity, data)}: 1.21.1 also takes the position the entity is spawned at. */
    public static ClientboundAddEntityPacket newAddEntityPacket(Entity entity, int data) {
        return new ClientboundAddEntityPacket(entity, data, entity.blockPosition());
    }

    /**
     * {@code CrossbowItem.getChargeDuration(stack)}: 25 ticks minus 5 per Quick Charge level. 1.21.1 asks the stack's enchantment effects with the shooter in
     * hand; a caller without a shooter gets the vanilla Quick Charge result (modded charge-time effects are not included).
     */
    public static int crossbowChargeDuration(ItemStack stack) {
        int level = 0;
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(Enchantments.QUICK_CHARGE)) level = entry.getIntValue();
        }
        return Math.max(0, 25 - 5 * level);
    }

    /** {@code MobEffectInstance.isCurativeItem(stack)}: 1.21.1 keeps cures, not items; the milk bucket is the item of the milk cure. */
    public static boolean isCurativeItem(MobEffectInstance effect, ItemStack stack) {
        return stack.is(Items.MILK_BUCKET) && effect.getCures().contains(EffectCures.MILK);
    }

    /** {@code LecternBlock.tryPlaceBook(Entity, ...)}: 1.21.1 takes the living entity that places the book (null for another kind of entity). */
    public static boolean tryPlaceBook(Entity entity, Level level, BlockPos pos, BlockState state, ItemStack book) {
        return LecternBlock.tryPlaceBook(entity instanceof LivingEntity living ? living : null, level, pos, state, book);
    }

    // ------------------------------------------------------------------ enchanting table hook

    /** {@code item.canApplyAtEnchantingTable(stack, enchantment)}: the item's primary-item hook (a mod's override is bridged to it). */
    public static boolean itemCanApplyAtEnchantingTable(net.minecraft.world.item.Item item, ItemStack stack, LegacyEnchantment enchantment) {
        return item.isPrimaryItemFor(stack, HolderCompat.holder(enchantment));
    }

    /** {@code super.canApplyAtEnchantingTable(stack, enchantment)} of a mod's item: the vanilla answer, the enchantment's primary items. */
    public static boolean superCanApplyAtEnchantingTable(Object item, ItemStack stack, LegacyEnchantment enchantment) {
        return HolderCompat.holder(enchantment).value().isPrimaryItem(stack);
    }

    /** {@code stack.canApplyAtEnchantingTable(enchantment)} through the IItemStackExtension interface. */
    public static boolean stackCanApplyAtEnchantingTable(ItemStack stack, LegacyEnchantment enchantment) {
        return stack.isPrimaryItemFor(HolderCompat.holder(enchantment));
    }

    /** {@code LevelChunk.getBlockEntityNbtForSaving(pos)}: 1.21.1 also takes the registry provider (the chunk's level has it). */
    public static net.minecraft.nbt.CompoundTag blockEntityNbtForSaving(LevelChunk chunk, BlockPos pos) {
        return chunk.getBlockEntityNbtForSaving(pos, chunk.getLevel().registryAccess());
    }

    // ------------------------------------------------------------------ jigsaw structures

    /**
     * {@code JigsawPlacement.addPieces(context, pool, jigsawName, depth, pos, expansionHack, heightmap, maxDistance)}: 1.21.1 added pool aliases, the dimension
     * padding and the liquid settings; the 1.20.1 behaviour is no aliases, no padding and waterlogging applied.
     */
    public static java.util.Optional<net.minecraft.world.level.levelgen.structure.Structure.GenerationStub> addPieces(
            net.minecraft.world.level.levelgen.structure.Structure.GenerationContext context,
            net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool> pool,
            java.util.Optional<net.minecraft.resources.ResourceLocation> jigsawName, int depth, BlockPos pos, boolean expansionHack,
            java.util.Optional<net.minecraft.world.level.levelgen.Heightmap.Types> heightmap, int maxDistance) {
        return net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement.addPieces(context, pool, jigsawName, depth, pos, expansionHack, heightmap,
                maxDistance, net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup.EMPTY,
                net.minecraft.world.level.levelgen.structure.pools.DimensionPadding.ZERO,
                net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.APPLY_WATERLOGGING);
    }

    /** The liquid settings parameter that single pool elements gained in 1.21.1 (the 1.20.1 behaviour: waterlogging applied). */
    public static java.util.Optional<net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings> defaultLiquid(
            net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection projection) {
        return java.util.Optional.of(net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.APPLY_WATERLOGGING);
    }

    // ------------------------------------------------------------------ loot entries

    /** The condition array of a 1.20.1 loot entry constructor as the list 1.21.1 takes. */
    public static java.util.List<net.minecraft.world.level.storage.loot.predicates.LootItemCondition> conditionList(
            net.minecraft.world.level.storage.loot.predicates.LootItemCondition[] conditions) {
        return conditions == null ? java.util.List.of() : java.util.List.of(conditions);
    }

    /** The function array of a 1.20.1 loot entry constructor as the list 1.21.1 takes. */
    public static java.util.List<net.minecraft.world.level.storage.loot.functions.LootItemFunction> functionList(
            net.minecraft.world.level.storage.loot.functions.LootItemFunction[] functions) {
        return functions == null ? java.util.List.of() : java.util.List.of(functions);
    }

    // ------------------------------------------------------------------ tooltip hide flags

    /** {@code stack.hideTooltipPart(part)}: the hide flag of 1.20.1 is the {@code show_in_tooltip} switch of the matching component in 1.21.1. */
    public static void hideTooltipPart(ItemStack stack, LegacyTooltipPart part) {
        switch (part) {
            case ENCHANTMENTS -> {
                var e = stack.get(DataComponents.ENCHANTMENTS);
                if (e != null) stack.set(DataComponents.ENCHANTMENTS, e.withTooltip(false));
                var s = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (s != null) stack.set(DataComponents.STORED_ENCHANTMENTS, s.withTooltip(false));
            }
            case MODIFIERS -> stack.set(DataComponents.ATTRIBUTE_MODIFIERS, stack.getAttributeModifiers().withTooltip(false));
            case UNBREAKABLE -> {
                var u = stack.get(DataComponents.UNBREAKABLE);
                if (u != null) stack.set(DataComponents.UNBREAKABLE, u.withTooltip(false));
            }
            case CAN_DESTROY -> {
                var p = stack.get(DataComponents.CAN_BREAK);
                if (p != null) stack.set(DataComponents.CAN_BREAK, p.withTooltip(false));
            }
            case CAN_PLACE_ON -> {
                var p = stack.get(DataComponents.CAN_PLACE_ON);
                if (p != null) stack.set(DataComponents.CAN_PLACE_ON, p.withTooltip(false));
            }
            case ADDITIONAL -> stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
            case DYE -> {
                var d = stack.get(DataComponents.DYED_COLOR);
                if (d != null) stack.set(DataComponents.DYED_COLOR, d.withTooltip(false));
            }
            case UPGRADES -> {
                var trim = stack.get(DataComponents.TRIM);
                if (trim != null) stack.set(DataComponents.TRIM, trim.withTooltip(false));
            }
        }
    }

    // ------------------------------------------------------------------ items and containers

    /** {@code item.appendHoverText(stack, level, lines, flag)}: 1.21.1 takes a tooltip context (the level is all a 1.20.1 caller can give). */
    public static void itemAppendHoverText(net.minecraft.world.item.Item item, ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> lines,
                                           net.minecraft.world.item.TooltipFlag flag) {
        item.appendHoverText(stack, level == null ? net.minecraft.world.item.Item.TooltipContext.EMPTY : net.minecraft.world.item.Item.TooltipContext.of(level), lines, flag);
    }

    /** {@code ItemUtils.onContainerDestroyed(entity, stacks)}: takes the stacks as an iterable now. */
    public static Iterable<ItemStack> iterable(java.util.stream.Stream<ItemStack> stacks) {
        return stacks::iterator;
    }

    /** {@code NbtAccounter.UNLIMITED}. */
    public static NbtAccounter unlimitedAccounter() {
        return NbtAccounter.unlimitedHeap();
    }

    /** {@code BundleItem.getContentWeight(stack)}: the bundle's fill in 64ths (1.21.1 keeps it as a fraction of one). */
    public static int bundleContentWeight(ItemStack stack) {
        var contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? 0 : (int) Math.floor(contents.weight().doubleValue() * 64.0);
    }

    /** {@code BundleItem.getWeight(stack)}: the weight of one stack in 64ths; a filled bundle counts 4 plus its content. */
    public static int bundleItemWeight(ItemStack stack) {
        var contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) return 4 + bundleContentWeight(stack);
        return 64 / stack.getMaxStackSize();
    }

    // ------------------------------------------------------------------ predicates, registries, entities

    /** {@code StatePropertiesPredicate.fromJson(json)}: a record with a codec now. */
    public static net.minecraft.advancements.critereon.StatePropertiesPredicate statePropertiesFromJson(com.google.gson.JsonElement json) {
        return net.minecraft.advancements.critereon.StatePropertiesPredicate.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                .getOrThrow(com.google.gson.JsonParseException::new);
    }

    /** {@code predicate.serializeToJson()}. */
    public static com.google.gson.JsonElement statePropertiesToJson(net.minecraft.advancements.critereon.StatePropertiesPredicate predicate) {
        return net.minecraft.advancements.critereon.StatePropertiesPredicate.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, predicate)
                .getOrThrow(IllegalStateException::new);
    }

    /** {@code MinMaxBounds.Ints.serializeToJson()}. */
    public static com.google.gson.JsonElement intsToJson(net.minecraft.advancements.critereon.MinMaxBounds.Ints bounds) {
        return net.minecraft.advancements.critereon.MinMaxBounds.Ints.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, bounds)
                .getOrThrow(IllegalStateException::new);
    }

    /** {@code BuiltInRegistries.BANNER_PATTERN}: a data-driven registry now, looked up in the running game's registries. */
    public static net.minecraft.core.Registry<BannerPattern> bannerPatternRegistry() {
        return MigrateContext.registries().registryOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN);
    }

    /** {@code AbstractArrow.setShotFromCrossbow(flag)}: 1.21.1 derives the flag from the weapon that fired the arrow; there is nothing to set (soft link). */
    public static void setShotFromCrossbow(net.minecraft.world.entity.projectile.AbstractArrow arrow, boolean flag) {}

    /** {@code CrossbowItem.containsChargedProjectile(crossbow, item)}. */
    public static boolean containsChargedProjectile(ItemStack crossbow, net.minecraft.world.item.Item item) {
        var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
        return charged != null && charged.contains(item);
    }

    /** {@code ArmorTrim.getTrim(access, stack)}: the trim component. */
    public static java.util.Optional<net.minecraft.world.item.armortrim.ArmorTrim> getTrim(net.minecraft.core.RegistryAccess access, ItemStack stack) {
        return java.util.Optional.ofNullable(stack.get(DataComponents.TRIM));
    }

    /** {@code crashReport.getFriendlyReport()}: 1.21.1 asks for the kind of report. */
    public static String friendlyReport(net.minecraft.CrashReport report) {
        return report.getFriendlyReport(net.minecraft.ReportType.CRASH);
    }

    /** {@code FinalizeSpawnEvent.getSpawner()}: the spawner of a spawner block entity, null when the mob came from elsewhere. */
    public static net.minecraft.world.level.BaseSpawner finalizeSpawnSpawner(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event) {
        var either = event.getSpawner();
        if (either == null) return null;
        return either.left().filter(be -> be instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity)
                .map(be -> ((net.minecraft.world.level.block.entity.SpawnerBlockEntity) be).getSpawner()).orElse(null);
    }

    /** {@code evaluator.getBlockPathType(getter, x, y, z, mob)}: the path type of the block, asked through a pathfinding context of the mob. */
    public static net.minecraft.world.level.pathfinder.PathType blockPathType(net.minecraft.world.level.pathfinder.NodeEvaluator evaluator,
                                                                          net.minecraft.world.level.BlockGetter getter, int x, int y, int z,
                                                                          net.minecraft.world.entity.Mob mob) {
        return evaluator.getPathTypeOfMob(new net.minecraft.world.level.pathfinder.PathfindingContext(mob.level(), mob), x, y, z, mob);
    }

    /** {@code state.getBeaconColorMultiplier(level, pos, beaconPos)}: the tint as float RGB, null when the block does not tint the beam. */
    public static float[] beaconColor(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos, BlockPos beacon) {
        Integer rgb = state.getBeaconColorMultiplier(level, pos, beacon);
        if (rgb == null) return null;
        return new float[] {((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F};
    }

    /** {@code entity.getLootTable()}: the table's name (1.21.1 keeps a resource key). */
    public static net.minecraft.resources.ResourceLocation lootTableLocation(LivingEntity entity) {
        return entity.getLootTable().location();
    }

    /** {@code super.getLootTable()} of a mod's entity: the default table of its type. */
    public static net.minecraft.resources.ResourceLocation superLootTableLocation(LivingEntity entity) {
        return entity.getType().getDefaultLootTable().location();
    }

    /** The resource key 1.21.1 wants for a loot table name a bridged {@code getLootTable} override returned. */
    public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootTableKey(net.minecraft.resources.ResourceLocation location) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, location);
    }

    // ------------------------------------------------------------------ fluid stacks on the network buffer

    /** {@code buf.writeFluidStack(stack)}: the stack codec wants the registry-aware buffer; a plain one receives the bytes. */
    public static void writeFluidStack(net.minecraft.network.FriendlyByteBuf buf, net.neoforged.neoforge.fluids.FluidStack stack) {
        if (buf instanceof net.minecraft.network.RegistryFriendlyByteBuf reg) {
            net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC.encode(reg, stack);
            return;
        }
        var out = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), MigrateContext.registries());
        net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC.encode(out, stack);
        buf.writeBytes(out);
    }

    /** {@code buf.readFluidStack()}. */
    public static net.neoforged.neoforge.fluids.FluidStack readFluidStack(net.minecraft.network.FriendlyByteBuf buf) {
        var reg = Round3Compat.registryBuffer(buf);
        int before = reg.readerIndex();
        var stack = net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC.decode(reg);
        if (reg != buf) buf.readerIndex(buf.readerIndex() + (reg.readerIndex() - before));
        return stack;
    }
}
