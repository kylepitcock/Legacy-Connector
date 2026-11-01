package org.kylepitcock.legacyconnector.core.perf;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.LoggerFactory;

/**
 * {@code perf.staticTileEntities}: legacy tile entities that never tick get no modern ticker.
 * <p>1.7.10 Forge added a tile entity to the world's ticking list only if {@code canUpdate()} returned true when it was added (and
 * the default {@code updateEntity()} does nothing); 1.12.2 ticked only tile entities implementing {@code ITickable}. The host block
 * cannot know which tile entity it will hold when the chunk asks for its ticker (the legacy object is made later, on first use), so
 * every host starts with a ticker; once a host knows its tile entity is static it asks its chunk to look again
 * ({@link #refresh}), and {@code mixin.perf.LevelChunkStaticTickerMixin} answers "no ticker" for it. A static legacy machine,
 * cable, storage block or decoration then costs nothing per tick (vanilla's ticker wrapper did a ticking-range lookup, a profiler
 * push and a block state check for each of them every tick). When the host gets another tile entity it refreshes again.
 */
public final class StaticTickers {
    private StaticTickers() {}

    /** A legacy tile entity host that can tell whether its current tile entity never ticks. */
    public interface Host {
        /** True only when the host holds a tile entity that is known not to tick (false while unknown). */
        boolean legacyconnector$isStatic();
    }

    private static int failures;

    /** Whether the chunk must not give this block entity a ticker. */
    public static boolean noTicker(BlockEntity be) {
        return Perf.STATIC_TILE_ENTITIES && be instanceof Host h && h.legacyconnector$isStatic();
    }

    /** Re-evaluates the ticker of a host in its chunk (drops it for a static tile entity, restores it otherwise). */
    public static void refresh(BlockEntity be) {
        if (!Perf.STATIC_TILE_ENTITIES) return;
        var level = be.getLevel();
        if (level == null || be.isRemoved()) return;
        try {
            // only the chunk that holds this very block entity (a copy in another mod's virtual level has no LevelChunk of its own)
            if (level.getChunk(be.getBlockPos()) instanceof LevelChunk chunk && chunk.getBlockEntities().get(be.getBlockPos()) == be) {
                chunk.updateBlockEntityTicker(be);
            }
        } catch (RuntimeException e) {
            // keeps its ticker (the old, slower path)
            if (failures++ < 3) LoggerFactory.getLogger("LegacyConnector/TileEntities").warn("Could not drop the ticker of a static tile entity at {}: {}",
                    be.getBlockPos(), e.toString());
        }
    }
}
