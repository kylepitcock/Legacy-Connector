package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.IProjectile;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 EntityArrow: flies on an arc, sticks into blocks, damages what it hits and can be picked up again. */
public class EntityArrow extends Entity implements IProjectile {
    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;
    private Block inTile;
    private int inData;
    private boolean inGround;
    /** 0: cannot be picked up, 1: can be picked up, 2: only in creative mode. */
    public int canBePickedUp;
    public int arrowShake;
    public Entity shootingEntity;
    private int ticksInGround;
    private int ticksInAir;
    private double damage = 2.0D;
    private int knockbackStrength;

    public EntityArrow(World world) {
        super(world);
        renderDistanceWeight = 10.0D;
        setSize(0.5F, 0.5F);
    }

    public EntityArrow(World world, double x, double y, double z) {
        super(world);
        renderDistanceWeight = 10.0D;
        setSize(0.5F, 0.5F);
        setPosition(x, y, z);
        yOffset = 0.0F;
    }

    public EntityArrow(World world, EntityLivingBase shooter, EntityLivingBase target, float velocity, float inaccuracy) {
        super(world);
        renderDistanceWeight = 10.0D;
        shootingEntity = shooter;
        if (shooter instanceof EntityPlayer) canBePickedUp = 1;
        mcc$set$posY(this, mcc$get$posY(shooter) + shooter.getEyeHeight() - 0.10000000149011612D);
        double dx = mcc$get$posX(target) - mcc$get$posX(shooter);
        double dy = mcc$get$boundingBox(target).minY + mcc$get$height(target) / 3.0F - mcc$get$posY(this);
        double dz = mcc$get$posZ(target) - mcc$get$posZ(shooter);
        double horizontal = MathHelper.sqrt_double(dx * dx + dz * dz);
        if (horizontal >= 1.0E-7D) {
            float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
            float pitch = (float) (-(Math.atan2(dy, horizontal) * 180.0D / Math.PI));
            double ox = dx / horizontal;
            double oz = dz / horizontal;
            setLocationAndAngles(mcc$get$posX(shooter) + ox, mcc$get$posY(this), mcc$get$posZ(shooter) + oz, yaw, pitch);
            yOffset = 0.0F;
            float arc = (float) horizontal * 0.2F;
            setThrowableHeading(dx, dy + arc, dz, velocity, inaccuracy);
        }
    }

    public EntityArrow(World world, EntityLivingBase shooter, float velocity) {
        super(world);
        renderDistanceWeight = 10.0D;
        shootingEntity = shooter;
        if (shooter instanceof EntityPlayer) canBePickedUp = 1;
        setSize(0.5F, 0.5F);
        setLocationAndAngles(mcc$get$posX(shooter), mcc$get$posY(shooter) + shooter.getEyeHeight(), mcc$get$posZ(shooter), mcc$get$rotationYaw(shooter), mcc$get$rotationPitch(shooter));
        mcc$set$posX(this, mcc$get$posX(this) - (MathHelper.cos(mcc$get$rotationYaw(this) / 180.0F * (float) Math.PI) * 0.16F));
        mcc$set$posY(this, mcc$get$posY(this) - (0.10000000149011612D));
        mcc$set$posZ(this, mcc$get$posZ(this) - (MathHelper.sin(mcc$get$rotationYaw(this) / 180.0F * (float) Math.PI) * 0.16F));
        setPosition(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
        yOffset = 0.0F;
        mcc$set$motionX(this, -MathHelper.sin(mcc$get$rotationYaw(this) / 180.0F * (float) Math.PI) * MathHelper.cos(mcc$get$rotationPitch(this) / 180.0F * (float) Math.PI));
        mcc$set$motionZ(this, MathHelper.cos(mcc$get$rotationYaw(this) / 180.0F * (float) Math.PI) * MathHelper.cos(mcc$get$rotationPitch(this) / 180.0F * (float) Math.PI));
        mcc$set$motionY(this, -MathHelper.sin(mcc$get$rotationPitch(this) / 180.0F * (float) Math.PI));
        setThrowableHeading(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this), velocity * 1.5F, 1.0F);
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(16, (byte) 0);
    }

