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

/** 1.12.2 iron golem: guards villages; a view reads the modern golem (player created, holding a rose). */
public class EntityIronGolem extends EntityGolem {
    public EntityIronGolem(World world) {
        super(world);
        setSize(1.4F, 2.7F);
    }

    protected EntityIronGolem(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.IronGolem mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.IronGolem m ? m : null;
    }

    protected static final DataParameter<Byte> PLAYER_CREATED = EntityDataManager.createKey(EntityIronGolem.class, DataSerializers.BYTE);
    private int attackTimer;
    private int holdRoseTick;

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(PLAYER_CREATED, (byte) 0);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackMelee(this, 1.0D, true));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMoveTowardsTarget(this, 0.9D, 32.0F));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMoveThroughVillage(this, 0.6D, true));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMoveTowardsRestriction(this, 1.0D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 0.6D));
        tasks.addTask(7, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(8, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
        targetTasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, false));
        targetTasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this,
                legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving.class, 10, false, true,
                (com.google.common.base.Predicate<legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving>) e -> e instanceof IMob && !(e instanceof EntityCreeper)));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
    }

    public boolean isPlayerCreated() {
        var m = mcc$self();
        if (m != null) return m.isPlayerCreated();
        Byte b = dataManager.get(PLAYER_CREATED);
        return b != null && (b & 1) != 0;
    }

    public void setPlayerCreated(boolean created) {
        var m = mcc$self();
        if (m != null) {
            m.setPlayerCreated(created);
            return;
        }
        Byte b = dataManager.get(PLAYER_CREATED);
        byte v = b == null ? 0 : b;
        dataManager.set(PLAYER_CREATED, (byte) (created ? v | 1 : v & -2));
    }

    public int getAttackTimer() {
        var m = mcc$self();
        return m != null ? m.getAttackAnimationTick() : attackTimer;
    }

    public int getHoldRoseTick() {
        var m = mcc$self();
        return m != null ? m.getOfferFlowerTick() : holdRoseTick;
    }

    public void setHoldingRose(boolean holding) {
        var m = mcc$self();
        if (m != null) m.offerFlower(holding);
        else holdRoseTick = holding ? 400 : 0;
    }

    @Override
    public boolean attackEntityAsMob(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        attackTimer = 10;
        if (mcc$entity != null) mcc$entity.level().broadcastEntityEvent(mcc$entity, (byte) 4);
        boolean hit = target != null && target.attackEntityFrom(DamageSource.causeMobDamage(this), (float) (7 + rand.nextInt(15)));
        if (hit && target != null) target.addVelocity(0.0D, 0.4D, 0.0D);
        playSound(legacy(SoundEvents.IRON_GOLEM_ATTACK), 1.0F, 1.0F);
        return hit;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (attackTimer > 0) --attackTimer;
        if (holdRoseTick > 0) --holdRoseTick;
    }

    @Override
    public boolean canAttackClass(Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase> cls) {
        if (isPlayerCreated() && EntityPlayer.class.isAssignableFrom(cls)) return false;
        return cls != EntityCreeper.class && super.canAttackClass(cls);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() == null) tag.setBoolean("PlayerCreated", isPlayerCreated());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() == null) setPlayerCreated(tag.getBoolean("PlayerCreated"));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.IRON_GOLEM_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.IRON_GOLEM_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/iron_golem");
    }
}
