package legacyconnector.vb1_7_3.net.minecraft.world;

/** Beta world metadata reads the same live clock as the owning world view. */
public final class WorldProperties {
    private final World world;
    public WorldProperties(World world){this.world=java.util.Objects.requireNonNull(world);}
    public long getTime(){return world.getTime();}
}
