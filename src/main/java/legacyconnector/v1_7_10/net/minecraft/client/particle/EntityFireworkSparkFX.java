package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A firework star spark: coloured, fading to its fade colour, optionally twinkling or leaving a trail of sparks. */
public class EntityFireworkSparkFX extends EntityFX {
    public int baseTextureIndex = 160;
    /** Trail: leaves sparks behind while young. */
    public boolean field_92054_ax;
    /** Twinkle: blinks in the second part of its life. */
    public boolean field_92048_ay;
    public final EffectRenderer field_92047_az;
    public float fadeColourRed;
    public float fadeColourGreen;
    public float fadeColourBlue;
    public boolean hasFadeColour;

    public EntityFireworkSparkFX(World world, double x, double y, double z, double vx, double vy, double vz, EffectRenderer effects) {
        super(world, x, y, z);
        setVelocity(vx, vy, vz);
        this.field_92047_az = effects;
        this.particleScale *= 0.75F;
        this.particleMaxAge = 48 + rand.nextInt(12);
        mcc$set$noClip(this, false);
    }

    public void setTrail(boolean trail) {
        this.field_92054_ax = trail;
    }

    public void setTwinkle(boolean twinkle) {
        this.field_92048_ay = twinkle;
    }

    public void setColour(int rgb) {
        setRBGColorF((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F);
    }

    public void setFadeColour(int rgb) {
        this.fadeColourRed = (rgb >> 16 & 255) / 255.0F;
        this.fadeColourGreen = (rgb >> 8 & 255) / 255.0F;
        this.fadeColourBlue = (rgb & 255) / 255.0F;
        this.hasFadeColour = true;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        if (!field_92048_ay || particleAge < particleMaxAge / 3 || (particleAge + particleMaxAge) / 3 % 2 == 0) {
            super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
        }
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        if (particleAge > particleMaxAge / 2) {
            setAlphaF(1.0F - ((float) particleAge - (float) (particleMaxAge / 2)) / particleMaxAge);
            if (hasFadeColour) {
                particleRed += (fadeColourRed - particleRed) * 0.2F;
                particleGreen += (fadeColourGreen - particleGreen) * 0.2F;
                particleBlue += (fadeColourBlue - particleBlue) * 0.2F;
            }
        }
        setParticleTextureIndex(baseTextureIndex + mcc$ageCell());
        mcc$set$motionY(this, mcc$get$motionY(this) - 0.004D);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.9100000262260437D, 0.9100000262260437D, 0.9100000262260437D);
        mcc$groundFriction();
        if (field_92054_ax && particleAge < particleMaxAge / 2 && (particleAge + particleMaxAge) % 2 == 0 && field_92047_az != null) {
            EntityFireworkSparkFX spark = new EntityFireworkSparkFX(mcc$world(), mcc$x(), mcc$y(), mcc$z(), 0.0D, 0.0D, 0.0D, field_92047_az);
            spark.setRBGColorF(particleRed, particleGreen, particleBlue);
            spark.particleAge = spark.particleMaxAge / 2;
            if (hasFadeColour) {
                spark.hasFadeColour = true;
                spark.fadeColourRed = fadeColourRed;
                spark.fadeColourGreen = fadeColourGreen;
                spark.fadeColourBlue = fadeColourBlue;
            }
            spark.field_92048_ay = field_92048_ay;
            field_92047_az.addEffect(spark);
        }
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        return 15728880;
    }

    @Override
    public float getBrightness(float partialTicks) {
        return 1.0F;
    }
}
