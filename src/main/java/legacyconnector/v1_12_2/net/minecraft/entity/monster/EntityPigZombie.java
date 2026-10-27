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

/** 1.12.2 zombie pigman (the modern zombified piglin): neutral until hurt, then angry with its group. */
public class EntityPigZombie extends EntityZombie {
    public EntityPigZombie(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    protected EntityPigZombie(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.ZombifiedPiglin mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.ZombifiedPiglin m ? m : null;
    }

    private int angerLevel;
    private java.util.UUID angerTargetUUID;

    @Override
    protected void applyEntityAI() {
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0D);
    }

    @Override
    protected boolean shouldBurnInDay() {
        return false;
    }

    public boolean isAngry() {
        var m = mcc$self();
        return m != null ? m.isAngry() : angerLevel > 0;
    }

    /** Becomes angry at an entity for 400 to 800 ticks (1.12). */
    public void becomeAngryAt(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        var m = mcc$self();
        if (m != null && target != null && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            m.setTarget(l);
            m.startPersistentAngerTimer();
            return;
        }
        angerLevel = 400 + rand.nextInt(400);
        if (target instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase l) setRevengeTarget(l);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable(source)) return false;
        if (mcc$self() == null && source != null && source.getTrueSource() instanceof EntityPlayer p) becomeAngryAt(p);
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void onLivingUpdate() {
        if (mcc$self() == null && angerLevel > 0) --angerLevel;
        super.onLivingUpdate();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() != null) return;
        tag.setShort("Anger", (short) angerLevel);
        tag.setString("HurtBy", angerTargetUUID == null ? "" : angerTargetUUID.toString());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() != null) return;
        angerLevel = tag.getShort("Anger");
        String s = tag.getString("HurtBy");
        angerTargetUUID = s.isEmpty() ? null : java.util.UUID.fromString(s);
    }

    @Override
    public boolean isImmuneToFire() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.ZOMBIFIED_PIGLIN_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.ZOMBIFIED_PIGLIN_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/zombified_piglin");
    }
}
