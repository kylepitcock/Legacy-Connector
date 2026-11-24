package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Failure-class round (hard ATM 9 jars): small Minecraft members of 1.20.1 that 1.21.1 changed or dropped (rows in mc-calls.tsv, mc-bridges.tsv and
 * mc-fields.tsv).
 */
public final class Round16Compat {
    private Round16Compat() {}

    private static final ResourceLocation UNKNOWN_RECIPE_ID = ResourceLocation.withDefaultNamespace("legacy_unknown_recipe");

    // ------------------------------------------------------------------ entity size

    /** {@code Entity.getStandingEyeHeight(pose, dimensions)} (call): 1.21.1 keeps the eye height in the dimensions. */
    public static float standingEyeHeight(Entity entity, net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
        return dimensions.eyeHeight();
    }

    // ------------------------------------------------------------------ portals

    /** {@code Entity.isInsidePortal} (field read, 1.20.1): the entity stands in a portal this tick. */
    public static boolean insidePortal(Entity entity) {
        PortalProcessor p = entity.portalProcess;
        return p != null && p.isInsidePortalThisTick();
    }

    /** {@code entity.isInsidePortal = value} (field write). Only the current portal process can be updated; a process cannot be created without a portal. */
    public static void setInsidePortal(Entity entity, boolean value) {
        PortalProcessor p = entity.portalProcess;
        if (p != null) p.setAsInsidePortalThisTick(value);
    }

    /** {@code Entity.portalTime} (field read, 1.20.1): ticks spent in the portal, 0 outside one. */
    public static int portalTime(Entity entity) {
        PortalProcessor p = entity.portalProcess;
        return p == null ? 0 : p.getPortalTime();
    }

    /** {@code entity.portalTime = value} (field write): 1.21.1 keeps the counter in a process that counts by itself; a write is dropped (APPROXIMATION). */
    public static void setPortalTime(Entity entity, int value) {}

    /**
     * {@code entity.handleInsidePortal(pos)} (call): 1.20.1 told the entity that it stands in a (nether) portal block; 1.21.1 hands the portal over as well.
     */
    public static void handleInsidePortal(Entity entity, BlockPos pos) {
        entity.setAsInsidePortal((Portal) Blocks.NETHER_PORTAL, pos);
    }

    /**
     * {@code super.handleInsidePortal(pos)} of an entity whose parent is Minecraft's: what {@code Entity.setAsInsidePortal} does (the override that the layer
     * bridged calls the mod's method, so the real method cannot be called here without looping).
     */
    public static void superHandleInsidePortal(Object receiver, BlockPos pos) {
        Entity entity = (Entity) receiver;
        if (entity.isOnPortalCooldown()) {
            entity.setPortalCooldown();
            return;
        }
        Portal portal = (Portal) Blocks.NETHER_PORTAL;
        if (entity.portalProcess == null || !entity.portalProcess.isSamePortal(portal)) {
            entity.portalProcess = new PortalProcessor(portal, pos.immutable());
        } else if (!entity.portalProcess.isInsidePortalThisTick()) {
            entity.portalProcess.updateEntryPosition(pos.immutable());
            entity.portalProcess.setAsInsidePortalThisTick(true);
        }
    }

    // ------------------------------------------------------------------ chunks, storage

    /** {@code ChunkHolder.getFullChunk()}: the chunk once it is full, else null. */
    public static LevelChunk fullChunk(ChunkHolder holder) {
        ChunkResult<LevelChunk> result = holder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);
        return result.orElse(null);
    }

    // ------------------------------------------------------------------ projectiles

    /** {@code ThrowableProjectile.getGravity()} (a float in 1.20.1) as 1.21.1's {@code getDefaultGravity()} asks it. */
    public static double gravityOf(float gravity) {
        return gravity;
    }

    /** {@code super.getGravity()} of a throwable projectile: the 1.20.1 default. */
    public static float superThrowableGravity(Object projectile) {
        return 0.03F;
    }

    // ------------------------------------------------------------------ recipes

    /**
     * The 1.20.1 {@code SingleItemMaker} a serializer was built with, as 1.21.1's recipe factory (no recipe id any more: a placeholder is passed, a recipe
     * that needs its id reads it from the holder).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static SingleItemRecipe.Factory<?> singleItemFactory(LegacySingleItemMaker maker) {
        return (group, ingredient, result) -> (SingleItemRecipe) maker.create(UNKNOWN_RECIPE_ID, group, ingredient, result);
    }
}
