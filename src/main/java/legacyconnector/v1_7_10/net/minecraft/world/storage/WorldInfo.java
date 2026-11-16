package legacyconnector.v1_7_10.net.minecraft.world.storage;

import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import net.minecraft.world.level.Level;

/** Read-mostly view of a modern level's data in the legacy shape. */
public class WorldInfo {
    /** World type legacy code sees for the overworld (a mod's own type, e.g. BIOMESOP, when its biomes are placed). */
    public static volatile WorldType mcc$overworldType = WorldType.DEFAULT;

    private final World world;

    protected WorldInfo() {
        this.world = null;
    }

    public WorldInfo(World world) {
        this.world = world;
    }

    /** The level.dat data of a detached view (see {@link #WorldInfo(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound)}); null for a view of a world. */
    private legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound mcc$nbt;

    /** A standalone info read from level.dat style data (seed, name, times, spawn, flags): what mods build to inspect a saved world. */
    public WorldInfo(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound nbt) {
        this.world = null;
        this.mcc$nbt = nbt;
    }

    /** The detached data when this is not a view of a world, else null. */
    private legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound saved() {
        return world == null ? mcc$nbt : null;
    }

    private Level level() {
        return world == null ? null : world.mcc$fullLevel();
    }

    public long getSeed() {
        if (saved() != null) return saved().getLong("RandomSeed");
        return world == null ? 0L : world.getSeed();
    }

    public WorldType getTerrainType() {
        Level level = level();
        if (level == null || level.dimension() == Level.OVERWORLD) return mcc$overworldType;
        return WorldType.DEFAULT;
    }

    public void setTerrainType(WorldType type) {
        if (type != null) mcc$overworldType = type;
    }

    public String getGeneratorOptions() {
        return "";
    }

    /** The dimension a world with a modded provider uses for its vanilla fields: always the overworld here. */
    public int getVanillaDimension() {
        return 0;
    }

    public String getWorldName() {
        if (saved() != null) return saved().hasKey("LevelName") ? saved().getString("LevelName") : "world";
        Level level = level();
        return level != null && level.getServer() != null ? level.getServer().getWorldData().getLevelName() : "world";
    }

    public long getWorldTime() {
        if (saved() != null) return saved().getLong("DayTime");
        Level level = level();
        return level == null ? 0L : level.getDayTime();
    }

    public long getWorldTotalTime() {
        if (saved() != null) return saved().getLong("Time");
        Level level = level();
        return level == null ? 0L : level.getGameTime();
    }

    /** Sets the world's total tick count (1.7.10's {@code incrementTotalWorldTime}; the game time of the level). */
    public void incrementTotalWorldTime(long time) {
        data(d -> d.setGameTime(time));
    }

    public boolean isRaining() {
        Level level = level();
        return level != null && level.isRaining();
    }

    public boolean isThundering() {
        Level level = level();
        return level != null && level.isThundering();
    }

    private void weather(java.util.function.Consumer<net.minecraft.server.level.ServerLevel> change) {
        Level level = level();
        if (level instanceof net.minecraft.server.level.ServerLevel server) change.accept(server);
    }

    /** Weather is shared by all dimensions and lives in the overworld's level data; each setter changes only its own field, as in 1.7.10. */
    private void data(java.util.function.Consumer<net.minecraft.world.level.storage.ServerLevelData> change) {
        weather(l -> change.accept(l.getServer().getWorldData().overworldData()));
    }

    public void setRaining(boolean raining) {
        data(d -> {
            d.setRaining(raining);
            if (!raining) d.setClearWeatherTime(0);
        });
    }

    public void setThundering(boolean thundering) {
        data(d -> d.setThundering(thundering));
    }

    public void setRainTime(int ticks) {
        data(d -> {
            d.setRainTime(ticks);
            if (ticks > 0) d.setClearWeatherTime(0);
        });
    }

    public void setThunderTime(int ticks) {
        data(d -> d.setThunderTime(ticks));
    }

    public int getRainTime() {
        Level level = level();
        return level instanceof net.minecraft.server.level.ServerLevel l ? l.getServer().getWorldData().overworldData().getRainTime() : 0;
    }

    public int getThunderTime() {
        Level level = level();
        return level instanceof net.minecraft.server.level.ServerLevel l ? l.getServer().getWorldData().overworldData().getThunderTime() : 0;
    }

    public void setWorldTime(long time) {
        weather(l -> l.setDayTime(time));
    }

