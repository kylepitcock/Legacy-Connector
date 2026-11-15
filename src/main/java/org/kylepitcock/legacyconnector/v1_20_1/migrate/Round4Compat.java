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
}
