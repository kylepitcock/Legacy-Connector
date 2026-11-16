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

    /** {@code BlockBehaviour.getLootTable()}: the table id (1.21.1 returns the resource key). */
    public static ResourceLocation blockLootTable(net.minecraft.world.level.block.state.BlockBehaviour block) {
        return block.getLootTable().location();
    }

    /**
     * {@code Entity.changeDimension(level)}: 1.21.1 moves through a {@code DimensionTransition}. APPROXIMATION: the entity keeps its position, motion and
     * rotation in the target level (no portal search or coordinate scaling, which the 1.20.1 call did for the vanilla portal destination).
     */
    public static net.minecraft.world.entity.Entity changeDimension(net.minecraft.world.entity.Entity entity, net.minecraft.server.level.ServerLevel level) {
        return entity.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(level, entity.position(), entity.getDeltaMovement(),
                entity.getYRot(), entity.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
    }

    /** {@code Blocks.leaves(sound)} (private static in both versions, the 1.20.1 return type was the leaves block). */
    public static net.minecraft.world.level.block.LeavesBlock leaves(net.minecraft.world.level.block.SoundType sound) {
        return (net.minecraft.world.level.block.LeavesBlock) net.minecraft.world.level.block.Blocks.leaves(sound);
    }

    /** {@code Blocks.log(top, side)}. */
    public static net.minecraft.world.level.block.RotatedPillarBlock log(net.minecraft.world.level.material.MapColor top, net.minecraft.world.level.material.MapColor side) {
        return (net.minecraft.world.level.block.RotatedPillarBlock) net.minecraft.world.level.block.Blocks.log(top, side);
    }

    /** {@code PortalShape.createPortalInfo(...)} of 1.20.1 (the arrival position inside a found portal rectangle), result in the stand-in for {@code PortalInfo}. */
    public static LegacyPortalInfo createPortalInfo(net.minecraft.server.level.ServerLevel level, net.minecraft.BlockUtil.FoundRectangle rect,
            net.minecraft.core.Direction.Axis axis, net.minecraft.world.phys.Vec3 relative, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.phys.Vec3 speed, float yRot, float xRot) {
        net.minecraft.core.BlockPos corner = rect.minCorner;
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(corner);
        net.minecraft.core.Direction.Axis portalAxis = state.getOptionalValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS)
                .orElse(net.minecraft.core.Direction.Axis.X);
        double width = rect.axis1Size;
        double height = rect.axis2Size;
        net.minecraft.world.entity.EntityDimensions dims = entity.getDimensions(entity.getPose());
        int turn = axis == portalAxis ? 0 : 90;
        net.minecraft.world.phys.Vec3 newSpeed = axis == portalAxis ? speed : new net.minecraft.world.phys.Vec3(speed.z, speed.y, -speed.x);
        double along = dims.width() / 2.0 + (width - dims.width()) * relative.x();
        double up = (height - dims.height()) * relative.y();
        double across = 0.5 + relative.z();
        boolean x = portalAxis == net.minecraft.core.Direction.Axis.X;
        net.minecraft.world.phys.Vec3 pos = new net.minecraft.world.phys.Vec3(corner.getX() + (x ? along : across), corner.getY() + up, corner.getZ() + (x ? across : along));
        pos = net.minecraft.world.level.portal.PortalShape.findCollisionFreePosition(pos, level, entity, dims);
        return new LegacyPortalInfo(pos, newSpeed, yRot + turn, xRot);
    }

    /** {@code WalkNodeEvaluator.getBlockPathTypeStatic(getter, pos)}. APPROXIMATION: the block's own path type, without the danger/neighbour checks. */
    public static net.minecraft.world.level.pathfinder.PathType blockPathTypeStatic(net.minecraft.world.level.BlockGetter getter, net.minecraft.core.BlockPos.MutableBlockPos pos) {
        return net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getPathTypeFromState(getter, pos);
    }

    /** {@code TheEndGatewayBlockEntity.canEntityTeleport(entity)}. */
    public static boolean canGatewayTeleport(net.minecraft.world.entity.Entity entity) {
        return entity.canUsePortal(false);
    }

    /** {@code TheEndGatewayBlockEntity.teleportEntity(level, pos, state, entity, gateway)}: hands the entity to the gateway block's portal logic (it teleports on its next tick). */
    public static void gatewayTeleport(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.entity.Entity entity, net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity gateway) {
        if (!level.isClientSide && state.getBlock() instanceof net.minecraft.world.level.block.Portal portal) entity.setAsInsidePortal(portal, pos);
    }

    /** {@code MerchantOffers.createTag()}: the offers as the compound with the {@code Recipes} list. */
    public static net.minecraft.nbt.CompoundTag merchantOffersTag(net.minecraft.world.item.trading.MerchantOffers offers) {
        net.minecraft.nbt.Tag tag = net.minecraft.world.item.trading.MerchantOffers.CODEC
                .encodeStart(MigrateContext.provider().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), offers).getOrThrow();
        return tag instanceof net.minecraft.nbt.CompoundTag compound ? compound : new net.minecraft.nbt.CompoundTag();
    }

    /** The two stacks {@code customArrow} gained (the weapon and the ammunition): unknown to a 1.20.1 call, so empty. */
    public static net.minecraft.world.item.ItemStack noStack(net.minecraft.world.entity.projectile.AbstractArrow ignored) {
        return net.minecraft.world.item.ItemStack.EMPTY;
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
