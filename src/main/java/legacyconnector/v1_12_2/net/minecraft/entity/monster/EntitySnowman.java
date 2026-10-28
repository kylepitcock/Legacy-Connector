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

/** 1.12.2 snow golem: throws snowballs, wears a pumpkin. */
public class EntitySnowman extends EntityGolem implements legacyconnector.v1_12_2.net.minecraft.entity.IRangedAttackMob {
    public EntitySnowman(World world) {
        super(world);
        setSize(0.7F, 1.9F);
    }

    protected EntitySnowman(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.SnowGolem mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.SnowGolem m ? m : null;
    }

    private static final DataParameter<Byte> PUMPKIN_EQUIPPED = EntityDataManager.createKey(EntitySnowman.class, DataSerializers.BYTE);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(PUMPKIN_EQUIPPED, (byte) 16);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.20000000298023224D);
    }

    public boolean isPumpkinEquipped() {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) return m.hasPumpkin();
        Byte b = dataManager.get(PUMPKIN_EQUIPPED);
        return b != null && (b & 16) != 0;
    }

    public void setPumpkinEquipped(boolean equipped) {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) {
            m.setPumpkin(equipped);
            return;
        }
        Byte b = dataManager.get(PUMPKIN_EQUIPPED);
        byte v = b == null ? 0 : b;
        dataManager.set(PUMPKIN_EQUIPPED, (byte) (equipped ? v | 16 : v & -17));
    }

    @Override
    public void attackEntityWithRangedAttack(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, float distanceFactor) {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.RangedAttackMob r && target != null && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            r.performRangedAttack(l, distanceFactor);
        }
    }

    @Override
    public void setSwingingArms(boolean swinging) {
    }

    @Override
    public float getEyeHeight() {
        return 1.7F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.SNOW_GOLEM_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.SNOW_GOLEM_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.SNOW_GOLEM_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/snowman");
    }
}
