package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2 world: a view over a modern level. */
public class World {
    private final net.minecraft.world.level.Level level;

    public final boolean isRemote;

    protected World(net.minecraft.world.level.Level level) {
        this.level = level;
        this.isRemote = level != null && level.isClientSide();
    }

    public net.minecraft.world.level.Level mcc$level() {
        return level;
    }
}
