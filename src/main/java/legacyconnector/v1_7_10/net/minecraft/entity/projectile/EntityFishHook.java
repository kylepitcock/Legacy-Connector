package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityXPOrb;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * 1.7.10 fishing hook. Mods' hooks (and their rods, which call {@link #handleHookRetraction}) run the 1.7.10 behaviour here: the hook
 * flies, bobs in water, waits for a fish (Lure shortens the wait, rain and open sky speed it up), and pulls up a catch from Forge's
 * fishing lists or the hooked entity. The vanilla hook a player casts is a view of the modern one.
 */
public class EntityFishHook extends Entity {
    public int xTile = -1;
    public int yTile = -1;
    public int zTile = -1;
    public Block inTile;
    public boolean inGround;
    public int shake;
    public EntityPlayer angler;
    public int ticksInGround;
    public int ticksInAir;
    public int ticksCatchable;
    public int ticksCaughtDelay;
    public int ticksCatchableDelay;
    public float fishApproachAngle;
    public Entity caughtEntity;
    public int fishPosRotationIncrements;
    public double fishX;
    public double fishY;
    public double fishZ;
    public double fishYaw;
    public double fishPitch;
    public double clientMotionX;
    public double clientMotionY;
    public double clientMotionZ;
    public boolean ignoreFrustumCheck;

    public EntityFishHook(World world) {
        super(world);
        setSize(0.25F, 0.25F);
        this.ignoreFrustumCheck = true;
    }

    public EntityFishHook(World world, double x, double y, double z, EntityPlayer angler) {
        this(world);
        setPosition(x, y, z);
        this.angler = angler;
        if (angler != null) EntityPlayer.mcc$set$fishEntity(angler, this);
    }

