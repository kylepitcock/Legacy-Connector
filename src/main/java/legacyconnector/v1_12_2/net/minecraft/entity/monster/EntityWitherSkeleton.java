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

/** 1.12.2 wither skeleton: fire immune, withers what it hits, carries a stone sword. */
public class EntityWitherSkeleton extends AbstractSkeleton {
    public EntityWitherSkeleton(World world) {
        super(world);
        setSize(0.7F, 2.4F);
    }

    protected EntityWitherSkeleton(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.WitherSkeleton mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.WitherSkeleton m ? m : null;
    }

    @Override
    public boolean isImmuneToFire() {
        return true;
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty) {
        setItemStackToSlot(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.MAINHAND, new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.STONE_SWORD));
    }

    @Override
    public boolean attackEntityAsMob(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && target != null && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            l.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200), mcc$entity);
        }
        return hit;
    }

    @Override
    public float getEyeHeight() {
        return 2.1F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.WITHER_SKELETON_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.WITHER_SKELETON_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.WITHER_SKELETON_DEATH);
    }

    @Override
    protected SoundEvent getStepSound() {
        return legacy(SoundEvents.WITHER_SKELETON_STEP);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/wither_skeleton");
    }
}
