package legacyconnector.v1_12_2.net.minecraft.world.storage;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty;
import legacyconnector.v1_12_2.net.minecraft.world.GameRules;
import legacyconnector.v1_12_2.net.minecraft.world.GameType;
import legacyconnector.v1_12_2.net.minecraft.world.WorldType;

/**
 * 1.12.2 world properties. For a modern level it is a view over the level's data (time, weather, spawn, game mode, rules are the
 * live values); a WorldInfo a mod builds itself keeps its own values.
 */
public class WorldInfo {
    private final net.minecraft.world.level.Level level;

    private long randomSeed;
    private WorldType terrainType = WorldType.DEFAULT;
    private String generatorOptions = "";
    private int spawnX;
    private int spawnY = 64;
    private int spawnZ;
    private long totalTime;
    private long worldTime;
    private String levelName = "MpServer";
    private int saveVersion = 19133;
    private int cleanWeatherTime;
    private boolean raining;
    private int rainTime;
    private boolean thundering;
    private int thunderTime;
    private GameType gameType = GameType.SURVIVAL;
    private boolean mapFeaturesEnabled = true;
    private boolean hardcore;
    private boolean allowCommands;
    private boolean initialized;
    private EnumDifficulty difficulty = EnumDifficulty.NORMAL;
    private boolean difficultyLocked;
    private GameRules gameRules;
    private final java.util.Map<Integer, NBTTagCompound> dimensionData = new java.util.HashMap<>();

    protected WorldInfo() {
        this.level = null;
    }

    public WorldInfo(NBTTagCompound nbt) {
        this.level = null;
        if (nbt == null) return;
        randomSeed = nbt.getLong("RandomSeed");
        WorldType t = WorldType.byName(nbt.getString("generatorName"));
        if (t != null) terrainType = t;
        generatorOptions = nbt.getString("generatorOptions");
        gameType = GameType.getByID(nbt.getInteger("GameType"));
        mapFeaturesEnabled = !nbt.hasKey("MapFeatures") || nbt.getBoolean("MapFeatures");
        spawnX = nbt.getInteger("SpawnX");
        spawnY = nbt.getInteger("SpawnY");
        spawnZ = nbt.getInteger("SpawnZ");
        totalTime = nbt.getLong("Time");
        worldTime = nbt.hasKey("DayTime") ? nbt.getLong("DayTime") : totalTime;
        levelName = nbt.getString("LevelName");
        raining = nbt.getBoolean("raining");
        rainTime = nbt.getInteger("rainTime");
        thundering = nbt.getBoolean("thundering");
        thunderTime = nbt.getInteger("thunderTime");
        hardcore = nbt.getBoolean("hardcore");
        allowCommands = nbt.getBoolean("allowCommands");
        initialized = !nbt.hasKey("initialized") || nbt.getBoolean("initialized");
        if (nbt.hasKey("Difficulty")) difficulty = EnumDifficulty.byId(nbt.getByte("Difficulty"));
        difficultyLocked = nbt.getBoolean("DifficultyLocked");
    }

    public WorldInfo(WorldInfo other) {
        this.level = other.level;
        copyFrom(other);
    }

    public WorldInfo(legacyconnector.v1_12_2.net.minecraft.world.WorldSettings settings, String name) {
        this.level = null;
        populateFromWorldSettings(settings);
        this.levelName = name;
    }

    private WorldInfo(net.minecraft.world.level.Level level) {
        this.level = level;
    }

    private static final java.util.Map<net.minecraft.world.level.Level, WorldInfo> VIEWS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** The info of a modern level (one per level). */
    public static WorldInfo mcc$of(net.minecraft.world.level.Level level) {
        if (level == null) return new WorldInfo();
        return VIEWS.computeIfAbsent(level, WorldInfo::new);
    }

    private void copyFrom(WorldInfo o) {
        randomSeed = o.getSeed();
        terrainType = o.getTerrainType();
        generatorOptions = o.getGeneratorOptions();
        spawnX = o.getSpawnX();
        spawnY = o.getSpawnY();
        spawnZ = o.getSpawnZ();
        totalTime = o.getWorldTotalTime();
        worldTime = o.getWorldTime();
        levelName = o.getWorldName();
        raining = o.isRaining();
        rainTime = o.getRainTime();
        thundering = o.isThundering();
        thunderTime = o.getThunderTime();
        gameType = o.getGameType();
        hardcore = o.isHardcoreModeEnabled();
        difficulty = o.getDifficulty();
    }

