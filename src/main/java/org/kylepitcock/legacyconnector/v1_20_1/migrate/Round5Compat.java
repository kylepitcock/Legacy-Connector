package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Signature changes of Minecraft 1.20.1 to 1.21.1 found by the ATM 9 ranking of 2026-11-15, round 5 (jars closest to linking; see {@code mc-calls.tsv}).
 */
public final class Round5Compat {
    private Round5Compat() {}

    /** A 1.20.1 loot table id as the resource key 1.21.1 wants (structure chests, containers). */
    public static ResourceKey<LootTable> lootKey(ResourceLocation id) {
        return ResourceKey.create(Registries.LOOT_TABLE, id);
    }

    /** The liquid settings parameter jigsaw pieces and elements gained in 1.21.1, from the old {@code keepJigsaws} flag (1.20.1 behaviour: waterlogging applied). */
    public static LiquidSettings liquidSettings(boolean ignored) {
        return LiquidSettings.APPLY_WATERLOGGING;
    }

    /** Same, for the {@code PoolElementStructurePiece} constructor (which had no flag). */
    public static LiquidSettings liquidSettings(StructureTemplateManager ignored) {
        return LiquidSettings.APPLY_WATERLOGGING;
    }

    /**
     * {@code StructureManager.checkStructurePresence(chunk, structure, skipKnown)}: 1.21.1 also wants the placement the structure is spread by, which
     * the structure set holding the structure provides. A structure in no set is never present.
     */
    public static StructureCheckResult checkStructurePresence(StructureManager manager, ChunkPos pos, Structure structure, boolean skipKnown) {
        Registry<StructureSet> sets = manager.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        for (StructureSet set : sets) {
            for (StructureSet.StructureSelectionEntry entry : set.structures()) {
                if (entry.structure().value() == structure) return manager.checkStructurePresence(pos, structure, set.placement(), skipKnown);
            }
        }
        return StructureCheckResult.START_NOT_PRESENT;
    }

    /** {@code CriterionTrigger.getId()}: the trigger's registry id (null while the trigger is not registered yet). */
    public static ResourceLocation triggerId(net.minecraft.advancements.CriterionTrigger<?> trigger) {
        return net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES.getKey(trigger);
    }

    /** {@code CreativeModeTab.Builder.backgroundSuffix("items")}: the tab image {@code textures/gui/container/creative_inventory/tab_items.png}. */
    public static net.minecraft.world.item.CreativeModeTab.Builder tabBackgroundSuffix(net.minecraft.world.item.CreativeModeTab.Builder builder, String suffix) {
        return builder.backgroundTexture(ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_" + suffix + ".png"));
    }

    /** The spawn dimensions scale {@code EntityType} gained as a constructor parameter (its default). */
    public static float spawnScale(net.minecraft.world.entity.EntityDimensions ignored) {
        return 1.0F;
    }

    /** {@code ButtonBlock.arrowsCanPress}: the flag moved into the block set type (wooden buttons yes, stone ones no). */
    public static boolean buttonArrows(ButtonBlock block) {
        return block.type.canButtonBeActivatedByArrows();
    }

    /** {@code ParticleOptions.writeToNetwork(buf)}: the options' payload (not the type id) through the type's stream codec. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void writeParticle(ParticleOptions options, FriendlyByteBuf buf) {
        RegistryFriendlyByteBuf target = buf instanceof RegistryFriendlyByteBuf r ? r
                : new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), MigrateContext.registries());
        ((net.minecraft.network.codec.StreamCodec) options.getType().streamCodec()).encode(target, options);
        if (target != buf) buf.writeBytes(target);
    }
}
