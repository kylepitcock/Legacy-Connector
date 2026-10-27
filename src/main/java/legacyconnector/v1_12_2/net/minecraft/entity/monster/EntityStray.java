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

/** 1.12.2 stray: a skeleton of the cold whose arrows slow. */
public class EntityStray extends AbstractSkeleton {
    public EntityStray(World world) {
        super(world);
        setSize(0.6F, 1.99F);
    }

    protected EntityStray(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Stray mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Stray m ? m : null;
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow getArrow(float distanceFactor) {
        var arrow = super.getArrow(distanceFactor);
        if (arrow instanceof legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityTippedArrow tipped) {
            tipped.addEffect(new legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect(legacyconnector.v1_12_2.net.minecraft.init.MobEffects.SLOWNESS, 600));
        }
        return arrow;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.STRAY_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.STRAY_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.STRAY_DEATH);
    }

    @Override
    protected SoundEvent getStepSound() {
        return legacy(SoundEvents.STRAY_STEP);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/stray");
    }
}
