package legacyconnector.v1_12_2.net.minecraft.world.storage;

/** 1.12.2 world info: a view over a modern level's data (time, weather, spawn, game mode). */
public class WorldInfo {
    private final net.minecraft.world.level.Level level;

    public WorldInfo(net.minecraft.world.level.Level level) {
        this.level = level;
    }

    private net.minecraft.world.level.storage.ServerLevelData mcc$server() {
        return level != null && level.getLevelData() instanceof net.minecraft.world.level.storage.ServerLevelData d ? d : null;
    }

    public String getWorldName() {
        var d = mcc$server();
        return d == null ? "world" : d.getLevelName();
    }

    public long getSeed() {
        return level instanceof net.minecraft.server.level.ServerLevel s ? s.getSeed() : 0L;
    }

    public long getWorldTotalTime() {
        return level == null ? 0 : level.getGameTime();
    }

    public long getWorldTime() {
        return level == null ? 0 : level.getDayTime();
    }

    public void setWorldTime(long time) {
        if (level instanceof net.minecraft.server.level.ServerLevel s) s.setDayTime(time);
    }

    public boolean isRaining() {
        return level != null && level.getLevelData().isRaining();
    }

    public void setRaining(boolean raining) {
        var d = mcc$server();
        if (d != null) d.setRaining(raining);
    }

    public boolean isThundering() {
        return level != null && level.getLevelData().isThundering();
    }

    public void setThundering(boolean thundering) {
        var d = mcc$server();
        if (d != null) d.setThundering(thundering);
    }

    public int getRainTime() {
        var d = mcc$server();
        return d == null ? 0 : d.getRainTime();
    }

    public void setRainTime(int time) {
        var d = mcc$server();
        if (d != null) d.setRainTime(time);
    }

    public int getThunderTime() {
        var d = mcc$server();
        return d == null ? 0 : d.getThunderTime();
    }

    public void setThunderTime(int time) {
        var d = mcc$server();
        if (d != null) d.setThunderTime(time);
    }

    public int getCleanWeatherTime() {
        var d = mcc$server();
        return d == null ? 0 : d.getClearWeatherTime();
    }

    public void setCleanWeatherTime(int time) {
        var d = mcc$server();
        if (d != null) d.setClearWeatherTime(time);
    }

    public int getSpawnX() {
        return level == null ? 0 : level.getSharedSpawnPos().getX();
    }

    public int getSpawnY() {
        return level == null ? 64 : level.getSharedSpawnPos().getY();
    }

    public int getSpawnZ() {
        return level == null ? 0 : level.getSharedSpawnPos().getZ();
    }

    public legacyconnector.v1_12_2.net.minecraft.world.GameType getGameType() {
        var d = mcc$server();
        return d == null ? legacyconnector.v1_12_2.net.minecraft.world.GameType.SURVIVAL : legacyconnector.v1_12_2.net.minecraft.world.GameType.mcc$of(d.getGameType());
    }

    public boolean isHardcoreModeEnabled() {
        return level != null && level.getLevelData().isHardcore();
    }

    public boolean areCommandsAllowed() {
        var d = mcc$server();
        return d != null && d.isAllowCommands();
    }

    public boolean isInitialized() {
        var d = mcc$server();
        return d == null || d.isInitialized();
    }

    public legacyconnector.v1_12_2.net.minecraft.world.GameRules getGameRulesInstance() {
        return new legacyconnector.v1_12_2.net.minecraft.world.GameRules(level == null ? null : level.getGameRules());
    }
}
