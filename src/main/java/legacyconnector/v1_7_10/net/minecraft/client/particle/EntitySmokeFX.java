package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A drifting smoke puff. */
public class EntitySmokeFX extends EntityFX {
    public EntitySmokeFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        this(world, x, y, z, vx, vy, vz, 1.0F);
    }

    public EntitySmokeFX(World world, double x, double y, double z, double vx, double vy, double vz, float scale) {
        super(world, x, y, z, vx, vy, vz);
        float shade = (float) (Math.random() * 0.3D);
        this.particleRed = shade;
        this.particleGreen = shade;
        this.particleBlue = shade;
        this.particleScale *= 0.75F * scale;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D) * scale);
    }
}
