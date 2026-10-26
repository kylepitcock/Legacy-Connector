package legacyconnector.v1_7_10.net.minecraft.stats;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/**
 * A table of statistic values (achievements are statistics with a value above zero). The client's copy of the local
 * player's statistics is one; the server's per-player {@link StatisticsFile} extends it.
 */
public class StatFileWriter {
    private final Map<String, Integer> mcc$values = new HashMap<>();

    public boolean hasAchievementUnlocked(Achievement achievement) {
        return achievement != null && writeStat(achievement) > 0;
    }

    public boolean canUnlockAchievement(Achievement achievement) {
        return achievement != null && (achievement.parentAchievement == null || hasAchievementUnlocked(achievement.parentAchievement));
    }

    /** How many parents of the achievement are still locked. */
    public int func_150874_c(Achievement achievement) {
        if (hasAchievementUnlocked(achievement)) return 0;
        int locked = 0;
        for (Achievement a = achievement.parentAchievement; a != null && !hasAchievementUnlocked(a); a = a.parentAchievement) locked++;
        return locked;
    }

    public void func_150871_b(EntityPlayer player, StatBase stat, int amount) {
        func_150871_b((Object) player, stat, amount);
    }

    public void func_150873_a(EntityPlayer player, StatBase stat, int value) {
        func_150873_a((Object) player, stat, value);
    }

    /** Adds to a statistic. */
    public void func_150871_b(Object player, StatBase stat, int amount) {
        if (stat != null && (!stat.isAchievement() || canUnlockAchievement((Achievement) stat))) func_150873_a(player, stat, writeStat(stat) + amount);
    }

    /** Sets a statistic. */
    public void func_150873_a(Object player, StatBase stat, int value) {
        if (stat != null) mcc$values.put(stat.statId, value);
    }

    public int writeStat(StatBase stat) {
        return stat == null ? 0 : mcc$values.getOrDefault(stat.statId, 0);
    }
}
