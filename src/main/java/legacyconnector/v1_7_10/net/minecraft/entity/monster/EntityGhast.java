package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityFlying;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityLargeFireball;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 ghast: drifts towards random waypoints, and shoots a fireball at a visible player within 64 blocks every 60 ticks (the
 * mouth opens, watcher slot 16, while charging). A plain ghast is the modern one; mods' ghasts (Twilight Forest) fly here.
 */
public class EntityGhast extends EntityFlying implements IMob {
    public int courseChangeCooldown;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    public Entity targetedEntity;
    public int aggroCooldown;
    public int prevAttackCounter;
    public int attackCounter;
    public int explosionStrength = 1;

    /** View of a modern ghast. */
    public EntityGhast(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityGhast(World world) {
        super(world);
        setSize(4.0F, 4.0F);
        this.isImmuneToFire = true;
        this.experienceValue = 5;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(10.0D);
    }

    /** Whether the mouth is open (charging a fireball). */
    public boolean func_110182_bF() {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Ghast g && !mcc$hosted()) return g.isCharging();
        return dataWatcher.getWatchableObjectByte(16) != 0;
    }

    /** A fireball sent back by a player kills the ghast. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable()) return false;
        if (mcc$hosted() && source != null && "fireball".equals(source.getDamageType()) && source.getEntity() instanceof EntityPlayer) {
            super.attackEntityFrom(source, 1000.0F);
            return true;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    protected void updateEntityActionState() {
        World world = mcc$world();
        if (!world.isRemote && World.mcc$get$difficultySetting(world) == legacyconnector.v1_7_10.net.minecraft.world.EnumDifficulty.PEACEFUL) setDead();
        despawnEntity();
        prevAttackCounter = attackCounter;
        double x = mcc$get$posX(this), y = mcc$get$posY(this), z = mcc$get$posZ(this);
        double dx = waypointX - x, dy = waypointY - y, dz = waypointZ - z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 1.0D || distSq > 3600.0D) {
            waypointX = x + (rand.nextFloat() * 2.0F - 1.0F) * 16.0F;
            waypointY = y + (rand.nextFloat() * 2.0F - 1.0F) * 16.0F;
            waypointZ = z + (rand.nextFloat() * 2.0F - 1.0F) * 16.0F;
        }
        if (courseChangeCooldown-- <= 0) {
            courseChangeCooldown += rand.nextInt(5) + 2;
            double dist = MathHelper.sqrt_double(distSq);
            if (isCourseTraversable(waypointX, waypointY, waypointZ, dist)) {
                mcc$set$motionX(this, mcc$get$motionX(this) + dx / dist * 0.1D);
                mcc$set$motionY(this, mcc$get$motionY(this) + dy / dist * 0.1D);
                mcc$set$motionZ(this, mcc$get$motionZ(this) + dz / dist * 0.1D);
            } else {
                waypointX = x;
                waypointY = y;
                waypointZ = z;
            }
        }
        if (targetedEntity != null && !targetedEntity.isEntityAlive()) targetedEntity = null;
        if (targetedEntity == null || aggroCooldown-- <= 0) {
            targetedEntity = world.getClosestVulnerablePlayerToEntity(this, 100.0D);
            if (targetedEntity != null) aggroCooldown = 20;
        }
        if (targetedEntity != null && targetedEntity.getDistanceSqToEntity(this) < 64.0D * 64.0D) {
            double tx = mcc$get$posX(targetedEntity) - x;
            double ty = mcc$get$boundingBox(targetedEntity).minY + mcc$get$height(targetedEntity) / 2.0F - (y + mcc$get$height(this) / 2.0F);
            double tz = mcc$get$posZ(targetedEntity) - z;
            float yaw = -((float) Math.atan2(tx, tz)) * 180.0F / (float) Math.PI;
            mcc$set$rotationYaw(this, yaw);
            mcc$set$renderYawOffset(this, yaw);
            if (canEntityBeSeen(targetedEntity)) {
                int bx = (int) x, by = (int) y, bz = (int) z;
                if (attackCounter == 10) world.playAuxSFXAtEntity(null, 1007, bx, by, bz, 0);
                ++attackCounter;
                if (attackCounter == 20) {
                    world.playAuxSFXAtEntity(null, 1008, bx, by, bz, 0);
                    EntityLargeFireball fireball = new EntityLargeFireball(world, this, tx, ty, tz);
                    fireball.field_92057_e = explosionStrength;
                    Vec3 look = getLook(1.0F);
                    fireball.setPosition(x + look.xCoord * 4.0D, y + mcc$get$height(this) / 2.0F + 0.5D, z + look.zCoord * 4.0D);
                    world.spawnEntityInWorld(fireball);
                    attackCounter = -40;
                }
            } else if (attackCounter > 0) {
                --attackCounter;
            }
        } else {
            float yaw = -((float) Math.atan2(mcc$get$motionX(this), mcc$get$motionZ(this))) * 180.0F / (float) Math.PI;
            mcc$set$rotationYaw(this, yaw);
            mcc$set$renderYawOffset(this, yaw);
            if (attackCounter > 0) --attackCounter;
        }
        if (!world.isRemote) {
            byte open = (byte) (attackCounter > 10 ? 1 : 0);
            if (dataWatcher.getWatchableObjectByte(16) != open) dataWatcher.updateObject(16, open);
        }
    }

    /** Whether the straight path to the waypoint is free of blocks. */
    protected boolean isCourseTraversable(double x, double y, double z, double distance) {
        double sx = (waypointX - mcc$get$posX(this)) / distance;
        double sy = (waypointY - mcc$get$posY(this)) / distance;
        double sz = (waypointZ - mcc$get$posZ(this)) / distance;
        AxisAlignedBB box = mcc$get$boundingBox(this).copy();
        for (int i = 1; i < distance; i++) {
            box.offset(sx, sy, sz);
            if (!mcc$world().getCollidingBoundingBoxes(this, box).isEmpty()) return false;
        }
        return true;
    }

    @Override
    protected String getLivingSound() {
        return "mob.ghast.moan";
    }

    @Override
    protected String getHurtSound() {
        return "mob.ghast.scream";
    }

    @Override
    protected String getDeathSound() {
        return "mob.ghast.death";
    }

    @Override
    protected float getSoundVolume() {
        return 10.0F;
    }

    @Override
    public boolean getCanSpawnHere() {
        return rand.nextInt(20) == 0 && super.getCanSpawnHere()
                && World.mcc$get$difficultySetting(mcc$world()) != legacyconnector.v1_7_10.net.minecraft.world.EnumDifficulty.PEACEFUL;
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("ExplosionPower", explosionStrength);
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("ExplosionPower")) explosionStrength = tag.getInteger("ExplosionPower");
    }
}
