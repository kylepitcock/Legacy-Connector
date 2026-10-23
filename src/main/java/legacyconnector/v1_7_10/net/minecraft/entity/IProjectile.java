package legacyconnector.v1_7_10.net.minecraft.entity;

/** Something that can be launched in a direction. */
public interface IProjectile {
    void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy);
}