    private net.minecraft.world.level.storage.ServerLevelData server() {
        return level != null && level.getLevelData() instanceof net.minecraft.world.level.storage.ServerLevelData s ? s : null;
    }

    public void populateFromWorldSettings(legacyconnector.v1_12_2.net.minecraft.world.WorldSettings settings) {
        if (settings == null) return;
        randomSeed = settings.getSeed();
        gameType = settings.getGameType();
        mapFeaturesEnabled = settings.isMapFeaturesEnabled();
        hardcore = settings.getHardcoreEnabled();
        terrainType = settings.getTerrainType();
        generatorOptions = settings.getGeneratorOptions();
        allowCommands = settings.areCommandsAllowed();
    }

    public long getSeed() {
        if (level instanceof net.minecraft.server.level.ServerLevel s) return s.getSeed();
        return randomSeed;
    }

    public int getSpawnX() {
        return level != null ? level.getLevelData().getSpawnPos().getX() : spawnX;
    }

    public int getSpawnY() {
        return level != null ? level.getLevelData().getSpawnPos().getY() : spawnY;
    }

    public int getSpawnZ() {
        return level != null ? level.getLevelData().getSpawnPos().getZ() : spawnZ;
    }

    public long getWorldTotalTime() {
        return level != null ? level.getGameTime() : totalTime;
    }

    public long getWorldTime() {
        return level != null ? level.getDayTime() : worldTime;
    }

    public long getSizeOnDisk() {
        return 0L;
    }

    public NBTTagCompound getPlayerNBTTagCompound() {
        return null;
    }

    public void setSpawnX(int x) {
        setSpawn(new BlockPos(x, getSpawnY(), getSpawnZ()));
    }

    public void setSpawnY(int y) {
        setSpawn(new BlockPos(getSpawnX(), y, getSpawnZ()));
    }

    public void setSpawnZ(int z) {
        setSpawn(new BlockPos(getSpawnX(), getSpawnY(), z));
    }

    public void setWorldTotalTime(long time) {
        var s = server();
        if (s != null) s.setGameTime(time);
        else totalTime = time;
    }

