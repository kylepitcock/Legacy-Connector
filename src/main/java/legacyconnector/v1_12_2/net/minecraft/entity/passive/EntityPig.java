package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/** 1.12.2 pig: saddles and riding (a view reads the modern pig). */
public class EntityPig extends EntityAnimal {
    public EntityPig(World world) {
        super(world);
        setSize(0.9F, 0.9F);
    }

    protected EntityPig(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Pig mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Pig m ? m : null;
    }

    private static final DataParameter<Boolean> SADDLED = EntityDataManager.createKey(EntityPig.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> BOOST_TIME = EntityDataManager.createKey(EntityPig.class, DataSerializers.VARINT);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SADDLED, false);
        dataManager.register(BOOST_TIME, 0);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIPanic(this, 1.25D));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMate(this, 1.0D));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAITempt(this, 1.25D, legacyconnector.v1_12_2.net.minecraft.init.Items.CARROT, false));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIFollowParent(this, 1.25D));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(7, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    public boolean getSaddled() {
        var m = mcc$self();
        return m != null ? m.isSaddled() : Boolean.TRUE.equals(dataManager.get(SADDLED));
    }

    public void setSaddled(boolean saddled) {
        var m = mcc$self();
        if (m != null) {
            if (saddled && !m.isSaddled()) m.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
            return;
        }
        dataManager.set(SADDLED, saddled);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() == null) tag.setBoolean("Saddle", getSaddled());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() == null) setSaddled(tag.getBoolean("Saddle"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.PIG_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.PIG_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.PIG_DEATH);
    }

    @Override
    public boolean isBreedingItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        if (!mcc$hosted()) return super.isBreedingItem(stack);
        return stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.CARROT || stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.POTATO || stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.BEETROOT;
    }

    @Override
    public EntityPig createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityPig(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/pig");
    }
}
