package mccompat.v1_7_10.net.minecraft.world.storage;

import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraft.world.WorldType;
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
}
