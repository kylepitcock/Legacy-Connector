package mccompat.v1_7_10.net.minecraft.entity;

import java.util.Arrays;
import mccompat.v1_7_10.net.minecraft.entity.ai.EntityAITasks;
import mccompat.v1_7_10.net.minecraft.entity.ai.EntityJumpHelper;
import mccompat.v1_7_10.net.minecraft.entity.ai.EntityLookHelper;
import mccompat.v1_7_10.net.minecraft.entity.ai.EntityMoveHelper;
import mccompat.v1_7_10.net.minecraft.entity.ai.EntitySenses;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.pathfinding.PathNavigate;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 1.7.10 EntityLiving: AI (task lists or the old updateEntityActionState AI), helpers, drops and the mob state the
 * host keeps (target, name tag, persistence, equipment drop chances, leash).
 */
public abstract class EntityLiving extends EntityLivingBase {
    public int livingSoundTime;
    protected int experienceValue;
    protected EntityLookHelper lookHelper;
    protected EntityMoveHelper moveHelper;
    protected EntityJumpHelper jumpHelper;
    protected PathNavigate navigator;
    public final EntityAITasks tasks;
    public final EntityAITasks targetTasks;
    protected EntitySenses senses;
    protected float[] equipmentDropChances = new float[5];
    protected float defaultPitch;
    protected int numTicksToChaseTarget;
    private Entity mcc$lookTarget;

    public EntityLiving(World world) {
        super(world);
        this.tasks = new EntityAITasks();
        this.targetTasks = new EntityAITasks();
        this.lookHelper = new EntityLookHelper(this);
        this.moveHelper = new EntityMoveHelper(this);
        this.jumpHelper = new EntityJumpHelper(this);
        this.senses = new EntitySenses(this);
        this.navigator = new PathNavigate(this, world);
        Arrays.fill(equipmentDropChances, 0.085F);
    }

    protected EntityLiving(LivingEntity modern) {
        super(modern);
        this.tasks = new EntityAITasks();
        this.targetTasks = new EntityAITasks();
    }

