package legacyconnector.v1_12_2.net.minecraft.world;

/** The nether: no sky light, water evaporates, brighter ambient light. */
public class WorldProviderHell extends WorldProvider {
    public WorldProviderHell() {
    }

    @Override
    public void init() {
        this.biomeProvider = legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider.mcc$of(world);
        this.doesWaterVaporize = true;
        this.nether = true;
        this.hasSkyLight = false;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getFogColor(float celestialAngle, float partialTicks) {
        return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(0.2D, 0.03D, 0.03D);
    }

    @Override
    protected void generateLightBrightnessTable() {
        for (int i = 0; i <= 15; i++) {
            float f1 = 1.0F - i / 15.0F;
            lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * 0.9F + 0.1F;
        }
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }

    @Override
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
    public legacyconnector.v1_12_2.net.minecraft.world.border.WorldBorder createWorldBorder() {
        return new legacyconnector.v1_12_2.net.minecraft.world.border.WorldBorder();
    }

    @Override
    public DimensionType getDimensionType() {
        return DimensionType.NETHER;
    }
}
