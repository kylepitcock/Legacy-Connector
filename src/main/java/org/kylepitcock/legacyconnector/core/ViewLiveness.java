package org.kylepitcock.legacyconnector.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * When the {@link ViewSweeper} may drop the cached legacy view of a modern object: the game is done with the object.
 */
public final class ViewLiveness {
    private static final boolean SWEEP_PLAYERS = LegacyConfig.perfSweepRemovedPlayers();
    private static final boolean SWEEP_DEAD_VIEWS = LegacyConfig.perfSweepDeadViews();

    private ViewLiveness() {}

    /**
     * An entity that was removed (killed, discarded, unloaded with its chunk; it is not used again after that) or whose level
     * unloaded. Players change dimension in place (removed and revived within one call), so a removed player counts only when it
     * was removed for another reason at two sweeps in a row ({@code perf.sweepRemovedPlayers}): the old object after a death and
     * respawn, or a player that logged out.
     */
    public static boolean entityGone(Entity e) {
        if (LevelReleases.isDead(e.level())) return true;
        if (!(e instanceof Player)) return e.isRemoved();
        if (!SWEEP_PLAYERS) return false;
        Entity.RemovalReason reason = e.getRemovalReason();
        return ViewSweeper.twice(e, reason != null && reason != Entity.RemovalReason.CHANGED_DIMENSION);
    }

    /** A block entity that was removed (broken, or unloaded with its chunk) or whose level unloaded. */
    public static boolean blockEntityGone(BlockEntity be) {
        return be.isRemoved() || LevelReleases.isDead(be.getLevel());
    }

    /** Whether views of connections and inventories are swept ({@code perf.sweepDeadViews}). */
    public static boolean sweepDeadViews() {
        return SWEEP_DEAD_VIEWS;
    }

    /** A connection whose channel has closed (two sweeps in a row, so disconnect handlers still find its view). */
    public static boolean connectionGone(net.minecraft.network.Connection c) {
        var ch = c.channel();
        return ViewSweeper.twice(c, ch != null && !ch.isOpen());
    }
}
