package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.LivingEntity;

/** 1.7.10 EntityAgeable: growing age (negative = child) in watcher slot 12. */
public abstract class EntityAgeable extends EntityCreature {
    public EntityAgeable(World world) {
        super(world);
    }

    protected EntityAgeable(LivingEntity modern) {
        super(modern);
    }

    public abstract EntityAgeable createChild(EntityAgeable mate);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(12, Integer.valueOf(0));
    }

    public int getGrowingAge() {
        if (mcc$entity instanceof net.minecraft.world.entity.AgeableMob a && !mcc$hosted()) return a.getAge();
        return dataWatcher.getWatchableObjectInt(12);
    }

    public void addGrowth(int amount) {
        int age = getGrowingAge();
        age += amount * 20;
        if (age > 0) age = 0;
        setGrowingAge(age);
    }

    public void setGrowingAge(int age) {
        if (mcc$entity instanceof net.minecraft.world.entity.AgeableMob a && !mcc$hosted()) a.setAge(age);
        else dataWatcher.updateObject(12, Integer.valueOf(age));
    }

    @Override
    public boolean isChild() {
        return getGrowingAge() < 0;
    }

    /** The unscaled size (width, height) the entity set; {@link #setScale} multiplies it. */
    public float field_98056_d;
    public float field_98057_e;

    /** A child is drawn and collides at half size. */
    public void setScaleForAge(boolean child) {
        setScale(child ? 0.5F : 1.0F);
    }

    /** The scale last set (0: none yet, i.e. 1). */
    private float mcc$scale;

    /**
     * Records the unscaled size and applies it at the current scale (1.7.10 applied a later size only with the next
     * setScaleForAge, which its client tick called every tick).
     */
    @Override
    public void setSize(float width, float height) {
        field_98056_d = width;
        field_98057_e = height;
        setScale(mcc$scale > 0.0F ? mcc$scale : 1.0F);
    }

    /** Sizes the entity to its unscaled size times {@code scale}. */
    protected void setScale(float scale) {
        mcc$scale = scale;
        super.setSize(field_98056_d * scale, field_98057_e * scale);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("Age", getGrowingAge());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setGrowingAge(tag.getInteger("Age"));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (mcc$hosted() && !isClientWorld()) {
            int age = getGrowingAge();
            if (age < 0) setGrowingAge(age + 1);
            else if (age > 0) setGrowingAge(age - 1);
        }
    }
}
