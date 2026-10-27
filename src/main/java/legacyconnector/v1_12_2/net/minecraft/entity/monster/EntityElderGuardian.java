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

/** 1.12.2 elder guardian. */
public class EntityElderGuardian extends EntityGuardian {
    public EntityElderGuardian(World world) {
        super(world);
        setSize(1.9975F, 1.9975F);
    }

    protected EntityElderGuardian(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.ElderGuardian mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.ElderGuardian m ? m : null;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(80.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(8.0D);
    }

    @Override
    public int getAttackDuration() {
        return 60;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.ELDER_GUARDIAN_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.ELDER_GUARDIAN_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.ELDER_GUARDIAN_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/elder_guardian");
    }
}
