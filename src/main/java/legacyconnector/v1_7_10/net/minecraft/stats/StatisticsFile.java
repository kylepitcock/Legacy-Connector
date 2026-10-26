package legacyconnector.v1_7_10.net.minecraft.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * A player's statistics and achievements. The modern game keeps advancements instead; legacy achievements are tracked per player
 * in memory, so mods that unlock achievements and test for them see their own state during the session.
 */
public class StatisticsFile extends StatFileWriter {
    private final Map<String, Integer> values = new HashMap<>();

    public boolean hasAchievementUnlocked(Achievement achievement) {
        return achievement != null && values.getOrDefault(achievement.statId, 0) > 0;
    }

    public boolean canUnlockAchievement(Achievement achievement) {
        return achievement != null && !hasAchievementUnlocked(achievement)
                && (achievement.parentAchievement == null || hasAchievementUnlocked(achievement.parentAchievement));
    }

    public void func_150873_a(Object player, StatBase stat, int value) {
        if (stat != null) values.put(stat.statId, value);
    }

    public int writeStat(StatBase stat) {
        return stat == null ? 0 : values.getOrDefault(stat.statId, 0);
    }

    public void func_150871_b(Object player, StatBase stat, int amount) {
        if (stat != null) values.merge(stat.statId, amount, Integer::sum);
    }
}
