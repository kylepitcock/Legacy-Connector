package mccompat.v1_7_10.net.minecraft.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Read-only dimension facts. 1.7.10 numeric dimension ids: 0 overworld, -1 nether, 1 end, others hashed. */
public class WorldProvider {
    public final int dimensionId;
    public final boolean hasNoSky;
    public final boolean isHellWorld;
    private final String mcc$name;

    public WorldProvider(ResourceKey<Level> key, boolean hasSkyLight, boolean ultraWarm) {
        this.dimensionId = dimensionId(key);
        this.hasNoSky = !hasSkyLight;
        this.isHellWorld = ultraWarm;
        this.mcc$name = key.location().getPath();
    }

    public static int dimensionId(ResourceKey<Level> key) {
        if (key == Level.OVERWORLD) return 0;
        if (key == Level.NETHER) return -1;
        if (key == Level.END) return 1;
        return 2 + Math.floorMod(key.location().toString().hashCode(), 1 << 20);
    }

    public String getDimensionName() {
        return switch (dimensionId) {
            case 0 -> "Overworld";
            case -1 -> "Nether";
            case 1 -> "The End";
            default -> mcc$name;
        };
    }

    public boolean isSurfaceWorld() {
        return dimensionId == 0;
    }

    public boolean canRespawnHere() {
        return dimensionId == 0;
    }

    public int getAverageGroundLevel() {
        return 64;
    }

    public int getActualHeight() {
        return hasNoSky ? 128 : 256;
    }

    public String getSaveFolder() {
        return dimensionId == 0 ? null : "DIM" + dimensionId;
    }
}
