package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Small signature changes between 1.20.1 and 1.21.1 that the ATM 9 gap report of 2026-11-13 listed (rows in {@code mc-calls.tsv}): constructors that
 * gained a parameter, loot table ids that became resource keys, records that lost their getters.
 */
public final class GapCompat {
    private GapCompat() {}

    /** {@code new BlockSourceImpl(level, pos)}: the dispenser source is a record now; the state and block entity are read from the level. */
    public static net.minecraft.core.dispenser.BlockSource newBlockSource(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return new net.minecraft.core.dispenser.BlockSource(level, pos, level.getBlockState(pos), be instanceof DispenserBlockEntity d ? d : null);
    }

    /** {@code StructureManager.getStructureWithPieceAt(pos, ResourceKey<Structure>)}: the key is looked up in the structure registry. */
    public static StructureStart structureWithPieceAt(StructureManager manager, BlockPos pos, ResourceKey<?> key) {
        @SuppressWarnings("unchecked")
        Structure structure = manager.registryAccess().registryOrThrow(Registries.STRUCTURE).get((ResourceKey<Structure>) key);
        return structure == null ? StructureStart.INVALID_START : manager.getStructureWithPieceAt(pos, structure);
    }

    private static ResourceKey<LootTable> table(ResourceLocation id) {
        return ResourceKey.create(Registries.LOOT_TABLE, id);
    }

    /** {@code BrushableBlockEntity.setLootTable(ResourceLocation, long)}. */
    public static void setLootTable(BrushableBlockEntity entity, ResourceLocation id, long seed) {
        entity.setLootTable(table(id), seed);
    }

    /** {@code AbstractMinecartContainer.setLootTable(ResourceLocation, long)} (chest, hopper minecarts). */
    public static void setLootTable(net.minecraft.world.entity.vehicle.AbstractMinecartContainer minecart, ResourceLocation id, long seed) {
        minecart.setLootTable(table(id), seed);
    }

    /** {@code new SpawnData(entityTag, customSpawnRules)}: 1.21.1 added the equipment table (none). */
    public static net.minecraft.world.level.SpawnData newSpawnData(CompoundTag entity, Optional<net.minecraft.world.level.SpawnData.CustomSpawnRules> rules) {
        return new net.minecraft.world.level.SpawnData(entity, rules, Optional.empty());
    }

    /** The jigsaw piece constructor without the liquid settings 1.21 added (the default of the vanilla pools). */
    public static net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece newPoolPiece(
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates,
            net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement element, BlockPos pos, int groundLevelDelta,
            net.minecraft.world.level.block.Rotation rotation, net.minecraft.world.level.levelgen.structure.BoundingBox box) {
        return new net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece(templates, element, pos, groundLevelDelta, rotation, box,
                LiquidSettings.APPLY_WATERLOGGING);
    }

