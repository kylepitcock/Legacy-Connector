package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The red-dust particle: a coloured puff that shrinks away. Mods spawn it with their own colour. */
public class EntityReddustFX extends EntityFX {
    float reddustParticleScale;

    public EntityReddustFX(World world, double x, double y, double z, float red, float green, float blue) {
        this(world, x, y, z, 1.0F, red, green, blue);
    }

    public EntityReddustFX(World world, double x, double y, double z, float scale, float red, float green, float blue) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        mcc$scaleMotion(0.1D, 0.1D, 0.1D);
        if (red == 0.0F) red = 1.0F;
        float shade = (float) Math.random() * 0.4F + 0.6F;
        this.particleRed = ((float) (Math.random() * 0.2D) + 0.8F) * red * shade;
        this.particleGreen = ((float) (Math.random() * 0.2D) + 0.8F) * green * shade;
        this.particleBlue = ((float) (Math.random() * 0.2D) + 0.8F) * blue * shade;
        this.particleScale *= 0.75F;
        this.particleScale *= scale;
        this.reddustParticleScale = this.particleScale;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
        this.particleMaxAge = (int) (this.particleMaxAge * scale);
        mcc$set$noClip(this, false);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        this.particleScale = reddustParticleScale * mcc$growIn(partialTicks);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        setParticleTextureIndex(mcc$ageCell());
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        if (mcc$y() == mcc$get$prevPosY(this)) mcc$scaleMotion(1.1D, 1.0D, 1.1D);
        mcc$scaleMotion(0.9599999785423279D, 0.9599999785423279D, 0.9599999785423279D);
        mcc$groundFriction();
    }
}
