package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/**
 * An ender crystal: it heals a nearby dragon and explodes when hit. Constructing this exact class makes a modern end crystal
 * (OreSpawn's dungeons place them); its spin ({@code innerRotation}) is the modern crystal's age. The legacy posY is the middle of
 * its 2-block box (1.7.10's yOffset), the modern y the bottom.
 */
public class EntityEnderCrystal extends Entity {
    /** Phantom: the spin counter (redirected to {@code mcc$get$innerRotation}). */
    @Deprecated
    public int innerRotation;
    private int mcc$local_innerRotation;
    public int health;

    public EntityEnderCrystal(World world) {
        super(world);
        preventEntitySpawning = true;
        setSize(2.0F, 2.0F);
        yOffset = mcc$get$height(this) / 2.0F;
        mcc$posYShift = yOffset;
        health = 5;
        mcc$set$innerRotation(this, rand.nextInt(100000));
    }

    public EntityEnderCrystal(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    /** View of a modern end crystal. */
    public EntityEnderCrystal(EndCrystal modern) {
        super(modern);
        yOffset = 1.0F;
        mcc$posYShift = yOffset;
        health = 5;
    }

    private EndCrystal mcc$crystal() {
        return mcc$entity instanceof EndCrystal c && !mcc$hosted() ? c : null;
    }

    public static int mcc$get$innerRotation(EntityEnderCrystal e) {
        var c = e.mcc$crystal();
        return c != null ? c.time : e.mcc$local_innerRotation;
    }

    public static void mcc$set$innerRotation(EntityEnderCrystal e, int v) {
        var c = e.mcc$crystal();
        if (c != null) c.time = v;
        else e.mcc$local_innerRotation = v;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(8, Integer.valueOf(health));
    }

    /** Spins, and keeps a fire burning under itself in the End. */
    @Override
    public void onUpdate() {
        if (mcc$crystal() != null || mcc$entity == null) {
            super.onUpdate();
            return;
        }
        mcc$set$prevPosX(this, mcc$get$posX(this));
        mcc$set$prevPosY(this, mcc$get$posY(this));
        mcc$set$prevPosZ(this, mcc$get$posZ(this));
        ++mcc$local_innerRotation;
        dataWatcher.updateObject(8, Integer.valueOf(health));
        World world = mcc$world();
        int x = MathHelper.floor_double(mcc$get$posX(this));
        int y = MathHelper.floor_double(mcc$get$posY(this));
        int z = MathHelper.floor_double(mcc$get$posZ(this));
        if (!world.isRemote && mcc$entity.level().dimension() == net.minecraft.world.level.Level.END && world.getBlock(x, y, z) != Blocks.fire) {
            world.setBlock(x, y, z, Blocks.fire);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
    }

    @Override
    public float getShadowSize() {
        return 0.0F;
    }

    @Override
    public boolean canBeCollidedWith() {
        return mcc$crystal() != null ? super.canBeCollidedWith() : true;
    }

    /** Any hit destroys it in a strength 6 explosion. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (mcc$crystal() != null) return super.attackEntityFrom(source, amount);
        if (isEntityInvulnerable()) return false;
        if (isEntityAlive() && !mcc$world().isRemote) {
            health = 0;
            setDead();
            mcc$world().createExplosion(null, mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), 6.0F, true);
        }
        return true;
    }
}
