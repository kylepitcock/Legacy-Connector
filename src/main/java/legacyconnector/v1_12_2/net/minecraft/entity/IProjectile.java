package legacyconnector.v1_12_2.net.minecraft.entity;

/** Something thrown or shot (dispensers aim it with shoot). */
public interface IProjectile {
    void shoot(double x, double y, double z, float velocity, float inaccuracy);
}
