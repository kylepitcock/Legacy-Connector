package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.AgeableMob;

/**
 * 1.12.2 EntityAgeable: growing age (negative while a child). Views of modern ageable mobs read the modern age; a hosted mod animal
 * keeps its own and sends the child flag to clients through its data.
 */
public abstract class EntityAgeable extends EntityCreature {
    private static final DataParameter<Boolean> BABY = EntityDataManager.createKey(EntityAgeable.class, DataSerializers.BOOLEAN);
    protected int growingAge;
    protected int forcedAge;
    protected int forcedAgeTimer;
    private float ageWidth = -1.0F;
    private float ageHeight;

    public EntityAgeable(World world) {
        super(world);
    }

    protected EntityAgeable(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    private AgeableMob mcc$ageable() {
        return mcc$entity instanceof AgeableMob a ? a : null;
    }

    public abstract EntityAgeable createChild(EntityAgeable ageable);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(BABY, false);
    }

    public int getGrowingAge() {
        AgeableMob a = mcc$ageable();
        if (a != null) return a.getAge();
        if (mcc$entity != null && mcc$entity.level().isClientSide()) return Boolean.TRUE.equals(dataManager.get(BABY)) ? -1 : 1;
        return growingAge;
    }

    public void ageUp(int growthSeconds, boolean updateForcedAge) {
        AgeableMob a = mcc$ageable();
        if (a != null) {
            a.ageUp(growthSeconds, updateForcedAge);
            return;
        }
        int i = getGrowingAge();
        int j = i;
        i += growthSeconds * 20;
        if (i > 0) {
            i = 0;
            if (j < 0) onGrowingAdult();
        }
        int k = i - j;
        setGrowingAge(i);
        if (updateForcedAge) {
            forcedAge += k;
            if (forcedAgeTimer == 0) forcedAgeTimer = 40;
        }
        if (getGrowingAge() == 0) setGrowingAge(forcedAge);
    }

    public void addGrowth(int growth) {
        ageUp(growth, false);
    }

    public void setGrowingAge(int age) {
        AgeableMob a = mcc$ageable();
        if (a != null) {
            a.setAge(age);
            return;
        }
        boolean wasChild = growingAge < 0;
        dataManager.set(BABY, age < 0);
        growingAge = age;
        setScaleForAge(isChild());
        if (wasChild != age < 0 && !wasChild) {
            // became a child
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$ageable() != null) return;
        tag.setInteger("Age", getGrowingAge());
        tag.setInteger("ForcedAge", forcedAge);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$ageable() != null) return;
        setGrowingAge(tag.getInteger("Age"));
        forcedAge = tag.getInteger("ForcedAge");
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (BABY.equals(key)) setScaleForAge(isChild());
        super.notifyDataManagerChange(key);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (mcc$ageable() != null || mcc$entity == null) return;
        if (mcc$entity.level().isClientSide()) {
            if (forcedAgeTimer > 0) --forcedAgeTimer;
        } else if (isEntityAlive()) {
            int i = getGrowingAge();
            if (i < 0) setGrowingAge(i + 1);
            else if (i > 0) setGrowingAge(i - 1);
        }
    }

    protected void onGrowingAdult() {
    }

    @Override
    public boolean isChild() {
        AgeableMob a = mcc$ageable();
        if (a != null) return a.isBaby();
        return getGrowingAge() < 0;
    }

    public void setScaleForAge(boolean child) {
        setScale(child ? 0.5F : 1.0F);
    }

    @Override
    public void setSize(float width, float height) {
        boolean first = ageWidth > 0.0F;
        ageWidth = width;
        ageHeight = height;
        if (!first) setScale(1.0F);
        super.setSize(width, height);
    }

    protected final void setScale(float scale) {
        if (ageWidth > 0.0F) super.setSize(ageWidth * scale, ageHeight * scale);
    }

    /** Whether the stack is a spawn egg of exactly that entity class. */
    protected boolean holdingSpawnEggOfClass(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack, Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.Entity> entityClass) {
        if (stack == null || stack.isEmpty() || entityClass == null) return false;
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        if (!(modern.getItem() instanceof net.minecraft.world.item.SpawnEggItem egg)) return false;
        var type = egg.getType(modern);
        var level = mcc$entity == null ? null : mcc$entity.level();
        if (type == null || level == null) return false;
        var probe = type.create(level);
        if (probe == null) return false;
        try {
            var view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(probe);
            return view != null && view.getClass() == entityClass;
        } finally {
            probe.discard();
        }
    }
}
