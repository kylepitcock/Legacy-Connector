package legacyconnector.v1_7_10.net.minecraft.stats;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/**
 * A player's statistics and achievements. The modern game keeps advancements instead: achievements are remembered in
 * the player's persistent data (see {@code EntityPlayer.addStat}), other statistics in memory for the session. A writer
 * made for a client player reads the achievements of the same player on the integrated server.
 */
public class StatFileWriter {
    protected final Map<String, Integer> mcc$values = new HashMap<>();
    private final java.util.UUID mcc$player;

    public StatFileWriter() {
        this(null);
    }

    /** A writer whose achievements are those of the player with this id (null: kept in this object only). */
    public StatFileWriter(java.util.UUID player) {
        this.mcc$player = player;
    }

    /** The legacy view of the server-side player this writer belongs to, if that server runs in this game. */
    private EntityPlayer mcc$serverPlayer() {
        if (mcc$player == null) return null;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        var p = server.getPlayerList().getPlayer(mcc$player);
        return p == null ? null : (EntityPlayer) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(p);
    }

    public boolean hasAchievementUnlocked(Achievement achievement) {
        if (achievement == null) return false;
        EntityPlayer player = mcc$serverPlayer();
        if (player != null) return player.hasAchievementUnlocked(achievement);
        return mcc$values.getOrDefault(achievement.statId, 0) > 0;
    }

    public boolean canUnlockAchievement(Achievement achievement) {
        return achievement != null && (achievement.parentAchievement == null || hasAchievementUnlocked(achievement.parentAchievement));
    }

    /** How many of the achievement's ancestors are still locked. */
    public int func_150874_c(Achievement achievement) {
        if (hasAchievementUnlocked(achievement)) return 0;
        int n = 0;
        for (Achievement a = achievement.parentAchievement; a != null && !hasAchievementUnlocked(a); a = a.parentAchievement) n++;
        return n;
    }

    /** Adds to a statistic. */
    public void func_150871_b(EntityPlayer player, StatBase stat, int amount) {
        if (stat == null) return;
        if (stat instanceof Achievement && !canUnlockAchievement((Achievement) stat)) return;
        func_150873_a(player, stat, writeStat(stat) + amount);
    }

    /** Sets a statistic (an achievement is earned at 1). */
    public void func_150873_a(EntityPlayer player, StatBase stat, int value) {
        if (stat == null) return;
        mcc$values.put(stat.statId, value);
        if (stat instanceof Achievement a && value > 0) {
            EntityPlayer owner = player != null ? player : mcc$serverPlayer();
            if (owner != null && !owner.hasAchievementUnlocked(a)) owner.addStat(a, 1);
        }
    }

    public int writeStat(StatBase stat) {
        if (stat == null) return 0;
        if (stat instanceof Achievement a && hasAchievementUnlocked(a)) return Math.max(1, mcc$values.getOrDefault(stat.statId, 0));
        return mcc$values.getOrDefault(stat.statId, 0);
    }
}
