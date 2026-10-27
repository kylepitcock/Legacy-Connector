package legacyconnector.v1_12_2.net.minecraft.world;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider;

/**
 * 1.12.2 dimension description (Forge's version, with its hooks). The worlds of modern levels get the vanilla providers for the
 * overworld, nether and end ({@link #mcc$forLevel}) and a surface provider for other dimensions; what they report comes from the
 * modern dimension type. Mods subclass it for their own dimensions.
 */
public abstract class WorldProvider {
    public static final float[] MOON_PHASE_FACTORS = {1.0F, 0.75F, 0.5F, 0.25F, 0.0F, 0.25F, 0.5F, 0.75F};

    protected World world;
    protected WorldType terrainType;
    protected String generatorSettings;
    protected BiomeProvider biomeProvider;
    protected boolean doesWaterVaporize;
    protected boolean nether;
    protected boolean hasSkyLight = true;
    protected final float[] lightBrightnessTable = new float[16];
    private final float[] colorsSunriseSunset = new float[4];
    private int dimensionId;

    protected WorldProvider() {
    }

    public final void setWorld(World world) {
        this.world = world;
        this.terrainType = world.getWorldInfo().getTerrainType();
        this.generatorSettings = world.getWorldInfo().getGeneratorOptions();
        init();
        generateLightBrightnessTable();
    }

