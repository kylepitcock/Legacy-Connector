package legacyconnector.v1_7_10.net.minecraft.world;

/** The Nether's provider: one biome (Hell), no sky, red fog, a fixed half-light sky; mods extend it for nether-like dimensions. */
public class WorldProviderHell extends WorldProvider {
    public WorldProviderHell() {
    }

    @Override
    protected void registerWorldChunkManager() {
        this.worldChunkMgr = new legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManagerHell(
                legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.hell, 0.0F);
        this.isHellWorld = true;
        this.hasNoSky = true;
        this.dimensionId = -1;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 getFogColor(float celestialAngle, float partialTicks) {
        return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(0.2D, 0.03D, 0.03D);
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider createChunkGenerator() {
        return worldObj == null ? null : new legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderHell(worldObj, worldObj.getSeed());
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }

    public boolean canCoordinateBeSpawn(int x, int z) {
        return false;
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        return 0.5F;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public boolean doesXZShowFog(int x, int z) {
        return true;
    }

    @Override
    public String getDimensionName() {
        return "Nether";
    }
}