    public boolean isHardcoreModeEnabled() {
        if (saved() != null) return saved().getBoolean("hardcore");
        Level level = level();
        return level != null && level.getLevelData().isHardcore();
    }

    public boolean areCommandsAllowed() {
        if (saved() != null) return !saved().hasKey("allowCommands") || saved().getBoolean("allowCommands");
        return true;
    }

    public boolean isMapFeaturesEnabled() {
        if (saved() != null) return !saved().hasKey("MapFeatures") || saved().getBoolean("MapFeatures");
        Level level = level();
        return level == null || level.getServer() == null || level.getServer().getWorldData().worldGenOptions().generateStructures();
    }

    public int getSpawnX() {
        if (saved() != null) return saved().getInteger("SpawnX");
        Level level = level();
        return level == null ? 0 : level.getSharedSpawnPos().getX();
    }

    public int getSpawnY() {
        if (saved() != null) return saved().hasKey("SpawnY") ? saved().getInteger("SpawnY") : 64;
        Level level = level();
        return level == null ? 64 : level.getSharedSpawnPos().getY();
    }

    public int getSpawnZ() {
        if (saved() != null) return saved().getInteger("SpawnZ");
        Level level = level();
        return level == null ? 0 : level.getSharedSpawnPos().getZ();
    }

    public legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType getGameType() {
        if (saved() != null) return legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.getByID(saved().getInteger("GameType"));
        var current = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (current != null) return legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.getByID(current.getDefaultGameType().getId());
        return legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.SURVIVAL;
    }

    /** The save format version of level.dat: 19133, the Anvil format every 1.7.10 world was saved in. */
    public int getSaveVersion() {
        return 19133;
    }

    private boolean mcc$initialized;

    /** Whether the world finished its first setup (spawn chosen): a loaded level always has; a mod's own world once it says so. */
    public boolean isInitialized() {
        if (saved() != null) return saved().getBoolean("initialized");
        return level() != null || mcc$initialized;
    }

    public void setServerInitialized(boolean initialized) {
        this.mcc$initialized = initialized;
    }

    public legacyconnector.v1_7_10.net.minecraft.world.GameRules getGameRulesInstance() {
        return legacyconnector.v1_7_10.net.minecraft.world.GameRules.mcc$of(level());
    }

    /** Moves the world spawn (the server's shared spawn point). */
    public void setSpawnPosition(int x, int y, int z) {
        weather(l -> l.setDefaultSpawnPos(new net.minecraft.core.BlockPos(x, y, z), l.getSharedSpawnAngle()));
    }

    public void setSpawnX(int x) {
        setSpawnPosition(x, getSpawnY(), getSpawnZ());
    }

    public void setSpawnY(int y) {
        setSpawnPosition(getSpawnX(), y, getSpawnZ());
    }

    public void setSpawnZ(int z) {
        setSpawnPosition(getSpawnX(), getSpawnY(), z);
    }

    /** Forge: extra level.dat compounds (FML's data); none are read from the modern save, mods may set their own. */
    private java.util.Map<String, legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase> mcc$additional;

    public void setAdditionalProperties(java.util.Map<String, legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase> properties) {
        if (mcc$additional == null) this.mcc$additional = properties;
    }

    public legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase getAdditionalProperty(String name) {
        return mcc$additional == null ? null : mcc$additional.get(name);
    }

    /** The level data as 1.7.10 saved it ({@code level.dat}'s Data compound): a copy of the detached data, or the live world's values. */
    public legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound getNBTTagCompound() {
        if (saved() != null) return (legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound) saved().copy();
        legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
        tag.setLong("RandomSeed", getSeed());
        tag.setString("LevelName", getWorldName());
        tag.setLong("Time", getWorldTotalTime());
        tag.setLong("DayTime", getWorldTime());
        tag.setInteger("SpawnX", getSpawnX());
        tag.setInteger("SpawnY", getSpawnY());
        tag.setInteger("SpawnZ", getSpawnZ());
        tag.setBoolean("raining", isRaining());
        tag.setInteger("rainTime", getRainTime());
        tag.setBoolean("thundering", isThundering());
        tag.setInteger("thunderTime", getThunderTime());
        tag.setBoolean("hardcore", isHardcoreModeEnabled());
        tag.setBoolean("allowCommands", areCommandsAllowed());
        tag.setBoolean("MapFeatures", isMapFeaturesEnabled());
        tag.setBoolean("initialized", isInitialized());
        return tag;
    }
}
