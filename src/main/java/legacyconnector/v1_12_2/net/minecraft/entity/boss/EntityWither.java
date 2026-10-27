package legacyconnector.v1_12_2.net.minecraft.entity.boss;

import legacyconnector.v1_12_2.net.minecraft.entity.monster.IMob;

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

/** 1.12.2 wither: three heads with their own targets, an invulnerable charge-up, armour below half health. A view
 * reads the modern wither. */
public class EntityWither extends legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityMob implements legacyconnector.v1_12_2.net.minecraft.entity.IRangedAttackMob {
    public EntityWither(World world) {
        super(world);
        setSize(0.9F, 3.5F);
    }

    protected EntityWither(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.boss.wither.WitherBoss mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss m ? m : null;
    }

    private int mcc$invulTime;

    public static boolean canDestroyBlock(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(block.getDefaultState());
        return net.minecraft.world.entity.boss.wither.WitherBoss.canDestroy(modern);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(300.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.6000000238418579D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(4.0D);
    }

    public int getInvulTime() {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? m.getInvulnerableTicks() : mcc$invulTime;
    }

    public void setInvulTime(int ticks) {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) m.setInvulnerableTicks(ticks);
        mcc$invulTime = ticks;
    }

    public int getWatchedTargetId(int head) {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? m.getAlternativeTarget(head) : 0;
    }

    public void updateWatchedTargetId(int head, int id) {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) m.setAlternativeTarget(head, id);
    }

    /** Below half health: arrows bounce off. */
    public boolean isArmored() {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? m.isPowered() : getHealth() <= getMaxHealth() / 2.0F;
    }

    /** 1.12: starts the 220-tick charge-up at a third of its health. */
    public void ignite() {
        setInvulTime(220);
        setHealth(getMaxHealth() / 3.0F);
    }

    public float getHeadYRotation(int head) {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? m.getHeadYRot(head) : mcc$get$rotationYaw(this);
    }

    public float getHeadXRotation(int head) {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? m.getHeadXRot(head) : mcc$get$rotationPitch(this);
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    @Override
    public boolean isImmuneToFire() {
        return true;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute getCreatureAttribute() {
        return legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$hosted() || mcc$self() == null) tag.setInteger("Invul", getInvulTime());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$hosted() || mcc$self() == null) setInvulTime(tag.getInteger("Invul"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.WITHER_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.WITHER_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.WITHER_DEATH);
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
}
