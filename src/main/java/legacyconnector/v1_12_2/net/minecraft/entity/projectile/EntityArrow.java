package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.IProjectile;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 EntityArrow: flies on an arc, sticks in blocks, hurts what it hits ({@link #arrowHit} for the extra effects) and can be picked
 * up. A mod's arrow class runs this flight itself on its host; views of modern arrows use the modern arrow's state.
 */
public abstract class EntityArrow extends Entity implements IProjectile {
    /** What arrows can hit: living, non-spectating, collidable entities. */
    public static final com.google.common.base.Predicate<legacyconnector.v1_12_2.net.minecraft.entity.Entity> ARROW_TARGETS = com.google.common.base.Predicates.and(
            legacyconnector.v1_12_2.net.minecraft.util.EntitySelectors.NOT_SPECTATING, legacyconnector.v1_12_2.net.minecraft.util.EntitySelectors.IS_ALIVE,
            e -> e != null && e.canBeCollidedWith());

    /** Data fixers of saved arrows: the layer reads saves with the modern game's fixers, so there is nothing to register. */
    public static void registerFixesArrow(legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer fixer, String name) {
    }

    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;
    private legacyconnector.v1_12_2.net.minecraft.block.Block inTile;
    private int inData;
    protected boolean inGround;
    protected int timeInGround;
    public PickupStatus pickupStatus = PickupStatus.DISALLOWED;
    public int arrowShake;
    public Entity shootingEntity;
    private int ticksInGround;
    private int ticksInAir;
    private double damage = 2.0D;
    private int knockbackStrength;
    private boolean mcc$critical;

    public EntityArrow(World world) {
        super(world);
        setSize(0.5F, 0.5F);
    }

    public EntityArrow(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    public EntityArrow(World world, EntityLivingBase shooter) {
        this(world, shooter.mcc$x(), shooter.mcc$y() + shooter.getEyeHeight() - 0.10000000149011612D, shooter.mcc$z());
        this.shootingEntity = shooter;
        if (shooter instanceof EntityPlayer) pickupStatus = PickupStatus.ALLOWED;
        if (!mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.AbstractArrow a && shooter.mcc$entity != null) a.setOwner(shooter.mcc$entity);
    }

    protected EntityArrow(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.projectile.AbstractArrow mcc$arrow() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.AbstractArrow a ? a : null;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        double d = getEntityBoundingBox().getAverageEdgeLength() * 10.0D;
        if (Double.isNaN(d)) d = 1.0D;
        d *= 64.0D;
        return distance < d * d;
    }

    public void shoot(Entity shooter, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy) {
        float x = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float y = -MathHelper.sin(pitch * 0.017453292F);
        float z = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        shoot(x, y, z, velocity, inaccuracy);
        mcc$set$motionX(this, mcc$get$motionX(this) + mcc$get$motionX(shooter));
        mcc$set$motionZ(this, mcc$get$motionZ(this) + mcc$get$motionZ(shooter));
        if (!mcc$get$onGround(shooter)) mcc$set$motionY(this, mcc$get$motionY(this) + mcc$get$motionY(shooter));
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        var a = mcc$arrow();
        if (a != null) {
            a.shoot(x, y, z, velocity, inaccuracy);
            return;
        }
        float f = MathHelper.sqrt(x * x + y * y + z * z);
        x /= f;
        y /= f;
        z /= f;
        x += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        y += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        z += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        float h = MathHelper.sqrt(x * x + z * z);
        float yawNow = (float) (Math.atan2(x, z) * (180D / Math.PI));
        float pitchNow = (float) (Math.atan2(y, h) * (180D / Math.PI));
        mcc$set$rotationYaw(this, yawNow);
        mcc$set$prevRotationYaw(this, yawNow);
        mcc$set$rotationPitch(this, pitchNow);
        mcc$set$prevRotationPitch(this, pitchNow);
        ticksInGround = 0;
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        if (mcc$get$prevRotationPitch(this) == 0.0F && mcc$get$prevRotationYaw(this) == 0.0F) {
            float h = MathHelper.sqrt(x * x + z * z);
            float pitchNow = (float) (Math.atan2(y, h) * (180D / Math.PI));
            float yawNow = (float) (Math.atan2(x, z) * (180D / Math.PI));
            mcc$set$rotationPitch(this, pitchNow);
            mcc$set$prevRotationPitch(this, pitchNow);
            mcc$set$rotationYaw(this, yawNow);
            mcc$set$prevRotationYaw(this, yawNow);
            ticksInGround = 0;
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!mcc$hosted() || mcc$entity == null) return;
        var self = mcc$entity;
        var level = self.level();
        if (mcc$get$prevRotationPitch(this) == 0.0F && mcc$get$prevRotationYaw(this) == 0.0F) {
            float h = MathHelper.sqrt(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
            float yawNow = (float) (Math.atan2(mcc$get$motionX(this), mcc$get$motionZ(this)) * (180D / Math.PI));
            float pitchNow = (float) (Math.atan2(mcc$get$motionY(this), h) * (180D / Math.PI));
            mcc$set$rotationYaw(this, yawNow);
            mcc$set$prevRotationYaw(this, yawNow);
            mcc$set$rotationPitch(this, pitchNow);
            mcc$set$prevRotationPitch(this, pitchNow);
        }
        var tilePos = new net.minecraft.core.BlockPos(xTile, yTile, zTile);
        var state = level.getBlockState(tilePos);
        if (!state.isAir()) {
            var shape = state.getCollisionShape(level, tilePos);
            if (!shape.isEmpty() && shape.bounds().move(tilePos).contains(self.position())) inGround = true;
        }
        if (arrowShake > 0) --arrowShake;
        if (inGround) {
            if (org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.legacyBlock(state) == inTile && !level.noCollision(self.getBoundingBox().inflate(0.05D))) {
                ++ticksInGround;
                if (ticksInGround >= 1200) setDead();
            } else {
                inGround = false;
                mcc$set$motionX(this, mcc$get$motionX(this) * (rand.nextFloat() * 0.2F));
                mcc$set$motionY(this, mcc$get$motionY(this) * (rand.nextFloat() * 0.2F));
                mcc$set$motionZ(this, mcc$get$motionZ(this) * (rand.nextFloat() * 0.2F));
                ticksInGround = 0;
                ticksInAir = 0;
            }
            ++timeInGround;
            return;
        }
        timeInGround = 0;
        ++ticksInAir;
        var from = self.position();
        var to = from.add(self.getDeltaMovement());
        var blockHit = level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, self));
        net.minecraft.world.phys.HitResult hit = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : blockHit;
        if (hit != null) to = hit.getLocation();
        Entity entity = findEntityOnPath(legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d.mcc$of(from), legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d.mcc$of(to));
        RayTraceResult result = entity != null ? new RayTraceResult(entity) : hit == null ? null : RayTraceResult.mcc$of(hit);
        if (result != null && result.entityHit instanceof EntityPlayer target && shootingEntity instanceof EntityPlayer shooter && !shooter.canAttackPlayer(target)) {
            result = null;
        }
        if (result != null && !org.kylepitcock.legacyconnector.v1_12_2.entity.ProjectileEvents1122.impact(this, result)) onHit(result);
        if (getIsCritical() && level instanceof net.minecraft.server.level.ServerLevel sl) {
            var d = self.getDeltaMovement();
            for (int k = 0; k < 4; ++k) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, self.getX() + d.x * k / 4.0D, self.getY() + d.y * k / 4.0D,
                        self.getZ() + d.z * k / 4.0D, 1, -d.x, -d.y + 0.2D, -d.z, 0.0D);
            }
        }
        double nx = mcc$x() + mcc$get$motionX(this);
        double ny = mcc$y() + mcc$get$motionY(this);
        double nz = mcc$z() + mcc$get$motionZ(this);
        float h = MathHelper.sqrt(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
        mcc$set$rotationYaw(this, (float) (Math.atan2(mcc$get$motionX(this), mcc$get$motionZ(this)) * (180D / Math.PI)));
        mcc$set$rotationPitch(this, (float) (Math.atan2(mcc$get$motionY(this), h) * (180D / Math.PI)));
        while (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this) < -180.0F) mcc$set$prevRotationPitch(this, mcc$get$prevRotationPitch(this) - 360.0F);
        while (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this) >= 180.0F) mcc$set$prevRotationPitch(this, mcc$get$prevRotationPitch(this) + 360.0F);
        while (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this) < -180.0F) mcc$set$prevRotationYaw(this, mcc$get$prevRotationYaw(this) - 360.0F);
        while (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this) >= 180.0F) mcc$set$prevRotationYaw(this, mcc$get$prevRotationYaw(this) + 360.0F);
        mcc$set$rotationPitch(this, mcc$get$prevRotationPitch(this) + (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this)) * 0.2F);
        mcc$set$rotationYaw(this, mcc$get$prevRotationYaw(this) + (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this)) * 0.2F);
        float drag = 0.99F;
        if (isInWater()) {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, nx, ny, nz, 4, 0, 0, 0, 0);
            drag = 0.6F;
        }
        if (isWet()) extinguish();
        mcc$set$motionX(this, mcc$get$motionX(this) * drag);
        mcc$set$motionY(this, mcc$get$motionY(this) * drag);
        mcc$set$motionZ(this, mcc$get$motionZ(this) * drag);
        if (!hasNoGravity()) mcc$set$motionY(this, mcc$get$motionY(this) - 0.05000000074505806D);
        if (!mcc$get$isDead(this) && !inGround) setPosition(nx, ny, nz);
    }

    /** What happens on a hit: an entity is hurt (knocked back, set on fire) and the arrow removed; a block makes it stick. */
    protected void onHit(RayTraceResult result) {
        if (mcc$entity == null || result == null) return;
        var self = mcc$entity;
        Entity entity = result.entityHit;
        if (entity != null) {
            float speed = MathHelper.sqrt(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionY(this) * mcc$get$motionY(this)
                    + mcc$get$motionZ(this) * mcc$get$motionZ(this));
            int dmg = (int) Math.ceil(speed * damage);
            if (getIsCritical()) dmg += rand.nextInt(dmg / 2 + 2);
            legacyconnector.v1_12_2.net.minecraft.util.DamageSource source = legacyconnector.v1_12_2.net.minecraft.util.DamageSource.causeIndirectDamage(this,
                    shootingEntity instanceof EntityLivingBase l ? l : null);
            if (shootingEntity == null) source = legacyconnector.v1_12_2.net.minecraft.util.DamageSource.causeThrownDamage(this, this);
            source.setProjectile();
            if (isBurning() && !(entity.mcc$entity instanceof net.minecraft.world.entity.monster.EnderMan)) entity.setFire(5);
            if (entity.attackEntityFrom(source, dmg)) {
                if (entity instanceof EntityLivingBase living) {
                    if (!self.level().isClientSide()) living.setArrowCountInEntity(living.getArrowCountInEntity() + 1);
                    if (knockbackStrength > 0) {
                        float f = MathHelper.sqrt(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
                        if (f > 0.0F) living.addVelocity(mcc$get$motionX(this) * knockbackStrength * 0.6000000238418579D / f, 0.1D,
                                mcc$get$motionZ(this) * knockbackStrength * 0.6000000238418579D / f);
                    }
                    arrowHit(living);
                }
                playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.ARROW_HIT), 1.0F,
                        1.2F / (rand.nextFloat() * 0.2F + 0.9F));
                if (!(entity.mcc$entity instanceof net.minecraft.world.entity.monster.EnderMan)) setDead();
            } else {
                mcc$set$motionX(this, mcc$get$motionX(this) * -0.10000000149011612D);
                mcc$set$motionY(this, mcc$get$motionY(this) * -0.10000000149011612D);
                mcc$set$motionZ(this, mcc$get$motionZ(this) * -0.10000000149011612D);
                mcc$set$rotationYaw(this, mcc$get$rotationYaw(this) + 180.0F);
                mcc$set$prevRotationYaw(this, mcc$get$prevRotationYaw(this) + 180.0F);
                ticksInAir = 0;
                if (!self.level().isClientSide() && mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionY(this) * mcc$get$motionY(this)
                        + mcc$get$motionZ(this) * mcc$get$motionZ(this) < 0.0010000000474974513D) {
                    if (pickupStatus == PickupStatus.ALLOWED) entityDropItem(getArrowStack(), 0.1F);
                    setDead();
                }
            }
            return;
        }
        var pos = result.getBlockPos();
        if (pos == null) return;
        xTile = pos.getX();
        yTile = pos.getY();
        zTile = pos.getZ();
        var state = self.level().getBlockState(pos.mcc$modern());
        inTile = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.legacyBlock(state);
        var hitVec = result.hitVec;
        mcc$set$motionX(this, hitVec.x - mcc$x());
        mcc$set$motionY(this, hitVec.y - mcc$y());
        mcc$set$motionZ(this, hitVec.z - mcc$z());
        float f = MathHelper.sqrt(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionY(this) * mcc$get$motionY(this)
                + mcc$get$motionZ(this) * mcc$get$motionZ(this));
        if (f > 0.0F) {
            setPosition(mcc$x() - mcc$get$motionX(this) / f * 0.05000000074505806D, mcc$y() - mcc$get$motionY(this) / f * 0.05000000074505806D,
                    mcc$z() - mcc$get$motionZ(this) / f * 0.05000000074505806D);
        }
        playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.ARROW_HIT), 1.0F,
                1.2F / (rand.nextFloat() * 0.2F + 0.9F));
        inGround = true;
        arrowShake = 7;
        setIsCritical(false);
    }

    /** Extra effects on the living entity hit (tipped arrows' potions). */
    protected void arrowHit(EntityLivingBase living) {
    }

    /** The nearest entity the arrow passes through between the two points (not the shooter in its first ticks). */
    protected Entity findEntityOnPath(legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d start, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d end) {
        if (mcc$entity == null) return null;
        var self = mcc$entity;
        Entity best = null;
        double bestDist = 0.0D;
        var from = start.mcc$modern();
        var to = end.mcc$modern();
        for (var candidate : self.level().getEntities(self, self.getBoundingBox().expandTowards(self.getDeltaMovement()).inflate(1.0D))) {
            var view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(candidate);
            if (view == null || !view.canBeCollidedWith() || view == shootingEntity && ticksInAir < 5) continue;
            var intercept = candidate.getBoundingBox().inflate(0.30000001192092896D).clip(from, to);
            if (intercept.isPresent()) {
                double d = from.distanceToSqr(intercept.get());
                if (d < bestDist || bestDist == 0.0D) {
                    best = view;
                    bestDist = d;
                }
            }
        }
        return best;
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        if (mcc$entity == null || mcc$entity.level().isClientSide() || !inGround || arrowShake > 0) return;
        boolean take = pickupStatus == PickupStatus.ALLOWED || pickupStatus == PickupStatus.CREATIVE_ONLY && player.isCreative();
        if (pickupStatus == PickupStatus.ALLOWED && !player.addItemStackToInventory(getArrowStack())) take = false;
        if (take) {
            player.onItemPickup(this, 1);
            setDead();
        }
    }

    protected abstract ItemStack getArrowStack();

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    public void setDamage(double damage) {
        var a = mcc$arrow();
        if (a != null) a.setBaseDamage(damage);
        this.damage = damage;
    }

    public double getDamage() {
        var a = mcc$arrow();
        return a != null ? a.getBaseDamage() : damage;
    }

    public void setKnockbackStrength(int strength) {
        this.knockbackStrength = strength;
    }

    @Override
    public boolean canBeAttackedWithItem() {
        return false;
    }

    @Override
    public float getEyeHeight() {
        return 0.0F;
    }

    public void setIsCritical(boolean critical) {
        var a = mcc$arrow();
        if (a != null) a.setCritArrow(critical);
        mcc$critical = critical;
    }

    public boolean getIsCritical() {
        var a = mcc$arrow();
        return a != null ? a.isCritArrow() : mcc$critical;
    }

    /** Takes the power, punch and flame enchantments of the shooter's bow. */
    public void setEnchantmentEffectsFromEntity(EntityLivingBase shooter, float velocity) {
        var level = mcc$entity == null ? null : mcc$entity.level();
        setDamage(velocity * 2.0F + rand.nextGaussian() * 0.25D + (level == null ? 0.0F : level.getDifficulty().getId() * 0.11F));
        if (level == null || shooter == null || shooter.mcc$living() == null) return;
        var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var living = shooter.mcc$living();
        int power = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER), living);
        int punch = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PUNCH), living);
        int flame = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FLAME), living);
        if (power > 0) setDamage(getDamage() + power * 0.5D + 0.5D);
        if (punch > 0) setKnockbackStrength(punch);
        if (flame > 0) setFire(100);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("xTile", xTile);
        tag.setInteger("yTile", yTile);
        tag.setInteger("zTile", zTile);
        tag.setShort("life", (short) ticksInGround);
        var key = inTile == null ? null : inTile.getRegistryName();
        tag.setString("inTile", key == null ? "" : key.toString());
        tag.setByte("inData", (byte) inData);
        tag.setByte("shake", (byte) arrowShake);
        tag.setByte("inGround", (byte) (inGround ? 1 : 0));
        tag.setByte("pickup", (byte) pickupStatus.ordinal());
        tag.setDouble("damage", damage);
        tag.setBoolean("crit", getIsCritical());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        xTile = tag.getInteger("xTile");
        yTile = tag.getInteger("yTile");
        zTile = tag.getInteger("zTile");
        ticksInGround = tag.getShort("life");
        String tile = tag.getString("inTile");
        inTile = tile.isEmpty() ? null : legacyconnector.v1_12_2.net.minecraft.block.Block.getBlockFromName(tile);
        inData = tag.getByte("inData") & 255;
        arrowShake = tag.getByte("shake") & 255;
        inGround = tag.getByte("inGround") == 1;
        if (tag.hasKey("damage", 99)) damage = tag.getDouble("damage");
        if (tag.hasKey("pickup", 99)) pickupStatus = PickupStatus.getByOrdinal(tag.getByte("pickup"));
        setIsCritical(tag.getBoolean("crit"));
    }

    public enum PickupStatus {
        DISALLOWED, ALLOWED, CREATIVE_ONLY;

        public static PickupStatus getByOrdinal(int ordinal) {
            return ordinal < 0 || ordinal >= values().length ? DISALLOWED : values()[ordinal];
        }
    }
}
