package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "splash": a water droplet; a sideways velocity throws it out of the water. */
public class EntitySplashFX extends EntityRainFX {
    public EntitySplashFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z);
        this.particleGravity = 0.04F;
        nextTextureIndexX();
        if (vy == 0.0D && (vx != 0.0D || vz != 0.0D)) setVelocity(vx, vy + 0.1D, vz);
    }
}
