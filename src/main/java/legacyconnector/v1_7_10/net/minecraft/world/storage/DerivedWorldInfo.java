package legacyconnector.v1_7_10.net.minecraft.world.storage;

import legacyconnector.v1_7_10.net.minecraft.world.WorldType;

/**
 * The world info of a secondary dimension: every value is read from the overworld's info, and the setters change nothing (the
 * overworld owns the time, weather and spawn). Mods extend it to give their dimensions their own values.
 */
public class DerivedWorldInfo extends WorldInfo {
    public final WorldInfo theWorldInfo;

    public DerivedWorldInfo(WorldInfo info) {
        super();
        this.theWorldInfo = info;
    }

    @Override
    public long getSeed() {
        return theWorldInfo.getSeed();
    }

    @Override
    public int getSpawnX() {
        return theWorldInfo.getSpawnX();
    }

    @Override
    public int getSpawnY() {
        return theWorldInfo.getSpawnY();
    }

    @Override
    public int getSpawnZ() {
        return theWorldInfo.getSpawnZ();
    }

    @Override
    public long getWorldTotalTime() {
        return theWorldInfo.getWorldTotalTime();
    }

    @Override
    public long getWorldTime() {
        return theWorldInfo.getWorldTime();
    }

    @Override
    public String getWorldName() {
        return theWorldInfo.getWorldName();
    }

    @Override
    public int getSaveVersion() {
        return theWorldInfo.getSaveVersion();
    }

    @Override
    public boolean isThundering() {
        return theWorldInfo.isThundering();
    }

    @Override
    public boolean isRaining() {
        return theWorldInfo.isRaining();
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType getGameType() {
        return theWorldInfo.getGameType();
    }

    @Override
    public boolean isMapFeaturesEnabled() {
        return theWorldInfo.isMapFeaturesEnabled();
    }

    @Override
    public boolean isHardcoreModeEnabled() {
        return theWorldInfo.isHardcoreModeEnabled();
    }

    @Override
    public WorldType getTerrainType() {
        return theWorldInfo.getTerrainType();
    }

    @Override
    public String getGeneratorOptions() {
        return theWorldInfo.getGeneratorOptions();
    }

    @Override
    public boolean areCommandsAllowed() {
        return theWorldInfo.areCommandsAllowed();
    }

    @Override
    public boolean isInitialized() {
        return theWorldInfo.isInitialized();
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.world.GameRules getGameRulesInstance() {
        return theWorldInfo.getGameRulesInstance();
    }

    @Override
    public int getVanillaDimension() {
        return theWorldInfo.getVanillaDimension();
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase getAdditionalProperty(String name) {
        return theWorldInfo.getAdditionalProperty(name);
    }

    @Override
    public void setAdditionalProperties(java.util.Map<String, legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase> properties) {
        theWorldInfo.setAdditionalProperties(properties);
    }

    // the overworld owns these: a derived info ignores them

    @Override
    public void setTerrainType(WorldType type) {
    }

    @Override
    public void setRaining(boolean raining) {
    }

    @Override
    public void setThundering(boolean thundering) {
    }

    @Override
    public void setRainTime(int ticks) {
    }

    @Override
    public void setThunderTime(int ticks) {
    }

    @Override
    public void setWorldTime(long time) {
    }

    @Override
    public void setSpawnPosition(int x, int y, int z) {
    }

    @Override
    public void setSpawnX(int x) {
    }

    @Override
    public void setSpawnY(int y) {
    }

    @Override
    public void setSpawnZ(int z) {
    }

    @Override
    public void setServerInitialized(boolean initialized) {
    }
}
