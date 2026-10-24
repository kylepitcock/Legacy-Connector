package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A straight-flying fireball (ghast, blaze, dragon fire and mods' bolts): accelerates along its heading until it hits a block. */
public abstract class EntityFireball extends Entity {
    public EntityLivingBase shootingEntity;
    public double accelerationX;
    public double accelerationY;
    public double accelerationZ;
    private int ticksAlive;

    public EntityFireball(World world) {
        super(world);
        setSize(1.0F, 1.0F);
    }

    public EntityFireball(World world, double x, double y, double z, double ax, double ay, double az) {
        this(world);
        setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        setPosition(x, y, z);
        double length = Math.sqrt(ax * ax + ay * ay + az * az);
        if (length > 0) {
            this.accelerationX = ax / length * 0.1D;
            this.accelerationY = ay / length * 0.1D;
            this.accelerationZ = az / length * 0.1D;
        }
    }

    public EntityFireball(World world, EntityLivingBase shooter, double ax, double ay, double az) {
        this(world);
        this.shootingEntity = shooter;
        setLocationAndAngles(mcc$get$posX(shooter), mcc$get$posY(shooter), mcc$get$posZ(shooter), 0.0F, 0.0F);
        setPosition(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
        setVelocity(0, 0, 0);
        ax += rand.nextGaussian() * 0.4D;
        ay += rand.nextGaussian() * 0.4D;
        az += rand.nextGaussian() * 0.4D;
        double length = Math.sqrt(ax * ax + ay * ay + az * az);
        if (length > 0) {
            this.accelerationX = ax / length * 0.1D;
            this.accelerationY = ay / length * 0.1D;
            this.accelerationZ = az / length * 0.1D;
        }
    }

    @Override
    protected void entityInit() {
    }

    protected abstract void onImpact(MovingObjectPosition position);

    protected float getMotionFactor() {
        return 0.95F;
    }

    protected boolean isFireballFiery() {
        return true;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (mcc$world().isRemote) return;
        if (++ticksAlive > 600) {
            setDead();
            return;
        }
        double x = mcc$get$posX(this) + mcc$get$motionX(this);
        double y = mcc$get$posY(this) + mcc$get$motionY(this);
        double z = mcc$get$posZ(this) + mcc$get$motionZ(this);
        var block = mcc$world().getBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        if (block != null && block.getMaterial().isSolid()) {
            onImpact(new MovingObjectPosition((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z), 0, Vec3.createVectorHelper(x, y, z)));
            return;
        }
        setPosition(x, y, z);
        float factor = getMotionFactor();
        setVelocity((mcc$get$motionX(this) + accelerationX) * factor, (mcc$get$motionY(this) + accelerationY) * factor,
                (mcc$get$motionZ(this) + accelerationZ) * factor);
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
