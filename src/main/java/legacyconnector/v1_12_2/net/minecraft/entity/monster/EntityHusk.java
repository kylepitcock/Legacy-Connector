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

/** 1.12.2 husk: a zombie that does not burn in daylight and makes its victims hungry. */
public class EntityHusk extends EntityZombie {
    public EntityHusk(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    protected EntityHusk(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Husk mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Husk m ? m : null;
    }

    @Override
    protected boolean shouldBurnInDay() {
        return false;
    }

    @Override
    public boolean getCanSpawnHere() {
        return super.getCanSpawnHere() && (mcc$entity == null || mcc$entity.level().canSeeSky(mcc$entity.blockPosition()));
    }

    @Override
    public boolean attackEntityAsMob(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && mcc$entity != null && getHeldItemMainhand().isEmpty() && target != null
                && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            float f = mcc$entity.level().getCurrentDifficultyAt(mcc$entity.blockPosition()).getEffectiveDifficulty();
            l.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 140 * (int) f), mcc$entity);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.HUSK_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.HUSK_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.HUSK_DEATH);
    }

    @Override
    protected SoundEvent getStepSound() {
        return legacy(SoundEvents.HUSK_STEP);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/husk");
    }
}
