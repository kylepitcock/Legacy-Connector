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
        setLocationAndAngles(thrower.posX, thrower.posY + thrower.getEyeHeight(), thrower.posZ, thrower.rotationYaw, thrower.rotationPitch);
        posX -= MathHelper.cos(rotationYaw / 180.0F * (float) Math.PI) * 0.16F;
        posY -= 0.10000000149011612D;
        posZ -= MathHelper.sin(rotationYaw / 180.0F * (float) Math.PI) * 0.16F;
        setPosition(posX, posY, posZ);
        yOffset = 0.0F;
        float f = 0.4F;
        motionX = -MathHelper.sin(rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(rotationPitch / 180.0F * (float) Math.PI) * f;
        motionZ = MathHelper.cos(rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(rotationPitch / 180.0F * (float) Math.PI) * f;
        motionY = -MathHelper.sin((rotationPitch + getInaccuracy()) / 180.0F * (float) Math.PI) * f;
        setThrowableHeading(motionX, motionY, motionZ, func_70182_d(), 1.0F);
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
        double d1 = boundingBox.getAverageEdgeLength() * 4.0D;
        d1 *= 64.0D;
        return distance < d1 * d1;
    }

    protected float func_70182_d() {
        return 1.5F;
    }

    protected float getInaccuracy() {
        return 0.0F;
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
        motionX = x;
        motionY = y;
        motionZ = z;
        float f3 = MathHelper.sqrt_double(x * x + z * z);
        prevRotationYaw = rotationYaw = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
        prevRotationPitch = rotationPitch = (float) (Math.atan2(y, f3) * 180.0D / Math.PI);
        ticksInGround = 0;
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        motionX = x;
        motionY = y;
        motionZ = z;
        if (prevRotationPitch == 0.0F && prevRotationYaw == 0.0F) {
            float f = MathHelper.sqrt_double(x * x + z * z);
            prevRotationYaw = rotationYaw = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
            prevRotationPitch = rotationPitch = (float) (Math.atan2(y, f) * 180.0D / Math.PI);
        }
    }

    @Override
    public void onUpdate() {
        lastTickPosX = posX;
        lastTickPosY = posY;
        lastTickPosZ = posZ;
        super.onUpdate();
        if (throwableShake > 0) --throwableShake;
        if (inGround) {
            if (worldObj.getBlock(xTile, yTile, zTile) == inTile) {
                ++ticksInGround;
                if (ticksInGround == 1200) setDead();
                return;
            }
            inGround = false;
            motionX *= rand.nextFloat() * 0.2F;
            motionY *= rand.nextFloat() * 0.2F;
            motionZ *= rand.nextFloat() * 0.2F;
            ticksInGround = 0;
            ticksInAir = 0;
        } else {
            ++ticksInAir;
        }
        Vec3 from = Vec3.createVectorHelper(posX, posY, posZ);
        Vec3 to = Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
        MovingObjectPosition hit = worldObj.rayTraceBlocks(from, to);
        from = Vec3.createVectorHelper(posX, posY, posZ);
        to = Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
        if (hit != null) to = Vec3.createVectorHelper(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
        if (!worldObj.isRemote) {
            Entity target = null;
            List list = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.addCoord(motionX, motionY, motionZ).expand(1.0D, 1.0D, 1.0D));
            double nearest = 0.0D;
            EntityLivingBase owner = getThrower();
            for (Object o : list) {
                Entity candidate = (Entity) o;
                if (candidate.canBeCollidedWith() && (candidate != owner || ticksInAir >= 5)) {
                    float border = 0.3F;
                    AxisAlignedBB box = candidate.boundingBox.expand(border, border, border);
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
                    && worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ) == legacyconnector.v1_7_10.net.minecraft.init.Blocks.portal) {
                setInPortal();
            } else {
                onImpact(hit);
            }
        }
        posX += motionX;
        posY += motionY;
        posZ += motionZ;
        float horizontal = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
        rotationYaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        for (rotationPitch = (float) (Math.atan2(motionY, horizontal) * 180.0D / Math.PI); rotationPitch - prevRotationPitch < -180.0F; prevRotationPitch -= 360.0F) {
        }
        while (rotationPitch - prevRotationPitch >= 180.0F) prevRotationPitch += 360.0F;
        while (rotationYaw - prevRotationYaw < -180.0F) prevRotationYaw -= 360.0F;
        while (rotationYaw - prevRotationYaw >= 180.0F) prevRotationYaw += 360.0F;
        rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * 0.2F;
        rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * 0.2F;
        float drag = 0.99F;
        float gravity = getGravityVelocity();
        if (isInWater()) drag = 0.8F;
        motionX *= drag;
        motionY *= drag;
        motionZ *= drag;
        motionY -= gravity;
        setPosition(posX, posY, posZ);
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
            thrower = worldObj.getPlayerEntityByName(throwerName);
        }
        return thrower;
    }
}