    protected void generateLightBrightnessTable() {
        for (int i = 0; i <= 15; i++) {
            float f1 = 1.0F - i / 15.0F;
            lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F);
        }
    }

    protected void init() {
        this.hasSkyLight = true;
        this.biomeProvider = BiomeProvider.mcc$of(world);
    }

    /** The dimension's legacy chunk generator; null means the modern game generates it. */
    public legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator createChunkGenerator() {
        return null;
    }

    public boolean canCoordinateBeSpawn(int x, int z) {
        BlockPos pos = new BlockPos(x, 0, z);
        return world.getBiome(pos).ignorePlayerSpawnSuitability() || org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(world.getGroundAboveSeaLevel(pos)).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
    }

    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        int i = (int) (worldTime % 24000L);
        float f = (i + partialTicks) / 24000.0F - 0.25F;
        if (f < 0.0F) ++f;
        if (f > 1.0F) --f;
        float f1 = 1.0F - (float) ((Math.cos(f * Math.PI) + 1.0D) / 2.0D);
        return f + (f1 - f) / 3.0F;
    }

    public int getMoonPhase(long worldTime) {
        return (int) (worldTime / 24000L % 8L + 8L) % 8;
    }

    public boolean isSurfaceWorld() {
        return true;
    }

    public float[] calcSunriseSunsetColors(float celestialAngle, float partialTicks) {
        float f1 = (float) Math.cos(celestialAngle * (Math.PI * 2D)) - 0.0F;
        if (f1 >= -0.4F && f1 <= 0.4F) {
            float f3 = (f1 + 0.0F) / 0.4F * 0.5F + 0.5F;
            float f4 = 1.0F - (1.0F - (float) Math.sin(f3 * Math.PI)) * 0.99F;
            f4 = f4 * f4;
            colorsSunriseSunset[0] = f3 * 0.3F + 0.7F;
            colorsSunriseSunset[1] = f3 * f3 * 0.7F + 0.2F;
            colorsSunriseSunset[2] = f3 * f3 * 0.0F + 0.2F;
            colorsSunriseSunset[3] = f4;
            return colorsSunriseSunset;
        }
        return null;
    }

    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        float f = (float) Math.cos(celestialAngle * (Math.PI * 2D)) * 2.0F + 0.5F;
        f = Math.max(0.0F, Math.min(1.0F, f));
        return new Vec3d(0.7529412F * (f * 0.94F + 0.06F), 0.84705883F * (f * 0.94F + 0.06F), 1.0F * (f * 0.91F + 0.09F));
    }

    public boolean canRespawnHere() {
        return true;
    }

    public float getCloudHeight() {
        return 128.0F;
    }

    public boolean isSkyColored() {
        return true;
    }

    public BlockPos getSpawnCoordinate() {
        return null;
    }

    public int getAverageGroundLevel() {
        return terrainType == WorldType.FLAT ? 4 : world.getSeaLevel() + 1;
    }

    public double getVoidFogYFactor() {
        return terrainType == WorldType.FLAT ? 1.0D : 0.03125D;
    }

    public boolean doesXZShowFog(int x, int z) {
        return false;
    }

    public BiomeProvider getBiomeProvider() {
        return biomeProvider;
    }

    public boolean doesWaterVaporize() {
        return doesWaterVaporize;
    }

    public boolean hasSkyLight() {
        return hasSkyLight;
    }

    public boolean isNether() {
        return nether;
    }

    public float[] getLightBrightnessTable() {
        return lightBrightnessTable;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.border.WorldBorder createWorldBorder() {
        return new legacyconnector.v1_12_2.net.minecraft.world.border.WorldBorder();
    }

    public void onPlayerAdded(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player) {
    }

    public void onPlayerRemoved(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player) {
    }

    public abstract DimensionType getDimensionType();

    public void onWorldSave() {
    }

    public void onWorldUpdateEntities() {
    }

    public boolean canDropChunk(int x, int z) {
        return true;
    }

    // ------------------------------------------------------------------ Forge

    public void setDimension(int dim) {
        this.dimensionId = dim;
    }

    public int getDimension() {
        return dimensionId;
    }

    public String getSaveFolder() {
        return dimensionId == 0 ? null : "DIM" + dimensionId;
    }

    public double getMovementFactor() {
        return this instanceof WorldProviderHell ? 8.0D : 1.0D;
    }

    public BlockPos getRandomizedSpawnPoint() {
        return world.getSpawnPoint();
    }

    public boolean shouldMapSpin(String entity, double x, double z, double rotation) {
        return dimensionId < 0 || dimensionId > 1;
    }

    public int getRespawnDimension(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player) {
        return canRespawnHere() ? dimensionId : 0;
    }

    public boolean isDaytime() {
        return world.getSkylightSubtracted() < 4;
    }

    public float getSunBrightnessFactor(float partialTicks) {
        return world.getSunBrightnessFactor(partialTicks);
    }

    public float getCurrentMoonPhaseFactor() {
        return MOON_PHASE_FACTORS[getMoonPhase(world.getWorldTime())];
    }

    public void setAllowedSpawnTypes(boolean allowHostile, boolean allowPeaceful) {
    }

    public void calculateInitialWeather() {
    }

    public void updateWeather() {
    }

    public boolean canBlockFreeze(BlockPos pos, boolean byWater) {
        return world.canBlockFreezeBody(pos, byWater);
    }

    public boolean canSnowAt(BlockPos pos, boolean checkLight) {
        return world.canSnowAtBody(pos, checkLight);
    }

    public void setWorldTime(long time) {
        world.getWorldInfo().setWorldTime(time);
    }

    public long getSeed() {
        return world.getWorldInfo().getSeed();
    }

    public long getWorldTime() {
        return world.getWorldInfo().getWorldTime();
    }

    public BlockPos getSpawnPoint() {
        var info = world.getWorldInfo();
        return new BlockPos(info.getSpawnX(), info.getSpawnY(), info.getSpawnZ());
    }

    public void setSpawnPoint(BlockPos pos) {
        world.getWorldInfo().setSpawn(pos);
    }

    public boolean canMineBlock(EntityPlayer player, BlockPos pos) {
        return world.canMineBlockBody(player, pos);
    }

    public boolean isBlockHighHumidity(BlockPos pos) {
        return world.getBiome(pos).isHighHumidity();
    }

    public int getHeight() {
        return 256;
    }

    public int getActualHeight() {
        return nether ? 128 : 256;
    }

    public double getHorizon() {
        return world.getWorldInfo().getTerrainType().getHorizon(world);
    }

    public void resetRainAndThunder() {
        var info = world.getWorldInfo();
        info.setRainTime(0);
        info.setRaining(false);
        info.setThunderTime(0);
        info.setThundering(false);
    }

    public boolean canDoLightning(legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk chunk) {
        return true;
    }

    public boolean canDoRainSnowIce(legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk chunk) {
        return true;
    }

    public Biome getBiomeForCoords(BlockPos pos) {
        return world.getBiomeForCoordsBody(pos);
    }

    public float getStarBrightness(float partialTicks) {
        return world.getStarBrightnessBody(partialTicks);
    }

    public float getSunBrightness(float partialTicks) {
        return world.getSunBrightnessBody(partialTicks);
    }

    public Vec3d getSkyColor(legacyconnector.v1_12_2.net.minecraft.entity.Entity camera, float partialTicks) {
        return world.getSkyColorBody(camera, partialTicks);
    }

    public Vec3d getCloudColor(float partialTicks) {
        return world.getCloudColorBody(partialTicks);
    }

    public boolean isDimensionalPortalUsable() {
        return true;
    }

    // ------------------------------------------------------------------ modern levels

    /** A provider for a modern level: the vanilla one of the dimension type, with the dimension's id. */
    public static WorldProvider mcc$forLevel(net.minecraft.world.level.Level level, World world) {
        WorldProvider p;
        var key = level.dimension();
        if (key == net.minecraft.world.level.Level.NETHER) p = new WorldProviderHell();
        else if (key == net.minecraft.world.level.Level.END) p = new WorldProviderEnd();
        else p = new WorldProviderSurface();
        p.setDimension(legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$idOf(key));
        try {
            p.setWorld(world);
        } catch (RuntimeException | LinkageError e) {
            p.world = world;
        }
        var type = level.dimensionType();
        p.hasSkyLight = type.hasSkyLight();
        p.doesWaterVaporize = type.ultraWarm();
        if (type.ultraWarm() && !type.hasSkyLight()) p.nether = true;
        return p;
    }
}
