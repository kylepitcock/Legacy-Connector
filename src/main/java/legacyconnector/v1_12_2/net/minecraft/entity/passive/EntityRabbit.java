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

/** 1.12.2 rabbit (a view reads the modern rabbit's type). */
public class EntityRabbit extends EntityAnimal {
    public EntityRabbit(World world) {
        super(world);
        setSize(0.4F, 0.5F);
    }

    protected EntityRabbit(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Rabbit mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Rabbit m ? m : null;
    }

    private static final DataParameter<Integer> RABBIT_TYPE = EntityDataManager.createKey(EntityRabbit.class, DataSerializers.VARINT);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(RABBIT_TYPE, 0);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIPanic(this, 2.2D));
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
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(3.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
    }

    public int getRabbitType() {
        var m = mcc$self();
        if (m != null) return m.getVariant().id();
        Integer t = dataManager.get(RABBIT_TYPE);
        return t == null ? 0 : t;
    }

    public void setRabbitType(int type) {
        var m = mcc$self();
        if (m != null) {
            m.setVariant(net.minecraft.world.entity.animal.Rabbit.Variant.byId(type));
            return;
        }
        dataManager.set(RABBIT_TYPE, type);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() == null) tag.setInteger("RabbitType", getRabbitType());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() == null) setRabbitType(tag.getInteger("RabbitType"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.RABBIT_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.RABBIT_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.RABBIT_DEATH);
    }

    @Override
    public boolean isBreedingItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        if (!mcc$hosted()) return super.isBreedingItem(stack);
        return stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.CARROT || stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.GOLDEN_CARROT;
    }

    @Override
    public EntityRabbit createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityRabbit(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/rabbit");
    }
}
