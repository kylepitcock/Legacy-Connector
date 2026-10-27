package legacyconnector.v1_12_2.net.minecraft.entity.monster;

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

/** 1.12.2 illusioner: shoots arrows and casts blindness and mirror images. */
public class EntityIllusionIllager extends EntitySpellcasterIllager implements legacyconnector.v1_12_2.net.minecraft.entity.IRangedAttackMob {
    public EntityIllusionIllager(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    protected EntityIllusionIllager(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Illusioner mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Illusioner m ? m : null;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackMelee(this, 1.0D, false));
        tasks.addTask(8, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWander(this, 0.6D));
        tasks.addTask(9, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 3.0F, 1.0F));
        tasks.addTask(10, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving.class, 8.0F));
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, true, AbstractIllager.class));
        targetTasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        targetTasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager.class, true));
        targetTasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(32.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(18.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.ILLUSIONER_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.ILLUSIONER_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.ILLUSIONER_DEATH);
    }

    @Override
    public void attackEntityWithRangedAttack(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, float distanceFactor) {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.RangedAttackMob r && target != null && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            r.performRangedAttack(l, distanceFactor);
        }
    }

    @Override
    public void setSwingingArms(boolean swinging) {
        setAggressive(1, swinging);
    }
}
