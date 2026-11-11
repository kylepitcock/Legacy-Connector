package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * Beta kept ticking the chunks around a dead player until they were lazily unloaded, so a boss still saw its dead target and reset. A
 * modern chunk stops ticking entities once no player is near, so the boss froze mid-fight. When Beta entities are near a dying
 * player, the chunks around the death stay ticking for a few seconds.
 */
public final class PlayerDeathGraceB173 {
    /** Ticks the death spot keeps ticking (Beta's unload was slower than this). */
    static final int GRACE_TICKS = 100;
    private static final double RANGE = 48;
    private static final TicketType<ChunkPos> TICKET = TicketType.create("legacyconnector_b173_death", Comparator.comparingLong(ChunkPos::toLong), GRACE_TICKS);
    private PlayerDeathGraceB173() {}

    static void hold(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !betaEntityNear(level, player)) return;
        ChunkPos at = player.chunkPosition();
        // Distance 3 gives the centre chunk the entity-ticking level and its neighbours loaded ones.
        level.getChunkSource().addRegionTicket(TICKET, at, 3, at);
    }

    private static boolean betaEntityNear(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, player.getBoundingBox().inflate(RANGE), entity -> entity instanceof B173EntityHost).isEmpty();
    }
}
