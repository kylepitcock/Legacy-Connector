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

/** 1.12.2 vex: a small flying summon that charges through walls. */
public class EntityVex extends EntityMob {
    public EntityVex(World world) {
        super(world);
        setSize(0.4F, 0.8F);
    }

    protected EntityVex(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Vex mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Vex m ? m : null;
    }

    private static final DataParameter<Byte> VEX_FLAGS = EntityDataManager.createKey(EntityVex.class, DataSerializers.BYTE);
    private legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving owner;
    private legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos boundOrigin;
    private boolean limitedLifespan;
    private int limitedLifeTicks;

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(VEX_FLAGS, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(14.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0D);
    }

    public boolean isCharging() {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) return m.isCharging();
        Byte b = dataManager.get(VEX_FLAGS);
        return b != null && (b & 1) != 0;
    }

    public void setCharging(boolean charging) {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) {
            m.setIsCharging(charging);
            return;
        }
        Byte b = dataManager.get(VEX_FLAGS);
        byte v = b == null ? 0 : b;
        dataManager.set(VEX_FLAGS, (byte) (charging ? v | 1 : v & -2));
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving getOwner() {
        return owner;
    }

    public void setOwner(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving owner) {
        this.owner = owner;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getBoundOrigin() {
        return boundOrigin;
    }

    public void setBoundOrigin(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        boundOrigin = pos;
    }

    public void setLimitedLife(int ticks) {
        limitedLifespan = true;
        limitedLifeTicks = ticks;
    }

    @Override
    public void onUpdate() {
        mcc$set$noClip(this, true);
        super.onUpdate();
        mcc$set$noClip(this, false);
        setNoGravity(true);
        if (mcc$hosted() && limitedLifespan && --limitedLifeTicks <= 0) {
            limitedLifeTicks = 20;
            attackEntityFrom(DamageSource.STARVE, 1.0F);
        }
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.VEX_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.VEX_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.VEX_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/vex");
    }
}
