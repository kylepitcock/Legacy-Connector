package legacyconnector.v1_12_2.net.minecraft.world;

/**
 * 1.12.2 world provider of a world: what kind of dimension it is (id, sky, nether-ness, respawning). Answers from the modern
 * level's dimension type.
 */
public class WorldProvider {
    protected World world;
    protected boolean nether;
    protected boolean hasSkyLight = true;
    private int mcc$dimension;

    public WorldProvider() {
    }

    WorldProvider(World world) {
        setWorld(world);
    }

    public final void setWorld(World world) {
        this.world = world;
        var level = world == null ? null : world.mcc$level();
        if (level != null) {
            this.mcc$dimension = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.dimensionId(level);
            this.nether = level.dimensionType().ultraWarm();
            this.hasSkyLight = level.dimensionType().hasSkyLight();
        }
    }

    private net.minecraft.world.level.dimension.DimensionType mcc$type() {
        var level = world == null ? null : world.mcc$level();
        return level == null ? null : level.dimensionType();
    }

    public int getDimension() {
        return mcc$dimension;
    }

    public void setDimension(int dimension) {
        this.mcc$dimension = dimension;
    }

    public DimensionType getDimensionType() {
        for (DimensionType type : DimensionType.values()) if (type.getId() == mcc$dimension) return type;
        return DimensionType.OVERWORLD;
    }

    public boolean isSurfaceWorld() {
        var type = mcc$type();
        return type != null && type.natural();
    }

    public boolean isNether() {
        return nether;
    }

    public boolean hasSkyLight() {
        return hasSkyLight;
    }

    public boolean hasNoSky() {
        return !hasSkyLight;
    }

    public boolean canRespawnHere() {
        var type = mcc$type();
        return type == null || type.natural();
    }

    public boolean doesWaterVaporize() {
        var type = mcc$type();
        return type != null && type.ultraWarm();
    }

    public double getMovementFactor() {
        var type = mcc$type();
        return type == null ? 1.0D : type.coordinateScale();
    }

    public int getAverageGroundLevel() {
        var level = world == null ? null : world.mcc$level();
        return level == null ? 64 : level.getSeaLevel() + 1;
    }

    public int getActualHeight() {
        var type = mcc$type();
        return type == null ? 256 : type.logicalHeight();
    }

    public int getHeight() {
        var type = mcc$type();
        return type == null ? 256 : type.height();
    }

    public float getCloudHeight() {
        return 128.0F;
    }

    public boolean isDaytime() {
        var level = world == null ? null : world.mcc$level();
        return level != null && level.isDay();
    }

    public long getWorldTime() {
        var level = world == null ? null : world.mcc$level();
        return level == null ? 0 : level.getDayTime();
    }

    public String getSaveFolder() {
        return mcc$dimension == 0 ? null : "DIM" + mcc$dimension;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getSpawnPoint() {
        var level = world == null ? null : world.mcc$level();
        if (level == null) return legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.ORIGIN;
        var p = level.getSharedSpawnPos();
        return new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(p.getX(), p.getY(), p.getZ());
    }
}
