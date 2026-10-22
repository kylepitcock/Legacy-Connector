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

    public void setScaleForAge(boolean child) {
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