    /** {@code Player.triggerRecipeCrafted(Recipe, List)}: 1.21.1 takes the holder (the recipe's id is looked up in the recipe manager). */
    public static void triggerRecipeCrafted(Player player, Recipe<?> recipe, List<ItemStack> items) {
        ResourceLocation id = RecipeCompat.getId(recipe);
        if (id == null) id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "unknown");
        player.triggerRecipeCrafted(new RecipeHolder<>(id, recipe), items);
    }

    /** {@code ClientboundForgetLevelChunkPacket.getX()}: the packet is a record of the chunk position now. */
    public static int forgetX(net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket packet) {
        return packet.pos().x;
    }

    public static int forgetZ(net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket packet) {
        return packet.pos().z;
    }

    // ---- round 2: arrows, spawn rules, experience drops, simple containers

    /** The 1.20.1 parameter of {@code ArrowItem.isInfinite} was a Player; the shooter in 1.21.1 may be any living entity (null for a mob). */
    public static net.minecraft.world.entity.player.Player asPlayer(net.minecraft.world.entity.LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
    }

    /** {@code BlockState.isValidSpawn(level, pos, placementType, entityType)}: the placement type decides in 1.21.1 (the block rule is part of it). */
    public static boolean stateIsValidSpawn(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos,
            net.minecraft.world.entity.SpawnPlacementType type, net.minecraft.world.entity.EntityType<?> entityType) {
        return type.isSpawnPositionOk(level, pos, entityType);
    }

    /** {@code Block.isValidSpawn(state, level, pos, type, entityType)} (a super call): the block's own spawn predicate, vanilla's default for a plain block. */
    public static boolean blockIsValidSpawn(net.minecraft.world.level.block.Block block, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.entity.SpawnPlacementType type, net.minecraft.world.entity.EntityType<?> entityType) {
        return state.isValidSpawn(level, pos, entityType);
    }

    /** {@code BlockState.getExpDrop(level, random, pos, fortune, silkTouch)}: asks the block without a breaker or tool. */
    public static int stateExpDrop(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelReader level,
            net.minecraft.util.RandomSource random, BlockPos pos, int fortune, int silkTouch) {
        return level instanceof net.minecraft.world.level.LevelAccessor la ? state.getExpDrop(la, pos, null, null, net.minecraft.world.item.ItemStack.EMPTY) : 0;
    }

    /** {@code Block.getExpDrop(state, level, random, pos, fortune, silkTouch)} (a super call): blocks drop no experience unless they override it. */
    public static int blockExpDrop(net.minecraft.world.level.block.Block block, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.LevelReader level, net.minecraft.util.RandomSource random, BlockPos pos, int fortune, int silkTouch) {
        return 0;
    }

    /** The random source of the level (the 1.20.1 {@code getExpDrop} got one). */
    public static net.minecraft.util.RandomSource randomOf(net.minecraft.world.level.LevelAccessor level) {
        return level.getRandom();
    }

    /** The fortune level of the tool a block was broken with (the 1.20.1 {@code getExpDrop} got the level as an argument). */
    public static int fortune(net.minecraft.world.item.ItemStack tool) {
        return enchantLevel(tool, net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
    }

    public static int silkTouch(net.minecraft.world.item.ItemStack tool) {
        return enchantLevel(tool, net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
    }

    private static int enchantLevel(net.minecraft.world.item.ItemStack tool, ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        if (tool == null || tool.isEmpty()) return 0;
        return tool.getEnchantmentLevel(MigrateContext.provider().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key));
    }

    /** {@code DamageSource.isIndirect()}: the direct entity is not the one that caused the damage (the negation of 1.21.1's {@code isDirect}). */
    public static boolean isIndirect(net.minecraft.world.damagesource.DamageSource source) {
        return !source.isDirect();
    }

    /** {@code SimpleContainer.createTag()}: the registry provider is an argument now. */
    public static net.minecraft.nbt.ListTag containerCreateTag(net.minecraft.world.SimpleContainer container) {
        return container.createTag(MigrateContext.provider());
    }

    public static void containerFromTag(net.minecraft.world.SimpleContainer container, net.minecraft.nbt.ListTag tag) {
        container.fromTag(tag, MigrateContext.provider());
    }

    /** The state 1.21.1's {@code CropBlock.getGrowthSpeed} reads where 1.20.1 took the block. */
    public static net.minecraft.world.level.block.state.BlockState defaultState(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState();
    }

    /** {@code EnchantmentHelper.getRandomItemWith(enchantment, entity)}: a random equipped stack that carries the enchantment, with its slot (null when none does). */
    public static java.util.Map.Entry<net.minecraft.world.entity.EquipmentSlot, ItemStack> randomItemWith(LegacyEnchantment enchantment,
            net.minecraft.world.entity.LivingEntity entity) {
        return randomItemWith(enchantment, entity, stack -> true);
    }

    public static java.util.Map.Entry<net.minecraft.world.entity.EquipmentSlot, ItemStack> randomItemWith(LegacyEnchantment enchantment,
            net.minecraft.world.entity.LivingEntity entity, java.util.function.Predicate<ItemStack> filter) {
        List<java.util.Map.Entry<net.minecraft.world.entity.EquipmentSlot, ItemStack>> found = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && HolderCompat.stackEnchantmentLevel(stack, enchantment) > 0 && filter.test(stack)) found.add(java.util.Map.entry(slot, stack));
        }
        return found.isEmpty() ? null : found.get(entity.getRandom().nextInt(found.size()));
    }

    /**
     * {@code BaseContainerBlockEntity.setCustomName(Component)}: the name is only readable in 1.21.1 ({@code getCustomName}); it is set the way an item stack's
     * components are applied to the block entity (the {@code custom_name} component).
     */
    public static void setCustomName(net.minecraft.world.level.block.entity.BaseContainerBlockEntity container, net.minecraft.network.chat.Component name) {
        var map = net.minecraft.core.component.DataComponentMap.builder().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name).build();
        container.applyComponents(map, net.minecraft.core.component.DataComponentPatch.EMPTY);
    }

    /** {@code AreaEffectCloud.setPotion(Potion)}: the cloud holds potion contents now. */
    public static void cloudSetPotion(net.minecraft.world.entity.AreaEffectCloud cloud, net.minecraft.world.item.alchemy.Potion potion) {
        cloud.setPotionContents(new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder(potion)));
    }

    private static final java.lang.reflect.Field CLOUD_POTION = cloudPotionField();

    private static java.lang.reflect.Field cloudPotionField() {
        try {
            java.lang.reflect.Field f = net.minecraft.world.entity.AreaEffectCloud.class.getDeclaredField("potionContents");
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** {@code AreaEffectCloud.getPotion()}: read from the (private) potion contents of the cloud; the empty potion when there is none. */
    public static net.minecraft.world.item.alchemy.Potion cloudGetPotion(net.minecraft.world.entity.AreaEffectCloud cloud) {
        try {
            if (CLOUD_POTION != null && CLOUD_POTION.get(cloud) instanceof net.minecraft.world.item.alchemy.PotionContents contents && contents.potion().isPresent()) {
                return contents.potion().get().value();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // falls through to the empty potion
        }
        return net.minecraft.core.registries.BuiltInRegistries.POTION.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("empty"));
    }
}
