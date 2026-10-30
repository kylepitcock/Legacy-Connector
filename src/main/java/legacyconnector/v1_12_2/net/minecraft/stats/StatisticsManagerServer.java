package legacyconnector.v1_12_2.net.minecraft.stats;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

/**
 * 1.12.2 server-side statistics of a player ({@code EntityPlayerMP.getStatFile()}): a view of the player's modern stats, which the
 * game saves to its stats file and sends to the client itself.
 */
public class StatisticsManagerServer extends StatisticsManager {
    public StatisticsManagerServer(legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer serverIn, java.io.File statsFileIn) {
        super(null);
    }

    protected StatisticsManagerServer(net.minecraft.stats.ServerStatsCounter modern) {
        super(modern);
    }

    public static StatisticsManagerServer mcc$of(net.minecraft.stats.ServerStatsCounter modern) {
        return modern == null ? null : new StatisticsManagerServer(modern);
    }

    /** The modern stats file is read when the player joins. */
    public void readStatFile() {
    }

    /** The modern stats file is saved with the player. */
    public void saveStatFile() {
    }

    /** Sends the player their statistics (the modern stats packet). */
    public void sendStats(EntityPlayerMP player) {
        var p = player == null ? null : player.mcc$serverPlayer();
        if (p != null) p.getStats().sendStats(p);
    }

    public void markAllDirty() {
    }
}