    private Mob mcc$mob() {
        return mcc$entity instanceof Mob m ? m : null;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.followRange);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(11, Byte.valueOf((byte) 0));
        dataWatcher.addObject(10, "");
    }

    public EntityLookHelper getLookHelper() {
        return lookHelper;
    }

    public EntityMoveHelper getMoveHelper() {
        return moveHelper;
    }

    public EntityJumpHelper getJumpHelper() {
        return jumpHelper;
    }

    public PathNavigate getNavigator() {
        return navigator;
    }

    public EntitySenses getEntitySenses() {
        return senses;
    }

    public EntityLivingBase getAttackTarget() {
        Mob m = mcc$mob();
        return m == null || m.getTarget() == null ? null : (EntityLivingBase) org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge.wrap(m.getTarget());
    }

    public void setAttackTarget(EntityLivingBase target) {
        Mob m = mcc$mob();
        if (m == null) return;
        LivingEntity t = target == null ? null : target.mcc$living();
        if (m instanceof org.kylepitcock.mccompat.v1_7_10.entity.LegacyLivingHost h) h.mcc$superSetTarget(t);
        else m.setTarget(t);
    }

    public boolean canAttackClass(Class<?> cls) {
        String n = cls.getSimpleName();
        return !n.equals("EntityCreeper") && !n.equals("EntityGhast");
    }

    public void eatGrassBonus() {
    }

    public int getTalkInterval() {
        return 80;
    }

    public void playLivingSound() {
        String s = getLivingSound();
        if (s != null) playSound(s, getSoundVolume(), getSoundPitch());
    }

    protected int getExperiencePoints(EntityPlayer player) {
        if (experienceValue <= 0) return experienceValue;
        int xp = experienceValue;
        for (int i = 0; i < 5; i++) {
            if (getEquipmentInSlot(i) != null && equipmentDropChances[i] <= 1.0F) xp += 1 + getRNG().nextInt(3);
        }
        return xp;
    }

    public void spawnExplosionParticle() {
        if (mcc$mob() != null) mcc$mob().spawnAnim();
    }

    protected String getLivingSound() {
        return null;
    }

    protected Item getDropItem() {
        return null;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        Item item = getDropItem();
        if (item == null) return;
        int n = getRNG().nextInt(3);
        if (looting > 0) n += getRNG().nextInt(looting + 1);
        for (int i = 0; i < n; i++) dropItem(item, 1);
    }

    public void setMoveForward(float forward) {
        mcc$set$moveForward(this, forward);
    }

    protected boolean canDespawn() {
        return true;
    }

    protected void despawnEntity() {
        if (mcc$mob() != null) mcc$mob().checkDespawn();
    }

    /** The new AI: target tasks, tasks, then custom move/look/jump helpers (vanilla ones are ticked by the host). */
    protected void updateAITasks() {
        ++entityAge;
        senses.clearSensingCache();
        targetTasks.onUpdateTasks();
        tasks.onUpdateTasks();
        updateAITick();
        if (moveHelper != null && moveHelper.getClass() != EntityMoveHelper.class) moveHelper.onUpdateMoveHelper();
        if (lookHelper != null && lookHelper.getClass() != EntityLookHelper.class) lookHelper.onUpdateLook();
        if (jumpHelper != null && jumpHelper.getClass() != EntityJumpHelper.class) jumpHelper.doJump();
    }

    protected void updateAITick() {
    }

    /** The old AI: idle looking around at nearby players. */
    protected void updateEntityActionState() {
        ++entityAge;
        mcc$set$moveStrafing(this, 0.0F);
        setMoveForward(0.0F);
        float range = 8.0F;
        if (getRNG().nextFloat() < 0.02F) {
            EntityPlayer p = mcc$world() == null ? null : mcc$world().getClosestPlayerToEntity(this, range);
            mcc$lookTarget = p;
            numTicksToChaseTarget = p == null ? 0 : 10 + getRNG().nextInt(20);
        }
        if (mcc$lookTarget != null) {
            faceEntity(mcc$lookTarget, 10.0F, getVerticalFaceSpeed());
            if (numTicksToChaseTarget-- <= 0 || !mcc$lookTarget.isEntityAlive() || mcc$lookTarget.getDistanceSqToEntity(this) > range * range) {
                mcc$lookTarget = null;
            }
        } else if (getRNG().nextFloat() < 0.05F) {
            randomYawVelocity = (getRNG().nextFloat() - 0.5F) * 20.0F;
        }
        if (mcc$lookTarget == null && randomYawVelocity != 0.0F) {
            mcc$set$rotationYaw(this, mcc$get$rotationYaw(this) + randomYawVelocity);
            mcc$set$rotationPitch(this, defaultPitch);
        }
        if ((isInWater() || handleLavaMovement()) && getRNG().nextFloat() < 0.8F) setJumping(true);
    }

    public int getVerticalFaceSpeed() {
        return 40;
    }

    public void faceEntity(Entity target, float maxYawChange, float maxPitchChange) {
        double dx = target.mcc$x() - mcc$x();
        double dz = target.mcc$z() - mcc$z();
        double dy = target instanceof EntityLivingBase l
                ? l.mcc$y() + l.getEyeHeight() - (mcc$y() + getEyeHeight())
                : target.mcc$y() - (mcc$y() + getEyeHeight());
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, dist) * 180.0 / Math.PI);
        mcc$set$rotationPitch(this, updateRotation(mcc$get$rotationPitch(this), pitch, maxPitchChange));
        mcc$set$rotationYaw(this, updateRotation(mcc$get$rotationYaw(this), yaw, maxYawChange));
    }

    private float updateRotation(float current, float target, float max) {
        float d = net.minecraft.util.Mth.wrapDegrees(target - current);
        if (d > max) d = max;
        if (d < -max) d = -max;
        return current + d;
    }

    public boolean getCanSpawnHere() {
        Mob m = mcc$mob();
        if (m == null) return true;
        var level = m.level();
        return level.isUnobstructed(m) && level.noCollision(m) && !level.containsAnyLiquid(m.getBoundingBox());
    }

    public float getRenderSizeModifier() {
        return 1.0F;
    }

    public int getMaxSpawnedInChunk() {
        return 4;
    }

    public int getMaxSafePointTries() {
        return getAttackTarget() == null ? 3 : 3 + (int) (getHealth() - 1.0F);
    }

    protected void addRandomArmor() {
    }

    protected void enchantEquipment() {
    }

    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        return data;
    }

    public boolean canBeSteered() {
        return false;
    }

    public void enablePersistence() {
        if (mcc$mob() != null) mcc$mob().setPersistenceRequired();
    }

    public void setCustomNameTag(String name) {
        if (mcc$entity != null) mcc$entity.setCustomName(name == null || name.isEmpty() ? null : Component.literal(name));
    }

    public String getCustomNameTag() {
        return mcc$entity == null || mcc$entity.getCustomName() == null ? "" : mcc$entity.getCustomName().getString();
    }

    public boolean hasCustomNameTag() {
        return mcc$entity != null && mcc$entity.hasCustomName();
    }

    public void setAlwaysRenderNameTag(boolean always) {
        if (mcc$entity != null) mcc$entity.setCustomNameVisible(always);
    }

    public boolean getAlwaysRenderNameTag() {
        return mcc$entity != null && mcc$entity.isCustomNameVisible();
    }

    public boolean getAlwaysRenderNameTagForRender() {
        return getAlwaysRenderNameTag();
    }

    public void setEquipmentDropChance(int slot, float chance) {
        if (slot >= 0 && slot < equipmentDropChances.length) equipmentDropChances[slot] = chance;
        Mob m = mcc$mob();
        if (m != null) m.setDropChance(mcc$slot(slot), chance);
    }

    public boolean canPickUpLoot() {
        Mob m = mcc$mob();
        return m != null && m.canPickUpLoot();
    }

    public void setCanPickUpLoot(boolean b) {
        Mob m = mcc$mob();
        if (m != null) m.setCanPickUpLoot(b);
    }

    public boolean isNoDespawnRequired() {
        Mob m = mcc$mob();
        return m != null && m.isPersistenceRequired();
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        return interact(player);
    }

    protected boolean interact(EntityPlayer player) {
        return false;
    }

    public void clearLeashed(boolean sendPacket, boolean dropLead) {
        Mob m = mcc$mob();
        if (m != null) m.dropLeash(sendPacket, dropLead);
    }

    public boolean allowLeashing() {
        Mob m = mcc$mob();
        return m != null && m.canBeLeashed();
    }

    public boolean getLeashed() {
        Mob m = mcc$mob();
        return m != null && m.isLeashed();
    }

    public Entity getLeashedToEntity() {
        Mob m = mcc$mob();
        return m == null || m.getLeashHolder() == null ? null : org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge.wrap(m.getLeashHolder());
    }

    public void setLeashedToEntity(Entity holder, boolean sendPacket) {
        Mob m = mcc$mob();
        if (m != null && holder != null && holder.mcc$entity != null) m.setLeashedTo(holder.mcc$entity, sendPacket);
    }

    // ------------------------------------------------------------------ bridges for the host

    public final void mcc$serverAiStep() {
        if (isAIEnabled()) updateAITasks();
        else updateEntityActionState();
    }

    public final int mcc$experience() {
        return getExperiencePoints(null);
    }

    public final String mcc$livingSound() {
        return getLivingSound();
    }

    public final boolean mcc$canDespawn() {
        return canDespawn();
    }

    static EquipmentSlot mcc$equipmentSlot(int legacy) {
        return mcc$slot(legacy);
    }
}
