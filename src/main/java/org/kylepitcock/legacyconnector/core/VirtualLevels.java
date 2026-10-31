package org.kylepitcock.legacyconnector.core;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * Levels other mods build as copies of part of a world: Create's contraption render world (VirtualRenderWorld) and its contraption
 * world, Ponder scenes, schematic previews. 1.7.10 and 1.12.2 had one World per dimension and no such copies, so legacy tile
 * entities placed in one must not act as if they were the real block (tick, send packets), and the legacy views of these short-lived
 * levels must not be kept forever.
 * <p>A level is "real" when it is a ServerLevel or the client's ClientLevel. Sable/Create Aeronautics sub-levels are not copies:
 * their blocks stay in the real level (in far-away plot chunks).
 */
public final class VirtualLevels {
    private VirtualLevels() {}

    /** Whether the level is a copy built by another mod (not a ServerLevel, not the client's level, not a generation region). */
    public static boolean isVirtual(LevelAccessor level) {
        if (!(level instanceof Level l)) return false;
        if (l instanceof net.minecraft.server.level.ServerLevel) return false;
        if (l.isClientSide() && net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return !Client.isClientLevel(l);
        return true;
    }

    private static final java.util.Set<Class<?>> SKIPPED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Whether a legacy tile entity in this level must not tick: the level is a copy (a moving contraption's render world, a Ponder
     * scene) where the tile entity would burn fuel, move items or send packets a second time next to the real one. Logged once per
     * kind of level.
     */
    public static boolean skipTick(Level level) {
        if (level == null || !isVirtual(level)) return false;
        if (SKIPPED.add(level.getClass())) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/TileEntities").info("Legacy tile entities in {} (a copy of a world) do not tick",
                    level.getClass().getName());
        }
        return true;
    }

    /** Kept apart so the client-only class is never resolved on a dedicated server. */
    private static final class Client {
        static boolean isClientLevel(Level level) {
            return level instanceof net.minecraft.client.multiplayer.ClientLevel;
        }
    }
}
