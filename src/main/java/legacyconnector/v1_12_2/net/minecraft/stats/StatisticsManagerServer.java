package legacyconnector.v1_12_2.net.minecraft.stats;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

/**
 * 1.12.2's statistics of a server player, saved in the world's stats folder. A view of the player's modern {@code ServerStatsCounter}
 * ({@link #mcc$of}) reads and writes the modern values (statistics a mod made count here, as in {@link StatisticsManager}).
 */
public class StatisticsManagerServer extends StatisticsManager {
    private final net.minecraft.stats.ServerStatsCounter mcc$server;

    private StatisticsManagerServer(net.minecraft.stats.ServerStatsCounter modern) {
        super(modern);
        this.mcc$server = modern;
    }

    /** A view of a server player's modern statistics (null gives null). */
    public static StatisticsManagerServer mcc$of(net.minecraft.stats.ServerStatsCounter modern) {
        return modern == null ? null : new StatisticsManagerServer(modern);
    }

    /** The file is read when the player joins; the modern counter keeps the values loaded. */
    public void readStatFile() {
    }

    public void saveStatFile() {
        if (mcc$server != null) mcc$server.save();
    }

    /** Marks every statistic changed, so the next {@link #sendStats} sends all of them. */
    public void markAllDirty() {
        if (mcc$server != null) mcc$server.markAllDirty();
    }

    /** Sends the changed statistics to the player's client. */
    public void sendStats(EntityPlayerMP player) {
        if (mcc$server != null && player != null && player.mcc$serverPlayer() != null) mcc$server.sendStats(player.mcc$serverPlayer());
    }
}