    @Override
    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
        float length = MathHelper.sqrt_double(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;
        x += rand.nextGaussian() * (rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        y += rand.nextGaussian() * (rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        z += rand.nextGaussian() * (rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        float horizontal = MathHelper.sqrt_double(x * x + z * z);
        var chain1 = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
        mcc$set$rotationYaw(this, chain1);
        mcc$set$prevRotationYaw(this, chain1);
        var chain2 = (float) (Math.atan2(y, horizontal) * 180.0D / Math.PI);
        mcc$set$rotationPitch(this, chain2);
        mcc$set$prevRotationPitch(this, chain2);
        ticksInGround = 0;
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        if (mcc$get$prevRotationPitch(this) == 0.0F && mcc$get$prevRotationYaw(this) == 0.0F) {
            float horizontal = MathHelper.sqrt_double(x * x + z * z);
            var chain3 = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
            mcc$set$rotationYaw(this, chain3);
            mcc$set$prevRotationYaw(this, chain3);
            var chain4 = (float) (Math.atan2(y, horizontal) * 180.0D / Math.PI);
            mcc$set$rotationPitch(this, chain4);
            mcc$set$prevRotationPitch(this, chain4);
            mcc$set$prevRotationPitch(this, mcc$get$rotationPitch(this));
            mcc$set$prevRotationYaw(this, mcc$get$rotationYaw(this));
            setLocationAndAngles(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), mcc$get$rotationYaw(this), mcc$get$rotationPitch(this));
            ticksInGround = 0;
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (mcc$get$prevRotationPitch(this) == 0.0F && mcc$get$prevRotationYaw(this) == 0.0F) {
            float horizontal = MathHelper.sqrt_double(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
            var chain5 = (float) (Math.atan2(mcc$get$motionX(this), mcc$get$motionZ(this)) * 180.0D / Math.PI);
            mcc$set$rotationYaw(this, chain5);
            mcc$set$prevRotationYaw(this, chain5);
            var chain6 = (float) (Math.atan2(mcc$get$motionY(this), horizontal) * 180.0D / Math.PI);
            mcc$set$rotationPitch(this, chain6);
            mcc$set$prevRotationPitch(this, chain6);
        }
        Block block = mcc$get$worldObj(this).getBlock(xTile, yTile, zTile);
        if (block.getMaterial() != Material.air) {
            block.setBlockBoundsBasedOnState(mcc$get$worldObj(this), xTile, yTile, zTile);
            AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(mcc$get$worldObj(this), xTile, yTile, zTile);
            if (box != null && box.isVecInside(Vec3.createVectorHelper(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this)))) inGround = true;
        }
        if (arrowShake > 0) --arrowShake;
        if (inGround) {
            int meta = mcc$get$worldObj(this).getBlockMetadata(xTile, yTile, zTile);
            if (block == inTile && meta == inData) {
                ++ticksInGround;
                if (ticksInGround == 1200) setDead();
            } else {
                inGround = false;
                mcc$set$motionX(this, mcc$get$motionX(this) * (rand.nextFloat() * 0.2F));
                mcc$set$motionY(this, mcc$get$motionY(this) * (rand.nextFloat() * 0.2F));
                mcc$set$motionZ(this, mcc$get$motionZ(this) * (rand.nextFloat() * 0.2F));
                ticksInGround = 0;
                ticksInAir = 0;
            }
        } else {
            ++ticksInAir;
            Vec3 from = Vec3.createVectorHelper(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
            Vec3 to = Vec3.createVectorHelper(mcc$get$posX(this) + mcc$get$motionX(this), mcc$get$posY(this) + mcc$get$motionY(this), mcc$get$posZ(this) + mcc$get$motionZ(this));
            MovingObjectPosition hit = mcc$get$worldObj(this).rayTraceBlocks(from, to, false, true, false);
            from = Vec3.createVectorHelper(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
            to = Vec3.createVectorHelper(mcc$get$posX(this) + mcc$get$motionX(this), mcc$get$posY(this) + mcc$get$motionY(this), mcc$get$posZ(this) + mcc$get$motionZ(this));
            if (hit != null) to = Vec3.createVectorHelper(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
            Entity target = null;
            List list = mcc$get$worldObj(this).getEntitiesWithinAABBExcludingEntity(this, mcc$get$boundingBox(this).addCoord(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this)).expand(1.0D, 1.0D, 1.0D));
            double nearest = 0.0D;
            for (Object o : list) {
                Entity candidate = (Entity) o;
                if (candidate.canBeCollidedWith() && (candidate != shootingEntity || ticksInAir >= 5)) {
                    float border = 0.3F;
                    AxisAlignedBB box = mcc$get$boundingBox(candidate).expand(border, border, border);
                    MovingObjectPosition intercept = box.calculateIntercept(from, to);
                    if (intercept != null) {
                        double distance = from.distanceTo(intercept.hitVec);
                        if (distance < nearest || nearest == 0.0D) {
                            target = candidate;
                            nearest = distance;
                        }
                    }
                }
            }
            if (target != null) hit = new MovingObjectPosition(target);
            if (hit != null && hit.entityHit instanceof EntityPlayer player) {
                if (legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$disableDamage(player.capabilities) || shootingEntity instanceof EntityPlayer && !((EntityPlayer) shootingEntity).canAttackPlayer(player)) hit = null;
            }
            if (hit != null) {
                if (hit.entityHit != null) {
                    float speed = MathHelper.sqrt_double(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionY(this) * mcc$get$motionY(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
                    int amount = MathHelper.ceiling_double_int(speed * damage);
                    if (getIsCritical()) amount += rand.nextInt(amount / 2 + 2);
                    DamageSource source = shootingEntity == null ? DamageSource.causeArrowDamage(this, this) : DamageSource.causeArrowDamage(this, shootingEntity);
                    if (isBurning() && !isEnderman(hit.entityHit)) hit.entityHit.setFire(5);
                    if (hit.entityHit.attackEntityFrom(source, amount)) {
                        if (hit.entityHit instanceof EntityLivingBase living) {
                            if (!mcc$get$worldObj(this).isRemote) living.setArrowCountInEntity(living.getArrowCountInEntity() + 1);
                            if (knockbackStrength > 0) {
                                float horizontalSpeed = MathHelper.sqrt_double(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
                                if (horizontalSpeed > 0.0F) {
                                    hit.entityHit.addVelocity(mcc$get$motionX(this) * knockbackStrength * 0.6000000238418579D / horizontalSpeed, 0.1D,
                                            mcc$get$motionZ(this) * knockbackStrength * 0.6000000238418579D / horizontalSpeed);
                                }
                            }
                        }
                        playSound("random.bowhit", 1.0F, 1.2F / (rand.nextFloat() * 0.2F + 0.9F));
                        if (!isEnderman(hit.entityHit)) setDead();
                    } else {
                        mcc$set$motionX(this, mcc$get$motionX(this) * (-0.10000000149011612D));
                        mcc$set$motionY(this, mcc$get$motionY(this) * (-0.10000000149011612D));
                        mcc$set$motionZ(this, mcc$get$motionZ(this) * (-0.10000000149011612D));
                        mcc$set$rotationYaw(this, mcc$get$rotationYaw(this) + (180.0F));
                        mcc$set$prevRotationYaw(this, mcc$get$prevRotationYaw(this) + (180.0F));
                        ticksInAir = 0;
                    }
                } else {
                    xTile = hit.blockX;
                    yTile = hit.blockY;
                    zTile = hit.blockZ;
                    inTile = mcc$get$worldObj(this).getBlock(xTile, yTile, zTile);
                    inData = mcc$get$worldObj(this).getBlockMetadata(xTile, yTile, zTile);
                    mcc$set$motionX(this, (float) (hit.hitVec.xCoord - mcc$get$posX(this)));
                    mcc$set$motionY(this, (float) (hit.hitVec.yCoord - mcc$get$posY(this)));
                    mcc$set$motionZ(this, (float) (hit.hitVec.zCoord - mcc$get$posZ(this)));
                    float speed = MathHelper.sqrt_double(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionY(this) * mcc$get$motionY(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
                    mcc$set$posX(this, mcc$get$posX(this) - (mcc$get$motionX(this) / speed * 0.05000000074505806D));
                    mcc$set$posY(this, mcc$get$posY(this) - (mcc$get$motionY(this) / speed * 0.05000000074505806D));
                    mcc$set$posZ(this, mcc$get$posZ(this) - (mcc$get$motionZ(this) / speed * 0.05000000074505806D));
                    playSound("random.bowhit", 1.0F, 1.2F / (rand.nextFloat() * 0.2F + 0.9F));
                    inGround = true;
                    arrowShake = 7;
                    setIsCritical(false);
                    if (inTile.getMaterial() != Material.air) inTile.onEntityCollidedWithBlock(mcc$get$worldObj(this), xTile, yTile, zTile, this);
                }
            }
            mcc$set$posX(this, mcc$get$posX(this) + (mcc$get$motionX(this)));
            mcc$set$posY(this, mcc$get$posY(this) + (mcc$get$motionY(this)));
            mcc$set$posZ(this, mcc$get$posZ(this) + (mcc$get$motionZ(this)));
            float horizontal = MathHelper.sqrt_double(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
            mcc$set$rotationYaw(this, (float) (Math.atan2(mcc$get$motionX(this), mcc$get$motionZ(this)) * 180.0D / Math.PI));
            mcc$set$rotationPitch(this, (float) (Math.atan2(mcc$get$motionY(this), horizontal) * 180.0D / Math.PI));
            while (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this) < -180.0F) {
                mcc$set$prevRotationPitch(this, mcc$get$prevRotationPitch(this) - (360.0F));
            }
            while (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this) >= 180.0F) {
                mcc$set$prevRotationPitch(this, mcc$get$prevRotationPitch(this) + (360.0F));
            }
            while (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this) < -180.0F) {
                mcc$set$prevRotationYaw(this, mcc$get$prevRotationYaw(this) - (360.0F));
            }
            while (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this) >= 180.0F) {
                mcc$set$prevRotationYaw(this, mcc$get$prevRotationYaw(this) + (360.0F));
            }
            mcc$set$rotationPitch(this, mcc$get$prevRotationPitch(this) + (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this)) * 0.2F);
            mcc$set$rotationYaw(this, mcc$get$prevRotationYaw(this) + (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this)) * 0.2F);
            float drag = 0.99F;
            if (isInWater()) drag = 0.8F;
            mcc$set$motionX(this, mcc$get$motionX(this) * (drag));
            mcc$set$motionY(this, mcc$get$motionY(this) * (drag));
            mcc$set$motionZ(this, mcc$get$motionZ(this) * (drag));
            mcc$set$motionY(this, mcc$get$motionY(this) - (0.05000000074505806D));
            setPosition(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
            doBlockCollisions();
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        tag.setShort("xTile", (short) xTile);
        tag.setShort("yTile", (short) yTile);
        tag.setShort("zTile", (short) zTile);
        tag.setShort("life", (short) ticksInGround);
        tag.setByte("inTile", (byte) Block.getIdFromBlock(inTile));
        tag.setByte("inData", (byte) inData);
        tag.setByte("shake", (byte) arrowShake);
        tag.setByte("inGround", (byte) (inGround ? 1 : 0));
        tag.setByte("pickup", (byte) canBePickedUp);
        tag.setDouble("damage", damage);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        xTile = tag.getShort("xTile");
        yTile = tag.getShort("yTile");
        zTile = tag.getShort("zTile");
        ticksInGround = tag.getShort("life");
        inTile = Block.getBlockById(tag.getByte("inTile") & 255);
        inData = tag.getByte("inData") & 255;
        arrowShake = tag.getByte("shake") & 255;
        inGround = tag.getByte("inGround") == 1;
        if (tag.hasKey("damage", 99)) damage = tag.getDouble("damage");
        if (tag.hasKey("pickup", 99)) canBePickedUp = tag.getByte("pickup");
        else if (tag.hasKey("player", 99)) canBePickedUp = tag.getBoolean("player") ? 1 : 0;
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        if (!mcc$get$worldObj(this).isRemote && inGround && arrowShake <= 0) {
            boolean pickUp = canBePickedUp == 1 || canBePickedUp == 2 && legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities);
            if (canBePickedUp == 1 && !player.inventory.addItemStackToInventory(new ItemStack(Items.arrow, 1))) pickUp = false;
            if (pickUp) {
                playSound("random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                player.onItemPickup(this, 1);
                setDead();
            }
        }
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public float getShadowSize() {
        return 0.0F;
    }

    /** Endermen shrug arrows off: they are matched by class name since the shim has no Enderman base to name. */
    private static boolean isEnderman(Entity entity) {
        for (Class<?> c = entity.getClass(); c != null; c = c.getSuperclass()) if (c.getSimpleName().equals("EntityEnderman")) return true;
        return false;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return damage;
    }

    public void setKnockbackStrength(int strength) {
        knockbackStrength = strength;
    }

    @Override
    public boolean canAttackWithItem() {
        return false;
    }

    public void setIsCritical(boolean critical) {
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (critical ? flags | 1 : flags & -2));
    }

    public boolean getIsCritical() {
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    /** The flame/punch/power enchantments of the bow that shot it are applied by the bow; kept for subclasses that read it. */
    protected static int enchantmentLevel(int id, ItemStack bow) {
        return EnchantmentHelper.getEnchantmentLevel(id, bow);
    }
}
