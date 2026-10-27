package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 primed TNT: a view of a modern primed TNT (fuse, igniter); a mod's subclass counts its own fuse down and explodes. */
public class EntityTNTPrimed extends Entity {
    private static final DataParameter<Integer> FUSE = EntityDataManager.createKey(EntityTNTPrimed.class, DataSerializers.VARINT);
    private EntityLivingBase tntPlacedBy;
    private int fuse = 80;

    public EntityTNTPrimed(World world) {
        super(world);
        preventEntitySpawning = true;
        setSize(0.98F, 0.98F);
    }

    public EntityTNTPrimed(World world, double x, double y, double z, EntityLivingBase igniter) {
        this(world);
        setPosition(x, y, z);
        float f = (float) (Math.random() * (Math.PI * 2D));
        mcc$set$motionX(this, -((float) Math.sin(f)) * 0.02F);
        mcc$set$motionY(this, 0.20000000298023224D);
        mcc$set$motionZ(this, -((float) Math.cos(f)) * 0.02F);
        setFuse(80);
        mcc$set$prevPosX(this, x);
        mcc$set$prevPosY(this, y);
        mcc$set$prevPosZ(this, z);
        tntPlacedBy = igniter;
    }

    protected EntityTNTPrimed(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.item.PrimedTnt mcc$tnt() {
        return mcc$entity instanceof net.minecraft.world.entity.item.PrimedTnt t ? t : null;
    }

    @Override
    protected void entityInit() {
        dataManager.register(FUSE, 80);
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !mcc$get$isDead(this);
    }

    @Override
    public void onUpdate() {
        if (!mcc$hosted() || mcc$entity == null) {
            super.onUpdate();
            return;
        }
        mcc$set$prevPosX(this, mcc$x());
        mcc$set$prevPosY(this, mcc$y());
        mcc$set$prevPosZ(this, mcc$z());
        if (!hasNoGravity()) mcc$set$motionY(this, mcc$get$motionY(this) - 0.03999999910593033D);
        move(legacyconnector.v1_12_2.net.minecraft.entity.MoverType.SELF, mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$set$motionX(this, mcc$get$motionX(this) * 0.9800000190734863D);
        mcc$set$motionY(this, mcc$get$motionY(this) * 0.9800000190734863D);
        mcc$set$motionZ(this, mcc$get$motionZ(this) * 0.9800000190734863D);
        if (mcc$get$onGround(this)) {
            mcc$set$motionX(this, mcc$get$motionX(this) * 0.699999988079071D);
            mcc$set$motionZ(this, mcc$get$motionZ(this) * 0.699999988079071D);
            mcc$set$motionY(this, mcc$get$motionY(this) * -0.5D);
        }
        --fuse;
        if (fuse <= 0) {
            setDead();
            if (!mcc$entity.level().isClientSide()) explode();
        } else {
            handleWaterMovement();
            if (mcc$entity.level().isClientSide()) {
                mcc$entity.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, mcc$x(), mcc$y() + 0.5D, mcc$z(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void explode() {
        var m = mcc$entity;
        m.level().explode(m, m.getX(), m.getY() + mcc$get$height(this) / 16.0F, m.getZ(), 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setShort("Fuse", (short) getFuse());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setFuse(tag.getShort("Fuse"));
    }

    public EntityLivingBase getTntPlacedBy() {
        var t = mcc$tnt();
        if (t != null && !mcc$hosted()) {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(t.getOwner()) instanceof EntityLivingBase l ? l : tntPlacedBy;
        }
        return tntPlacedBy;
    }

    @Override
    public float getEyeHeight() {
        return 0.0F;
    }

    public void setFuse(int fuse) {
        var t = mcc$tnt();
        if (t != null && !mcc$hosted()) t.setFuse(fuse);
        this.fuse = fuse;
        dataManager.set(FUSE, fuse);
    }

    @Override
    public void notifyDataManagerChange(legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<?> key) {
        if (FUSE.equals(key)) {
            Integer f = dataManager.get(FUSE);
            if (f != null) fuse = f;
        }
    }

    public int getFuseDataManager() {
        Integer f = dataManager.get(FUSE);
        return f == null ? fuse : f;
    }

    public int getFuse() {
        var t = mcc$tnt();
        return t != null && !mcc$hosted() ? t.getFuse() : fuse;
    }
}