    public EntityFishHook(World world, EntityPlayer angler) {
        super(world);
        this.ignoreFrustumCheck = true;
        this.angler = angler;
        EntityPlayer.mcc$set$fishEntity(angler, this);
        setSize(0.25F, 0.25F);
        // from the angler's eyes (the modern eye height: 1.7.10's posY + 1.62 - yOffset)
        setLocationAndAngles(mcc$get$posX(angler), angler.mcc$entity.getEyeY(), mcc$get$posZ(angler), mcc$get$rotationYaw(angler), mcc$get$rotationPitch(angler));
        float yaw = mcc$get$rotationYaw(this), pitch = mcc$get$rotationPitch(this);
        setPosition(mcc$get$posX(this) - MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F, mcc$get$posY(this) - 0.10000000149011612D,
                mcc$get$posZ(this) - MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F);
        yOffset = 0.0F;
        float f = 0.4F;
        double mx = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI) * f;
        double mz = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI) * f;
        double my = -MathHelper.sin(pitch / 180.0F * (float) Math.PI) * f;
        handleHookCasting(mx, my, mz, 1.5F, 1.0F);
    }

    /** View of the modern hook a player cast. */
    public EntityFishHook(net.minecraft.world.entity.projectile.FishingHook modern) {
        super(modern);
        var owner = modern.getPlayerOwner();
        this.angler = owner == null ? null : (EntityPlayer) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(owner);
        var hooked = modern.getHookedIn();
        this.caughtEntity = hooked == null ? null : org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(hooked);
    }

    private net.minecraft.world.entity.projectile.FishingHook mcc$vanilla() {
        return mcc$entity instanceof net.minecraft.world.entity.projectile.FishingHook h && !mcc$hosted() ? h : null;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public boolean isInRangeToRenderDist(double distanceSq) {
        double d = mcc$get$boundingBox(this).getAverageEdgeLength() * 4.0D * 64.0D;
        return distanceSq < d * d;
    }

    /** Throws the hook along the direction (normalised, scattered by {@code inaccuracy}, scaled by {@code velocity}). */
    public void handleHookCasting(double x, double y, double z, float velocity, float inaccuracy) {
        float len = MathHelper.sqrt_double(x * x + y * y + z * z);
        x /= len;
        y /= len;
        z /= len;
        x += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        y += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        z += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        float horizontal = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(y, horizontal) * 180.0D / Math.PI);
        mcc$set$rotationYaw(this, yaw);
        mcc$set$prevRotationYaw(this, yaw);
        mcc$set$rotationPitch(this, pitch);
        mcc$set$prevRotationPitch(this, pitch);
        ticksInGround = 0;
    }

    /** Client: the server's position, reached over {@code increments} ticks by {@link #onUpdate}. */
    @Override
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {
        if (mcc$vanilla() != null) {
            super.setPositionAndRotation2(x, y, z, yaw, pitch, increments);
            return;
        }
        fishX = x;
        fishY = y;
        fishZ = z;
        fishYaw = yaw;
        fishPitch = pitch;
        fishPosRotationIncrements = increments;
        mcc$set$motionX(this, clientMotionX);
        mcc$set$motionY(this, clientMotionY);
        mcc$set$motionZ(this, clientMotionZ);
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        clientMotionX = x;
        clientMotionY = y;
        clientMotionZ = z;
        super.setVelocity(x, y, z);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (mcc$vanilla() != null) return;
        World world = mcc$world();
        if (fishPosRotationIncrements > 0) {
            double x = mcc$get$posX(this) + (fishX - mcc$get$posX(this)) / fishPosRotationIncrements;
            double y = mcc$get$posY(this) + (fishY - mcc$get$posY(this)) / fishPosRotationIncrements;
            double z = mcc$get$posZ(this) + (fishZ - mcc$get$posZ(this)) / fishPosRotationIncrements;
            double dYaw = MathHelper.wrapAngleTo180_double(fishYaw - mcc$get$rotationYaw(this));
            float yaw = (float) (mcc$get$rotationYaw(this) + dYaw / fishPosRotationIncrements);
            float pitch = (float) (mcc$get$rotationPitch(this) + (fishPitch - mcc$get$rotationPitch(this)) / fishPosRotationIncrements);
            --fishPosRotationIncrements;
            setPosition(x, y, z);
            setRotation(yaw, pitch);
            return;
        }
        if (!world.isRemote) {
            ItemStack held = angler == null ? null : angler.getCurrentEquippedItem();
            if (angler == null || !angler.isEntityAlive() || held == null || held.getItem() != legacyconnector.v1_7_10.net.minecraft.init.Items.fishing_rod
                    || getDistanceSqToEntity(angler) > 1024.0D) {
                setDead();
                if (angler != null) EntityPlayer.mcc$set$fishEntity(angler, null);
                return;
            }
            if (caughtEntity != null) {
                if (caughtEntity.isEntityAlive()) {
                    setPosition(mcc$get$posX(caughtEntity), mcc$get$boundingBox(caughtEntity).minY + mcc$get$height(caughtEntity) * 0.8D, mcc$get$posZ(caughtEntity));
                    return;
                }
                caughtEntity = null;
            }
        }
        if (shake > 0) --shake;
        if (inGround) {
            if (world.getBlock(xTile, yTile, zTile) == inTile) {
                if (++ticksInGround == 1200) setDead();
                return;
            }
            inGround = false;
            mcc$set$motionX(this, mcc$get$motionX(this) * rand.nextFloat() * 0.2F);
            mcc$set$motionY(this, mcc$get$motionY(this) * rand.nextFloat() * 0.2F);
            mcc$set$motionZ(this, mcc$get$motionZ(this) * rand.nextFloat() * 0.2F);
            ticksInGround = 0;
            ticksInAir = 0;
        } else {
            ++ticksInAir;
        }
        mcc$flyAndCollide(world);
        if (!inGround) mcc$moveAndBob(world);
    }

    /** Ray-traces the step of this tick: hooks the first entity in the way or sticks in a block. */
    private void mcc$flyAndCollide(World world) {
        double x = mcc$get$posX(this), y = mcc$get$posY(this), z = mcc$get$posZ(this);
        double mx = mcc$get$motionX(this), my = mcc$get$motionY(this), mz = mcc$get$motionZ(this);
        Vec3 from = Vec3.createVectorHelper(x, y, z);
        Vec3 to = Vec3.createVectorHelper(x + mx, y + my, z + mz);
        MovingObjectPosition hit = world.rayTraceBlocks(from, to);
        from = Vec3.createVectorHelper(x, y, z);
        to = hit != null ? Vec3.createVectorHelper(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord) : Vec3.createVectorHelper(x + mx, y + my, z + mz);
        Entity closest = null;
        double best = 0.0D;
        List<?> list = world.getEntitiesWithinAABBExcludingEntity(this, mcc$get$boundingBox(this).addCoord(mx, my, mz).expand(1.0D, 1.0D, 1.0D));
        for (Object o : list) {
            if (!(o instanceof Entity e) || !e.canBeCollidedWith() || e == angler && ticksInAir < 5) continue;
            AxisAlignedBB box = mcc$get$boundingBox(e).expand(0.3D, 0.3D, 0.3D);
            MovingObjectPosition h = box.calculateIntercept(from, to);
            if (h == null) continue;
            double d = from.distanceTo(h.hitVec);
            if (d < best || best == 0.0D) {
                closest = e;
                best = d;
            }
        }
        if (closest != null) hit = new MovingObjectPosition(closest);
        if (hit == null) return;
        if (hit.entityHit != null) {
            if (hit.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, angler), 0.0F)) caughtEntity = hit.entityHit;
        } else {
            // as in 1.7.10 the tile is not recorded, so the hook only stops for this tick and then drops onto the block
            inGround = true;
        }
    }

    /** Moves the hook, turns it along its flight, floats it in water and runs the bite timers. */
    private void mcc$moveAndBob(World world) {
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        double mx = mcc$get$motionX(this), my = mcc$get$motionY(this), mz = mcc$get$motionZ(this);
        float horizontal = MathHelper.sqrt_double(mx * mx + mz * mz);
        float yaw = (float) (Math.atan2(mx, mz) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(my, horizontal) * 180.0D / Math.PI);
        float prevPitch = mcc$get$prevRotationPitch(this), prevYaw = mcc$get$prevRotationYaw(this);
        while (pitch - prevPitch < -180.0F) prevPitch -= 360.0F;
        while (pitch - prevPitch >= 180.0F) prevPitch += 360.0F;
        while (yaw - prevYaw < -180.0F) prevYaw -= 360.0F;
        while (yaw - prevYaw >= 180.0F) prevYaw += 360.0F;
        mcc$set$prevRotationPitch(this, prevPitch);
        mcc$set$prevRotationYaw(this, prevYaw);
        mcc$set$rotationPitch(this, prevPitch + (pitch - prevPitch) * 0.2F);
        mcc$set$rotationYaw(this, prevYaw + (yaw - prevYaw) * 0.2F);
        float drag = 0.92F;
        if (mcc$get$onGround(this) || mcc$get$isCollidedHorizontally(this)) drag = 0.5F;
        AxisAlignedBB box = mcc$get$boundingBox(this);
        double submerged = 0.0D;
        for (int i = 0; i < 5; i++) {
            double minY = box.minY + (box.maxY - box.minY) * i / 5.0D - 0.125D + 0.125D;
            double maxY = box.minY + (box.maxY - box.minY) * (i + 1) / 5.0D - 0.125D + 0.125D;
            AxisAlignedBB slice = AxisAlignedBB.getBoundingBox(box.minX, minY, box.minZ, box.maxX, maxY, box.maxZ);
            if (world.isAABBInMaterial(slice, legacyconnector.v1_7_10.net.minecraft.block.material.Material.water)) submerged += 0.2D;
        }
        if (!world.isRemote && submerged > 0.0D && mcc$entity != null && mcc$entity.level() instanceof ServerLevel level) mcc$bite(world, level, box);
        my = mcc$get$motionY(this) + 0.03999999910593033D * (submerged * 2.0D - 1.0D);
        if (submerged > 0.0D) {
            drag = (float) (drag * 0.9D);
            my *= 0.8D;
        }
        mcc$set$motionX(this, mcc$get$motionX(this) * drag);
        mcc$set$motionY(this, my * drag);
        mcc$set$motionZ(this, mcc$get$motionZ(this) * drag);
        setPosition(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
    }

    /** The bite timers: wait for a fish, show it approaching, then a short window in which reeling in catches it. */
    private void mcc$bite(World world, ServerLevel level, AxisAlignedBB box) {
        int x = MathHelper.floor_double(mcc$get$posX(this)), y = MathHelper.floor_double(mcc$get$posY(this)) + 1, z = MathHelper.floor_double(mcc$get$posZ(this));
        int speed = 1;
        if (rand.nextFloat() < 0.25F && world.canLightningStrikeAt(x, y, z)) speed = 2;
        if (rand.nextFloat() < 0.5F && !world.canBlockSeeTheSky(x, y, z)) --speed;
        float width = mcc$get$width(this);
        double px = mcc$get$posX(this), pz = mcc$get$posZ(this);
        double surface = MathHelper.floor_double(box.minY) + 1.0D;
        if (ticksCatchable > 0) {
            if (--ticksCatchable <= 0) {
                ticksCaughtDelay = 0;
                ticksCatchableDelay = 0;
            }
        } else if (ticksCatchableDelay > 0) {
            ticksCatchableDelay -= speed;
            if (ticksCatchableDelay <= 0) {
                mcc$set$motionY(this, mcc$get$motionY(this) - 0.20000000298023224D);
                playSound("random.splash", 0.25F, 1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
                int count = (int) (1.0F + width * 20.0F);
                level.sendParticles(ParticleTypes.BUBBLE, px, surface, pz, count, width, 0.0D, width, 0.20000000298023224D);
                level.sendParticles(ParticleTypes.FISHING, px, surface, pz, count, width, 0.0D, width, 0.20000000298023224D);
                ticksCatchable = MathHelper.getRandomIntegerInRange(rand, 10, 30);
            } else {
                fishApproachAngle = (float) (fishApproachAngle + rand.nextGaussian() * 4.0D);
                float angle = fishApproachAngle * 0.017453292F;
                float sin = MathHelper.sin(angle), cos = MathHelper.cos(angle);
                double fx = px + sin * ticksCatchableDelay * 0.1F;
                double fz = pz + cos * ticksCatchableDelay * 0.1F;
                if (rand.nextFloat() < 0.15F) level.sendParticles(ParticleTypes.BUBBLE, fx, surface - 0.10000000149011612D, fz, 1, sin, 0.1D, cos, 0.0D);
                float ox = sin * 0.04F, oz = cos * 0.04F;
                level.sendParticles(ParticleTypes.FISHING, fx, surface, fz, 0, oz, 0.01D, -ox, 1.0D);
                level.sendParticles(ParticleTypes.FISHING, fx, surface, fz, 0, -oz, 0.01D, ox, 1.0D);
            }
        } else if (ticksCaughtDelay > 0) {
            ticksCaughtDelay -= speed;
            float chance = 0.15F;
            if (ticksCaughtDelay < 20) chance = (float) (chance + (20 - ticksCaughtDelay) * 0.05D);
            else if (ticksCaughtDelay < 40) chance = (float) (chance + (40 - ticksCaughtDelay) * 0.02D);
            else if (ticksCaughtDelay < 60) chance = (float) (chance + (60 - ticksCaughtDelay) * 0.01D);
            if (rand.nextFloat() < chance) {
                float angle = MathHelper.randomFloatClamp(rand, 0.0F, 360.0F) * 0.017453292F;
                float dist = MathHelper.randomFloatClamp(rand, 25.0F, 60.0F);
                double fx = px + MathHelper.sin(angle) * dist * 0.1F;
                double fz = pz + MathHelper.cos(angle) * dist * 0.1F;
                level.sendParticles(ParticleTypes.SPLASH, fx, surface, fz, 2 + rand.nextInt(2), 0.10000000149011612D, 0.0D, 0.10000000149011612D, 0.0D);
            }
            if (ticksCaughtDelay <= 0) {
                fishApproachAngle = MathHelper.randomFloatClamp(rand, 0.0F, 360.0F);
                ticksCatchableDelay = MathHelper.getRandomIntegerInRange(rand, 20, 80);
            }
        } else {
            ticksCaughtDelay = MathHelper.getRandomIntegerInRange(rand, 100, 900);
            if (angler != null) ticksCaughtDelay -= legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper.func_151387_h(angler) * 20 * 5;
        }
        if (ticksCatchable > 0) mcc$set$motionY(this, mcc$get$motionY(this) - rand.nextFloat() * rand.nextFloat() * rand.nextFloat() * 0.2D);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setShort("xTile", (short) xTile);
        tag.setShort("yTile", (short) yTile);
        tag.setShort("zTile", (short) zTile);
        tag.setByte("inTile", (byte) Block.getIdFromBlock(inTile));
        tag.setByte("shake", (byte) shake);
        tag.setByte("inGround", (byte) (inGround ? 1 : 0));
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        xTile = tag.getShort("xTile");
        yTile = tag.getShort("yTile");
        zTile = tag.getShort("zTile");
        inTile = Block.getBlockById(tag.getByte("inTile") & 255);
        shake = tag.getByte("shake") & 255;
        inGround = tag.getByte("inGround") == 1;
    }

    @Override
    public float getShadowSize() {
        return 0.0F;
    }

    /**
     * Reels the hook in: pulls a hooked entity towards the angler (3 damage to the rod), lands a catch with experience (1), or
     * frees a hook stuck in a block (2). Returns the damage the rod takes.
     */
    public int handleHookRetraction() {
        var vanilla = mcc$vanilla();
        if (vanilla != null) {
            var owner = vanilla.getPlayerOwner();
            var rod = owner == null ? net.minecraft.world.item.ItemStack.EMPTY
                    : owner.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem ? owner.getMainHandItem() : owner.getOffhandItem();
            return vanilla.retrieve(rod);
        }
        World world = mcc$world();
        if (world.isRemote) return 0;
        int damage = 0;
        if (caughtEntity != null && angler != null) {
            double dx = mcc$get$posX(angler) - mcc$get$posX(this), dy = mcc$get$posY(angler) - mcc$get$posY(this), dz = mcc$get$posZ(angler) - mcc$get$posZ(this);
            double dist = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
            caughtEntity.addVelocity(dx * 0.1D, dy * 0.1D + MathHelper.sqrt_double(dist) * 0.08D, dz * 0.1D);
            damage = 3;
        } else if (ticksCatchable > 0 && angler != null) {
            ItemStack caught = mcc$fishingResult();
            if (caught != null) {
                EntityItem item = new EntityItem(world, mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), caught);
                double dx = mcc$get$posX(angler) - mcc$get$posX(this), dy = mcc$get$posY(angler) - mcc$get$posY(this), dz = mcc$get$posZ(angler) - mcc$get$posZ(this);
                double dist = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
                item.setVelocity(dx * 0.1D, dy * 0.1D + MathHelper.sqrt_double(dist) * 0.08D, dz * 0.1D);
                world.spawnEntityInWorld(item);
            }
            World anglerWorld = mcc$get$worldObj(angler);
            anglerWorld.spawnEntityInWorld(new EntityXPOrb(anglerWorld, mcc$get$posX(angler), mcc$get$posY(angler) + 0.5D, mcc$get$posZ(angler) + 0.5D, rand.nextInt(6) + 1));
            damage = 1;
        }
        if (inGround) damage = 2;
        setDead();
        if (angler != null) EntityPlayer.mcc$set$fishEntity(angler, null);
        return damage;
    }

    /** The catch, from Forge's junk/treasure/fish lists (Luck of the Sea and Lure shift the odds). */
    private ItemStack mcc$fishingResult() {
        int luck = legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper.func_151386_g(angler);
        int lure = legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper.func_151387_h(angler);
        return legacyconnector.v1_7_10.net.minecraftforge.common.FishingHooks.getRandomFishable(rand, rand.nextFloat(), luck, lure);
    }

    @Override
    public void setDead() {
        super.setDead();
        if (angler != null && EntityPlayer.mcc$get$fishEntity(angler) == this) EntityPlayer.mcc$set$fishEntity(angler, null);
    }
}
