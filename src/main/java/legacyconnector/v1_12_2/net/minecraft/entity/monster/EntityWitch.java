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

/** 1.12.2 witch: throws harmful splash potions and drinks helpful ones. */
public class EntityWitch extends EntityMob implements legacyconnector.v1_12_2.net.minecraft.entity.IRangedAttackMob {
    public EntityWitch(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    protected EntityWitch(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Witch mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Witch m ? m : null;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackRanged(this, 1.0D, 60, 10.0F));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(26.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    public boolean isDrinkingPotion() {
        var m = mcc$self();
        return m != null && m.isDrinkingPotion();
    }

    /** Throws a splash potion at the target (harming, or slowness/poison/weakness by distance and state; verbatim 1.12 choice). */
    @Override
    public void attackEntityWithRangedAttack(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, float distanceFactor) {
        var m = mcc$entity;
        if (m == null || isDrinkingPotion() || target == null || target.mcc$entity == null) return;
        if (m instanceof net.minecraft.world.entity.monster.RangedAttackMob ranged && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l && !mcc$hosted()) {
            ranged.performRangedAttack(l, distanceFactor);
            return;
        }
        var t = target.mcc$entity;
        double dy = t.getEyeY() - 1.100000023841858D;
        double dx = t.getX() + t.getDeltaMovement().x - m.getX();
        double ddy = dy - m.getY();
        double dz = t.getZ() + t.getDeltaMovement().z - m.getZ();
        float f = (float) Math.sqrt(dx * dx + dz * dz);
        var potion = net.minecraft.world.item.alchemy.Potions.HARMING;
        if (f >= 8.0F && !(t instanceof net.minecraft.world.entity.LivingEntity le && le.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN))) {
            potion = net.minecraft.world.item.alchemy.Potions.SLOWNESS;
        } else if (t instanceof net.minecraft.world.entity.LivingEntity le && le.getHealth() >= 8.0F && !le.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
            potion = net.minecraft.world.item.alchemy.Potions.POISON;
        } else if (f <= 3.0F && !(t instanceof net.minecraft.world.entity.LivingEntity le && le.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS))
                && rand.nextFloat() < 0.25F) {
            potion = net.minecraft.world.item.alchemy.Potions.WEAKNESS;
        }
        var thrown = new net.minecraft.world.entity.projectile.ThrownPotion(m.level(), (net.minecraft.world.entity.LivingEntity) m);
        thrown.setItem(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.SPLASH_POTION, potion));
        thrown.setXRot(thrown.getXRot() + 20.0F);
        thrown.shoot(dx, ddy + f * 0.2F, dz, 0.75F, 8.0F);
        m.level().playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.WITCH_THROW, m.getSoundSource(), 1.0F, 0.8F + rand.nextFloat() * 0.4F);
        m.level().addFreshEntity(thrown);
    }

    @Override
    public void setSwingingArms(boolean swinging) {
    }

    @Override
    public float getEyeHeight() {
        return 1.62F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.WITCH_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.WITCH_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.WITCH_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/witch");
    }
}
