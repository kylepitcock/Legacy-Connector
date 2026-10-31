package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.IProjectile;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 EntityThrowable: a small projectile that flies on a gravity arc, ray-traces blocks and entities each tick and calls
 * {@link #onImpact} on the first thing it hits (ender pearls, snowballs, potions and the thrown items of many mods).
 */
public abstract class EntityThrowable extends Entity implements IProjectile {
    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;
    private Block inTile;
    protected boolean inGround;
    public int throwableShake;
    private EntityLivingBase thrower;
    private String throwerName;
    private int ticksInGround;
    private int ticksInAir;

    public EntityThrowable(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    public EntityThrowable(World world, EntityLivingBase thrower) {
        super(world);
        this.thrower = thrower;
        setSize(0.25F, 0.25F);
        setLocationAndAngles(mcc$get$posX(thrower), mcc$get$posY(thrower) + thrower.getEyeHeight(), mcc$get$posZ(thrower), mcc$get$rotationYaw(thrower), mcc$get$rotationPitch(thrower));
        mcc$set$posX(this, mcc$get$posX(this) - (MathHelper.cos(mcc$get$rotationYaw(this) / 180.0F * (float) Math.PI) * 0.16F));
        mcc$set$posY(this, mcc$get$posY(this) - (0.10000000149011612D));
        mcc$set$posZ(this, mcc$get$posZ(this) - (MathHelper.sin(mcc$get$rotationYaw(this) / 180.0F * (float) Math.PI) * 0.16F));
        setPosition(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
        yOffset = 0.0F;
        float f = 0.4F;
        mcc$set$motionX(this, -MathHelper.sin(mcc$get$rotationYaw(this) / 180.0F * (float) Math.PI) * MathHelper.cos(mcc$get$rotationPitch(this) / 180.0F * (float) Math.PI) * f);
        mcc$set$motionZ(this, MathHelper.cos(mcc$get$rotationYaw(this) / 180.0F * (float) Math.PI) * MathHelper.cos(mcc$get$rotationPitch(this) / 180.0F * (float) Math.PI) * f);
        mcc$set$motionY(this, -MathHelper.sin((mcc$get$rotationPitch(this) + func_70183_g()) / 180.0F * (float) Math.PI) * f);
        setThrowableHeading(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this), func_70182_d(), 1.0F);
        // a view of a vanilla projectile (snowball, pearl, ...) credits its thrower (kills, ender pearl teleports)
        if (!mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.Projectile p && thrower != null && thrower.mcc$entity != null) {
            p.setOwner(thrower.mcc$entity);
        }
    }

    public EntityThrowable(World world, double x, double y, double z) {
        super(world);
        ticksInGround = 0;
        setSize(0.25F, 0.25F);
        setPosition(x, y, z);
        yOffset = 0.0F;
    }

    @Override
    protected void entityInit() {
    }

    public boolean isInRangeToRenderDist(double distance) {
        double d1 = mcc$get$boundingBox(this).getAverageEdgeLength() * 4.0D;
        d1 *= 64.0D;
        return distance < d1 * d1;
    }

    protected float func_70182_d() {
        return 1.5F;
    }

    @Override
    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
        float f2 = MathHelper.sqrt_double(x * x + y * y + z * z);
        x /= f2;
        y /= f2;
        z /= f2;
        x += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        y += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        z += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        float f3 = MathHelper.sqrt_double(x * x + z * z);
        var chain1 = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
        mcc$set$rotationYaw(this, chain1);
        mcc$set$prevRotationYaw(this, chain1);
        var chain2 = (float) (Math.atan2(y, f3) * 180.0D / Math.PI);
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
            float f = MathHelper.sqrt_double(x * x + z * z);
            var chain3 = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
            mcc$set$rotationYaw(this, chain3);
            mcc$set$prevRotationYaw(this, chain3);
            var chain4 = (float) (Math.atan2(y, f) * 180.0D / Math.PI);
            mcc$set$rotationPitch(this, chain4);
            mcc$set$prevRotationPitch(this, chain4);
        }
    }

    @Override
    public void onUpdate() {
        mcc$set$lastTickPosX(this, mcc$get$posX(this));
        mcc$set$lastTickPosY(this, mcc$get$posY(this));
        mcc$set$lastTickPosZ(this, mcc$get$posZ(this));
        super.onUpdate();
        if (throwableShake > 0) --throwableShake;
        if (inGround) {
            if (mcc$get$worldObj(this).getBlock(xTile, yTile, zTile) == inTile) {
                ++ticksInGround;
                if (ticksInGround == 1200) setDead();
                return;
            }
            inGround = false;
            mcc$set$motionX(this, mcc$get$motionX(this) * (rand.nextFloat() * 0.2F));
            mcc$set$motionY(this, mcc$get$motionY(this) * (rand.nextFloat() * 0.2F));
            mcc$set$motionZ(this, mcc$get$motionZ(this) * (rand.nextFloat() * 0.2F));
            ticksInGround = 0;
            ticksInAir = 0;
        } else {
            ++ticksInAir;
        }
        Vec3 from = Vec3.createVectorHelper(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
        Vec3 to = Vec3.createVectorHelper(mcc$get$posX(this) + mcc$get$motionX(this), mcc$get$posY(this) + mcc$get$motionY(this), mcc$get$posZ(this) + mcc$get$motionZ(this));
        MovingObjectPosition hit = mcc$get$worldObj(this).rayTraceBlocks(from, to);
        from = Vec3.createVectorHelper(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
        to = Vec3.createVectorHelper(mcc$get$posX(this) + mcc$get$motionX(this), mcc$get$posY(this) + mcc$get$motionY(this), mcc$get$posZ(this) + mcc$get$motionZ(this));
        if (hit != null) to = Vec3.createVectorHelper(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
        if (!mcc$get$worldObj(this).isRemote) {
            Entity target = null;
            List list = mcc$get$worldObj(this).getEntitiesWithinAABBExcludingEntity(this, mcc$get$boundingBox(this).addCoord(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this)).expand(1.0D, 1.0D, 1.0D));
            double nearest = 0.0D;
            EntityLivingBase owner = getThrower();
            for (Object o : list) {
                Entity candidate = (Entity) o;
                if (candidate.canBeCollidedWith() && (candidate != owner || ticksInAir >= 5)) {
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
        }
        if (hit != null) {
            if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    && mcc$get$worldObj(this).getBlock(hit.blockX, hit.blockY, hit.blockZ) == legacyconnector.v1_7_10.net.minecraft.init.Blocks.portal) {
                setInPortal();
            } else {
                onImpact(hit);
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
        float gravity = getGravityVelocity();
        if (isInWater()) drag = 0.8F;
        mcc$set$motionX(this, mcc$get$motionX(this) * (drag));
        mcc$set$motionY(this, mcc$get$motionY(this) * (drag));
        mcc$set$motionZ(this, mcc$get$motionZ(this) * (drag));
        mcc$set$motionY(this, mcc$get$motionY(this) - (gravity));
        setPosition(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
    }

    protected float getGravityVelocity() {
        return 0.03F;
    }

    protected abstract void onImpact(MovingObjectPosition hit);

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        tag.setShort("xTile", (short) xTile);
        tag.setShort("yTile", (short) yTile);
        tag.setShort("zTile", (short) zTile);
        tag.setByte("inTile", (byte) Block.getIdFromBlock(inTile));
        tag.setByte("shake", (byte) throwableShake);
        tag.setByte("inGround", (byte) (inGround ? 1 : 0));
        if ((throwerName == null || throwerName.length() == 0) && thrower instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer) {
            throwerName = thrower.getCommandSenderName();
        }
        tag.setString("ownerName", throwerName == null ? "" : throwerName);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        xTile = tag.getShort("xTile");
        yTile = tag.getShort("yTile");
        zTile = tag.getShort("zTile");
        inTile = Block.getBlockById(tag.getByte("inTile") & 255);
        throwableShake = tag.getByte("shake") & 255;
        inGround = tag.getByte("inGround") == 1;
        throwerName = tag.getString("ownerName");
        if (throwerName != null && throwerName.length() == 0) throwerName = null;
    }

    @Override
    public float getShadowSize() {
        return 0.0F;
    }

    public EntityLivingBase getThrower() {
        if (thrower == null && throwerName != null && throwerName.length() > 0) {
            thrower = mcc$get$worldObj(this).getPlayerEntityByName(throwerName);
        }
        return thrower;
    }

    /**
     * The pitch offset of the throw (1.7.10 has no MCP name for it): 0, potions and Witchery's brews -20 so they arc upwards.
     * Mods override it under this name, so the constructor must call it by this name.
     */
    protected float func_70183_g() {
        return 0.0F;
    }
}
