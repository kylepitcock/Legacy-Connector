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

/** 1.12.2 ocelot (a tamed ocelot is a cat with a skin): views read the modern ocelot, mods' subclasses keep the 1.12 state. */
public class EntityOcelot extends EntityTameable {
    public EntityOcelot(World world) {
        super(world);
        setSize(0.6F, 0.7F);
    }

    protected EntityOcelot(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Ocelot mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Ocelot m ? m : null;
    }

    private static final DataParameter<Integer> OCELOT_VARIANT = EntityDataManager.createKey(EntityOcelot.class, DataSerializers.VARINT);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(OCELOT_VARIANT, 0);
    }

    @Override
    protected void initEntityAI() {
        aiSit = new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISit(this);
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(2, aiSit);
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAITempt(this, 0.6D, legacyconnector.v1_12_2.net.minecraft.init.Items.FISH, true));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIFollowOwner(this, 1.0D, 10.0F, 5.0F));
        tasks.addTask(8, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILeapAtTarget(this, 0.3F));
        tasks.addTask(10, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMate(this, 0.8D));
        tasks.addTask(11, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 0.8D, 1.0000001E-5F));
        tasks.addTask(12, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 10.0F));
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAITargetNonTamed<>(this, EntityChicken.class, false, null));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
    }

    public int getTameSkin() {
        Integer v = dataManager.get(OCELOT_VARIANT);
        return v == null ? 0 : v;
    }

    public void setTameSkin(int skin) {
        dataManager.set(OCELOT_VARIANT, skin);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("CatType", getTameSkin());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setTameSkin(tag.getInteger("CatType"));
    }

    @Override
    public boolean canDespawn() {
        return !isTamed() && mcc$get$ticksExisted(this) > 2400;
    }

    @Override
    public boolean attackEntityAsMob(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        return target != null && target.attackEntityFrom(DamageSource.causeMobDamage(this), 3.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (isTamed()) return legacy(isInLove() ? SoundEvents.CAT_PURR : rand.nextInt(4) == 0 ? SoundEvents.CAT_PURREOW : SoundEvents.CAT_AMBIENT);
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.CAT_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.CAT_DEATH);
    }

    @Override
    public boolean isBreedingItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        if (!mcc$hosted()) return super.isBreedingItem(stack);
        return stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.FISH;
    }

    @Override
    public EntityOcelot createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityOcelot(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/ocelot");
    }
}
