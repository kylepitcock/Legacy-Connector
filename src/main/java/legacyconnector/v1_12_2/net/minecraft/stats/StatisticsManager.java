package legacyconnector.v1_12_2.net.minecraft.stats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/**
 * 1.12.2's statistics of a player. A view of a modern stats counter ({@link #mcc$of}, the client player's) reads and writes the modern
 * values of the statistics that became modern ones; other statistics (mods' own) are counted here.
 */
public class StatisticsManager {
    private final Map<StatBase, Integer> mcc$values = new ConcurrentHashMap<>();
    private final net.minecraft.stats.StatsCounter mcc$modern;

    public StatisticsManager() {
        this(null);
    }

    private StatisticsManager(net.minecraft.stats.StatsCounter modern) {
        this.mcc$modern = modern;
    }

    public static StatisticsManager mcc$of(net.minecraft.stats.StatsCounter modern) {
        return modern == null ? null : new StatisticsManager(modern);
    }

    public void increaseStat(EntityPlayer player, StatBase stat, int amount) {
        if (stat == null) return;
        unlockAchievement(player, stat, readStat(stat) + amount);
    }

    public void unlockAchievement(EntityPlayer playerIn, StatBase statIn, int value) {
        if (statIn == null) return;
        var modern = mcc$modern == null ? null : statIn.mcc$modern();
        if (modern != null) {
            mcc$modern.setValue(playerIn == null ? null : playerIn.mcc$player(), modern, value);
        } else {
            mcc$values.put(statIn, value);
        }
    }

    public int readStat(StatBase stat) {
        if (stat == null) return 0;
        var modern = mcc$modern == null ? null : stat.mcc$modern();
        if (modern != null) return mcc$modern.getValue(modern);
        Integer v = mcc$values.get(stat);
        return v == null ? 0 : v;
    }
}
