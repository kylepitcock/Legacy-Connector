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

/** 1.12.2 cow (milked with a bucket). */
public class EntityCow extends EntityAnimal {
    public EntityCow(World world) {
        super(world);
        setSize(0.9F, 1.4F);
    }

    protected EntityCow(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Cow mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Cow m ? m : null;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIPanic(this, 2.0D));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMate(this, 1.0D));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAITempt(this, 1.25D, legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT, false));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIFollowParent(this, 1.25D));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(7, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.20000000298023224D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.COW_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.COW_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.COW_DEATH);
    }

    @Override
    public EntityCow createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityCow(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/cow");
    }
}
