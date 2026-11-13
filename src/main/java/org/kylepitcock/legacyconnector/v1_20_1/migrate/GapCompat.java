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
}
