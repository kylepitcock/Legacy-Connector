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

/** 1.12.2 slime: its size sets its health, speed, damage and box (a view reads and resizes the modern slime). */
public class EntitySlime extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving implements IMob {
    public EntitySlime(World world) {
        super(world);
        setSize(0.51000005F, 0.51000005F);
    }

    protected EntitySlime(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    /** The squish of a landing slime (smaller for small slimes). */
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getJumpSound() {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(isSmallSlime() ? net.minecraft.sounds.SoundEvents.SLIME_JUMP_SMALL
                : net.minecraft.sounds.SoundEvents.SLIME_JUMP);
    }

    protected net.minecraft.world.entity.monster.Slime mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Slime m ? m : null;
    }

    private static final DataParameter<Integer> SLIME_SIZE = EntityDataManager.createKey(EntitySlime.class, DataSerializers.VARINT);
    @Deprecated
    public float squishAmount;
    @Deprecated
    public float squishFactor;
    @Deprecated
    public float prevSquishFactor;
    private float mcc$squishAmount;
    private float mcc$squishFactor;
    private float mcc$prevSquishFactor;

    public static float mcc$get$squishAmount(EntitySlime e) {
        var m = e.mcc$self();
        return m != null ? m.targetSquish : e.mcc$squishAmount;
    }

    public static void mcc$set$squishAmount(EntitySlime e, float v) {
        var m = e.mcc$self();
        if (m != null) m.targetSquish = v;
        else e.mcc$squishAmount = v;
    }

    public static float mcc$get$squishFactor(EntitySlime e) {
        var m = e.mcc$self();
        return m != null ? m.squish : e.mcc$squishFactor;
    }

    public static void mcc$set$squishFactor(EntitySlime e, float v) {
        var m = e.mcc$self();
        if (m != null) m.squish = v;
        else e.mcc$squishFactor = v;
    }

    public static float mcc$get$prevSquishFactor(EntitySlime e) {
        var m = e.mcc$self();
        return m != null ? m.oSquish : e.mcc$prevSquishFactor;
    }

    public static void mcc$set$prevSquishFactor(EntitySlime e, float v) {
        var m = e.mcc$self();
        if (m != null) m.oSquish = v;
        else e.mcc$prevSquishFactor = v;
    }

    /** Phantom field: {@link #mcc$get$wasOnGround}. */
    @Deprecated
    protected boolean wasOnGround;
    private boolean mcc$wasOnGround;
    private static final java.lang.reflect.Field MCC$WAS_ON_GROUND = mcc$wasOnGroundField();

    private static java.lang.reflect.Field mcc$wasOnGroundField() {
        try {
            return net.neoforged.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.world.entity.monster.Slime.class, "wasOnGround");
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** wasOnGround: whether the slime stood on the ground last tick (it squishes and splashes when it lands). */
    public static boolean mcc$get$wasOnGround(EntitySlime e) {
        var m = e.mcc$self();
        if (m != null && MCC$WAS_ON_GROUND != null) {
            try {
                return MCC$WAS_ON_GROUND.getBoolean(m);
            } catch (IllegalAccessException ex) {
                // the local copy
            }
        }
        return e.mcc$wasOnGround;
    }

    public static void mcc$set$wasOnGround(EntitySlime e, boolean v) {
        var m = e.mcc$self();
        if (m != null && MCC$WAS_ON_GROUND != null) {
            try {
                MCC$WAS_ON_GROUND.setBoolean(m, v);
                return;
            } catch (IllegalAccessException ex) {
                // the local copy
            }
        }
        e.mcc$wasOnGround = v;
    }

    /** Eases the squish back towards rest (each tick: 60% of it remains). */
    protected void alterSquishAmount() {
        mcc$set$squishAmount(this, mcc$get$squishAmount(this) * 0.6F);
    }

    /** Ticks between two jumps: 10 to 29. */
    protected int getJumpDelay() {
        return rand.nextInt(20) + 10;
    }

    /** Whether jumping plays the squish sound. */
    protected boolean makesSoundOnJump() {
        return getSlimeSize() > 0;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SLIME_SIZE, 1);
    }

    public int getSlimeSize() {
        var m = mcc$self();
        if (m != null) return m.getSize();
        Integer s = dataManager.get(SLIME_SIZE);
        return s == null ? 1 : s;
    }

    /** Sets the size: box 0.51 per size step, health size squared, speed 0.2 + 0.1 per size (verbatim 1.12). */
    public void setSlimeSize(int size, boolean resetHealth) {
        var m = mcc$self();
        if (m != null) {
            m.setSize(size, resetHealth);
            return;
        }
        dataManager.set(SLIME_SIZE, size);
        setSize(0.51000005F * size, 0.51000005F * size);
        setPosition(mcc$x(), mcc$y(), mcc$z());
        var health = getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(size * size);
        var speed = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.2F + 0.1F * size);
        if (resetHealth) setHealth(getMaxHealth());
        experienceValue = size;
    }

    public boolean isSmallSlime() {
        return getSlimeSize() <= 1;
    }

    protected boolean canDamagePlayer() {
        return !isSmallSlime();
    }

    protected int getAttackStrength() {
        return getSlimeSize();
    }

    protected EntitySlime createInstance() {
        return new EntitySlime(mcc$get$world(this));
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData onInitialSpawn(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty,
            legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData data) {
        if (mcc$hosted()) {
            int i = rand.nextInt(3);
            if (i < 2 && rand.nextFloat() < 0.5F * (difficulty == null ? 0.0F : difficulty.getClampedAdditionalDifficulty())) ++i;
            setSlimeSize(1 << i, true);
        }
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() == null) tag.setInteger("Size", getSlimeSize() - 1);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() == null) setSlimeSize(Math.max(tag.getInteger("Size") + 1, 1), false);
    }

    @Override
    public float getEyeHeight() {
        return 0.625F * mcc$get$height(this);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(isSmallSlime() ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SLIME_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(isSmallSlime() ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SLIME_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return getSlimeSize() == 1 ? new ResourceLocation("minecraft", "entities/slime") : null;
    }
}
