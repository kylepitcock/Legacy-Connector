package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The red-dust particle: a coloured puff that shrinks away. Mods spawn it with their own colour. */
public class EntityReddustFX extends EntityFX {
    private final float reddustParticleScale;

    public EntityReddustFX(World world, double x, double y, double z, float red, float green, float blue) {
        this(world, x, y, z, 1.0F, red, green, blue);
    }

    public EntityReddustFX(World world, double x, double y, double z, float scale, float red, float green, float blue) {
        super(world, x, y, z);
        if (red == 0.0F) red = 1.0F;
        float shade = (float) Math.random() * 0.4F + 0.6F;
        this.particleRed = ((float) (Math.random() * 0.2D) + 0.8F) * red * shade;
        this.particleGreen = ((float) (Math.random() * 0.2D) + 0.8F) * green * shade;
        this.particleBlue = ((float) (Math.random() * 0.2D) + 0.8F) * blue * shade;
        this.particleScale *= 0.75F;
        this.particleScale *= scale;
        this.reddustParticleScale = this.particleScale;
        this.particleMaxAge = (int) ((8.0D / (Math.random() * 0.8D + 0.2D)) * scale);
    }
}
