package legacyconnector.v1_12_2.net.minecraft.world;

/** The end: no sky light, fixed celestial angle, spawn on the obsidian platform. */
public class WorldProviderEnd extends WorldProvider {
    public WorldProviderEnd() {
    }

    @Override
    public void init() {
        this.biomeProvider = legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider.mcc$of(world);
        this.hasSkyLight = false;
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        return 0.0F;
    }

    @Override
    public float[] calcSunriseSunsetColors(float celestialAngle, float partialTicks) {
        return null;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getFogColor(float celestialAngle, float partialTicks) {
        float f = (float) Math.cos(celestialAngle * (Math.PI * 2D)) * 2.0F + 0.5F;
        f = Math.max(0.0F, Math.min(1.0F, f));
        return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(0.627451F * (f * 0.0F + 0.15F), 0.5019608F * (f * 0.0F + 0.15F),
                0.627451F * (f * 0.0F + 0.15F));
    }

    @Override
    public boolean isSkyColored() {
        return false;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }

    @Override
    public float getCloudHeight() {
        return 8.0F;
    }

    @Override
    public boolean canCoordinateBeSpawn(int x, int z) {
        return world.getGroundAboveSeaLevel(new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(x, 0, z)).getMaterial().blocksMovement();
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getSpawnCoordinate() {
        return new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(100, 50, 0);
    }

    @Override
    public int getAverageGroundLevel() {
        return 50;
    }

    @Override
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }

    @Override
    public DimensionType getDimensionType() {
        return DimensionType.THE_END;
    }

    /** Phantom: redirected to {@link #mcc$get$dragonFightManager}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.world.end.DragonFightManager dragonFightManager;

    public static legacyconnector.v1_12_2.net.minecraft.world.end.DragonFightManager mcc$get$dragonFightManager(WorldProviderEnd p) {
        return p.getDragonFightManager();
    }

    /** The End's dragon fight (the modern level's), or null on a client or a level without one. */
    public legacyconnector.v1_12_2.net.minecraft.world.end.DragonFightManager getDragonFightManager() {
        return world == null ? null : legacyconnector.v1_12_2.net.minecraft.world.end.DragonFightManager.mcc$of(world.mcc$level());
    }

    @Override
    public WorldSleepResult canSleepAt(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return WorldSleepResult.BED_EXPLODES;
    }
}
