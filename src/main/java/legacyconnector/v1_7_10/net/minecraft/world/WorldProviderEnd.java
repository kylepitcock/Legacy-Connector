package legacyconnector.v1_7_10.net.minecraft.world;

/** The End's provider: one biome (Sky), no sky light or day cycle, dark purple fog; mods extend it for end-like dimensions. */
public class WorldProviderEnd extends WorldProvider {
    public WorldProviderEnd() {
    }

    @Override
    protected void registerWorldChunkManager() {
        this.worldChunkMgr = new legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManagerHell(
                legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.sky, 0.0F);
        this.dimensionId = 1;
        this.hasNoSky = true;
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
    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 getFogColor(float celestialAngle, float partialTicks) {
        float f = net.minecraft.util.Mth.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F;
        f = Math.max(0.0F, Math.min(1.0F, f));
        float r = 0.627451F * (f * 0.0F + 0.15F);
        float g = 0.5019608F * (f * 0.0F + 0.15F);
        float b = 0.627451F * (f * 0.0F + 0.15F);
        return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(r, g, b);
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

    public boolean canCoordinateBeSpawn(int x, int z) {
        return worldObj != null && worldObj.getTopBlock(x, z).getMaterial().blocksMovement();
    }

    @Override
    public int getAverageGroundLevel() {
        return 50;
    }

    @Override
    public boolean doesXZShowFog(int x, int z) {
        return true;
    }

    @Override
    public String getDimensionName() {
        return "The End";
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates getEntrancePortalLocation() {
        return new legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates(100, 50, 0);
    }
}
