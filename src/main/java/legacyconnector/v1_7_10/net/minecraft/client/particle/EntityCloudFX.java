package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "cloud": a big white puff that a player standing in it pushes down. */
public class EntityCloudFX extends EntityFX {
    float field_70569_a;

    public EntityCloudFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        float size = 2.5F;
        mcc$scaleMotion(0.10000000149011612D, 0.10000000149011612D, 0.10000000149011612D);
        setVelocity(mcc$get$motionX(this) + vx, mcc$get$motionY(this) + vy, mcc$get$motionZ(this) + vz);
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F - (float) (Math.random() * 0.30000001192092896D);
        this.particleScale *= 0.75F;
        this.particleScale *= size;
        this.field_70569_a = this.particleScale;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.3D));
        this.particleMaxAge = (int) (this.particleMaxAge * size);
        mcc$set$noClip(this, false);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        this.particleScale = field_70569_a * mcc$growIn(partialTicks);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        setParticleTextureIndex(mcc$ageCell());
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.9599999785423279D, 0.9599999785423279D, 0.9599999785423279D);
        World world = mcc$world();
        EntityPlayer player = world == null ? null : world.getClosestPlayerToEntity(this, 2.0D);
        if (player != null) {
            double feet = player.mcc$entity != null ? player.mcc$entity.getY() : player.mcc$y(); // boundingBox.minY
            if (mcc$y() > feet) {
                double y = mcc$y() + (feet - mcc$y()) * 0.2D;
                mcc$set$motionY(this, mcc$get$motionY(this) + (mcc$get$motionY(player) - mcc$get$motionY(this)) * 0.2D);
                setPosition(mcc$x(), y, mcc$z());
            }
        }
        mcc$groundFriction();
    }
}
