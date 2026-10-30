package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 llama spit (the modern spit). */
public class EntityLlamaSpit extends legacyconnector.v1_12_2.net.minecraft.entity.Entity implements legacyconnector.v1_12_2.net.minecraft.entity.IProjectile {
    public EntityLlamaSpit(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    /** Spit at a position with a motion (the client's spawn of a spit packet). */
    public EntityLlamaSpit(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        this(world);
        setPosition(x, y, z);
        for (int i = 0; i < 7; ++i) {
            double d = 0.4D + 0.1D * i;
            world.spawnParticle(legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes.SPIT, x, y, z, motionX * d, motionY, motionZ * d);
        }
        mcc$set$motionX(this, motionX);
        mcc$set$motionY(this, motionY);
        mcc$set$motionZ(this, motionZ);
    }

    /** Sends the spit along the direction at {@code velocity}, scattered by {@code inaccuracy} (as every 1.12 projectile). */
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        var m = mcc$self();
        if (m != null) {
            m.shoot(x, y, z, velocity, inaccuracy);
            return;
        }
        float f = (float) Math.sqrt(x * x + y * y + z * z);
        x = x / f + rand.nextGaussian() * 0.0075D * inaccuracy;
        y = y / f + rand.nextGaussian() * 0.0075D * inaccuracy;
        z = z / f + rand.nextGaussian() * 0.0075D * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        float h = (float) Math.sqrt(x * x + z * z);
        mcc$set$rotationYaw(this, (float) (Math.atan2(x, z) * (180D / Math.PI)));
        mcc$set$rotationPitch(this, (float) (Math.atan2(y, h) * (180D / Math.PI)));
        mcc$set$prevRotationYaw(this, mcc$get$rotationYaw(this));
        mcc$set$prevRotationPitch(this, mcc$get$rotationPitch(this));
    }

    protected EntityLlamaSpit(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.projectile.LlamaSpit mcc$self() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.LlamaSpit m ? m : null;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
