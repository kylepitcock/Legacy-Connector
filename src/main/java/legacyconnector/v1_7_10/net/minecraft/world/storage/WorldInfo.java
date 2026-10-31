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

    private Level level() {
        return world == null ? null : world.mcc$fullLevel();
    }

    public long getSeed() {
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
        Level level = level();
        return level != null && level.getServer() != null ? level.getServer().getWorldData().getLevelName() : "world";
    }

    public long getWorldTime() {
        Level level = level();
        return level == null ? 0L : level.getDayTime();
    }

    public long getWorldTotalTime() {
        Level level = level();
        return level == null ? 0L : level.getGameTime();
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
        Level level = level();
        return level != null && level.getLevelData().isHardcore();
    }

    public boolean areCommandsAllowed() {
        return true;
    }

    public boolean isMapFeaturesEnabled() {
        Level level = level();
        return level == null || level.getServer() == null || level.getServer().getWorldData().worldGenOptions().generateStructures();
    }

    public int getSpawnX() {
        Level level = level();
        return level == null ? 0 : level.getSharedSpawnPos().getX();
    }

    public int getSpawnY() {
        Level level = level();
        return level == null ? 64 : level.getSharedSpawnPos().getY();
    }

    public int getSpawnZ() {
        Level level = level();
        return level == null ? 0 : level.getSharedSpawnPos().getZ();
    }

    public legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType getGameType() {
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
}