    public void setWorldTime(long time) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) sl.setDayTime(time);
        else worldTime = time;
    }

    public void setSpawn(BlockPos pos) {
        if (level != null && level.getLevelData() instanceof net.minecraft.world.level.storage.WritableLevelData w) {
            w.setSpawn(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()), w.getSpawnAngle());
            return;
        }
        spawnX = pos.getX();
        spawnY = pos.getY();
        spawnZ = pos.getZ();
    }

    public String getWorldName() {
        var s = server();
        return s != null ? s.getLevelName() : levelName;
    }

    public void setWorldName(String name) {
        levelName = name;
    }

    public int getSaveVersion() {
        return saveVersion;
    }

    public void setSaveVersion(int version) {
        saveVersion = version;
    }

    public long getLastTimePlayed() {
        return System.currentTimeMillis();
    }

    public int getCleanWeatherTime() {
        var s = server();
        return s != null ? s.getClearWeatherTime() : cleanWeatherTime;
    }

    public void setCleanWeatherTime(int time) {
        var s = server();
        if (s != null) s.setClearWeatherTime(time);
        else cleanWeatherTime = time;
    }

    public boolean isThundering() {
        return level != null ? level.getLevelData().isThundering() : thundering;
    }

    public void setThundering(boolean value) {
        var s = server();
        if (s != null) s.setThundering(value);
        else thundering = value;
    }

    public int getThunderTime() {
        var s = server();
        return s != null ? s.getThunderTime() : thunderTime;
    }

    public void setThunderTime(int time) {
        var s = server();
        if (s != null) s.setThunderTime(time);
        else thunderTime = time;
    }

    public boolean isRaining() {
        return level != null ? level.getLevelData().isRaining() : raining;
    }

    public void setRaining(boolean value) {
        if (level != null && level.getLevelData() instanceof net.minecraft.world.level.storage.WritableLevelData w) w.setRaining(value);
        else raining = value;
    }

    public int getRainTime() {
        var s = server();
        return s != null ? s.getRainTime() : rainTime;
    }

    public void setRainTime(int time) {
        var s = server();
        if (s != null) s.setRainTime(time);
        else rainTime = time;
    }

    public GameType getGameType() {
        var s = server();
        return s != null ? GameType.mcc$of(s.getGameType()) : gameType;
    }

    public boolean isMapFeaturesEnabled() {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) return sl.getServer().getWorldData().worldGenOptions().generateStructures();
        return mapFeaturesEnabled;
    }

    public void setMapFeaturesEnabled(boolean enabled) {
        mapFeaturesEnabled = enabled;
    }

    public void setGameType(GameType type) {
        var s = server();
        if (s != null && type.mcc$modern() != null) s.setGameType(type.mcc$modern());
        else gameType = type;
    }

    public boolean isHardcoreModeEnabled() {
        return level != null ? level.getLevelData().isHardcore() : hardcore;
    }

    public void setHardcore(boolean value) {
        hardcore = value;
    }

    public WorldType getTerrainType() {
        return level != null ? WorldType.mcc$of(level) : terrainType;
    }

    public void setTerrainType(WorldType type) {
        terrainType = type;
    }

    public String getGeneratorOptions() {
        return generatorOptions == null ? "" : generatorOptions;
    }

    public boolean areCommandsAllowed() {
        var s = server();
        return s != null ? s.isAllowCommands() : allowCommands;
    }

    public void setAllowCommands(boolean allow) {
        allowCommands = allow;
    }

    public boolean isInitialized() {
        var s = server();
        return s != null ? s.isInitialized() : initialized;
    }

    public void setServerInitialized(boolean value) {
        var s = server();
        if (s != null) s.setInitialized(value);
        else initialized = value;
    }

    public GameRules getGameRulesInstance() {
        if (level != null) return GameRules.mcc$of(level.getGameRules(), level.getServer());
        if (gameRules == null) gameRules = new GameRules();
        return gameRules;
    }

    public EnumDifficulty getDifficulty() {
        return level != null ? EnumDifficulty.mcc$of(level.getDifficulty()) : difficulty;
    }

    public void setDifficulty(EnumDifficulty value) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl && value != null) sl.getServer().setDifficulty(value.mcc$modern(), true);
        else difficulty = value;
    }

    public boolean isDifficultyLocked() {
        return level != null ? level.getLevelData().isDifficultyLocked() : difficultyLocked;
    }

    public void setDifficultyLocked(boolean locked) {
        difficultyLocked = locked;
    }

    public double getBorderCenterX() {
        return level != null ? level.getWorldBorder().getCenterX() : 0.0D;
    }

    public double getBorderCenterZ() {
        return level != null ? level.getWorldBorder().getCenterZ() : 0.0D;
    }

    public double getBorderSize() {
        return level != null ? level.getWorldBorder().getSize() : 6.0E7D;
    }

    /** Per-dimension data Forge kept in level.dat (by dimension id); kept for the session. */
    public synchronized NBTTagCompound getDimensionData(int dimension) {
        NBTTagCompound tag = dimensionData.get(dimension);
        return tag == null ? new NBTTagCompound() : tag;
    }

    public synchronized void setDimensionData(int dimension, NBTTagCompound tag) {
        dimensionData.put(dimension, tag);
    }

    public NBTTagCompound getDimensionData(legacyconnector.v1_12_2.net.minecraft.world.DimensionType type) {
        return getDimensionData(type.getId());
    }

    public void setDimensionData(legacyconnector.v1_12_2.net.minecraft.world.DimensionType type, NBTTagCompound tag) {
        setDimensionData(type.getId(), tag);
    }

    public int getVersionId() {
        return 1343;
    }

    public boolean isVersionSnapshot() {
        return false;
    }

    public String getVersionName() {
        return "1.12.2";
    }

    /** Forge: a copy of the level.dat data compound with the player tag. */
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound cloneNBTCompound(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound playerNbt) {
        legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound();
        tag.setString("LevelName", getWorldName() == null ? "" : getWorldName());
        tag.setLong("RandomSeed", getSeed());
        tag.setLong("Time", getWorldTotalTime());
        tag.setLong("DayTime", getWorldTime());
        tag.setLong("LastPlayed", getLastTimePlayed());
        tag.setBoolean("hardcore", isHardcoreModeEnabled());
        tag.setBoolean("allowCommands", areCommandsAllowed());
        tag.setString("generatorName", getTerrainType() == null ? "default" : getTerrainType().getName());
        tag.setString("generatorOptions", getGeneratorOptions() == null ? "" : getGeneratorOptions());
        if (playerNbt != null) tag.setTag("Player", playerNbt);
        return tag;
    }
}
