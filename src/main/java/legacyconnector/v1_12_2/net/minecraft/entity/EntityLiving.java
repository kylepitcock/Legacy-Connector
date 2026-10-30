package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAITasks;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityJumpHelper;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityLookHelper;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityMoveHelper;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntitySenses;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigate;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNodeType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/**
 * 1.12.2 EntityLiving: the AI task lists, helpers, navigation, drops and the mob state the host keeps (target, name tag, persistence,
 * equipment drop chances, leash). A host ticks the legacy task lists and updateAITasks as its server AI step; the vanilla move, look
 * and jump controls stand behind the legacy helpers unless a mod replaces them.
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
    protected float[] inventoryHandsDropChances = new float[2];
    protected float[] inventoryArmorDropChances = new float[4];
    protected ResourceLocation deathLootTable;
    protected long deathLootTableSeed;
    private boolean mcc$noAI;

    public EntityLiving(World world) {
        super(world);
        this.tasks = new EntityAITasks(null);
        this.targetTasks = new EntityAITasks(null);
        this.lookHelper = new EntityLookHelper(this);
        this.moveHelper = new EntityMoveHelper(this);
        this.jumpHelper = new EntityJumpHelper(this);
        this.senses = new EntitySenses(this);
        java.util.Arrays.fill(inventoryHandsDropChances, 0.085F);
        java.util.Arrays.fill(inventoryArmorDropChances, 0.085F);
        try {
            this.navigator = createNavigator(world);
        } catch (RuntimeException | LinkageError e) {
            this.navigator = new legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigateGround(this, world);
        }
        if (mcc$entity != null && !mcc$hosted()) {
            // a view of a vanilla mob (made through its World constructor): legacy tasks a mod adds run as modern goals
            tasks.mcc$bindGoals(this, false);
            targetTasks.mcc$bindGoals(this, true);
        } else if (world != null && !world.isRemote) {
            try {
                initEntityAI();
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("initEntityAI of {} failed: {}", getClass().getName(), e.toString());
            }
        }
    }

    protected EntityLiving(LivingEntity modern) {
        super(modern);
        this.tasks = new EntityAITasks(null);
        this.targetTasks = new EntityAITasks(null);
        this.lookHelper = new EntityLookHelper(this);
        this.moveHelper = new EntityMoveHelper(this);
        this.jumpHelper = new EntityJumpHelper(this);
        this.senses = new EntitySenses(this);
        this.navigator = new legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigateGround(this, null);
        java.util.Arrays.fill(inventoryHandsDropChances, 0.085F);
        java.util.Arrays.fill(inventoryArmorDropChances, 0.085F);
        // views of vanilla mobs: legacy tasks a mod adds run as modern goals
        tasks.mcc$bindGoals(this, false);
        targetTasks.mcc$bindGoals(this, true);
    }

    private Mob mcc$mob() {
        return mcc$entity instanceof Mob m ? m : null;
    }

    protected void initEntityAI() {
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
    }

    protected PathNavigate createNavigator(World world) {
        return new legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigateGround(this, world);
    }

    public float getPathPriority(PathNodeType type) {
        Mob m = mcc$mob();
        return m == null || type == null ? 0.0F : m.getPathfindingMalus(type.mcc$modern());
    }

    public void setPathPriority(PathNodeType type, float priority) {
        Mob m = mcc$mob();
        if (m != null && type != null) m.setPathfindingMalus(type.mcc$modern(), priority);
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
        return m == null ? null : EntityBridge1122.wrap(m.getTarget()) instanceof EntityLivingBase t ? t : null;
    }

    public void setAttackTarget(EntityLivingBase target) {
        Mob m = mcc$mob();
        if (m == null) return;
        LivingEntity t = target == null ? null : target.mcc$living();
        if (m instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.LivingHost1122 h && h.mcc$legacy() == this) h.mcc$superSetTarget(t);
        else m.setTarget(t);
    }

    public boolean canAttackClass(Class<? extends EntityLivingBase> cls) {
        String n = cls.getSimpleName();
        return !n.equals("EntityGhast");
    }

    public void eatGrassBonus() {
    }

    public int getTalkInterval() {
        return 80;
    }

    public void playLivingSound() {
        var sound = getAmbientSound();
        if (sound != null) playSound(sound, getSoundVolume(), getSoundPitch());
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getAmbientSound() {
        return null;
    }

    public final legacyconnector.v1_12_2.net.minecraft.util.SoundEvent mcc$ambientSound() {
        return getAmbientSound();
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        if (experienceValue <= 0) return experienceValue;
        int xp = experienceValue;
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            float chance = slot.getSlotType() == EntityEquipmentSlot.Type.HAND ? inventoryHandsDropChances[slot.getIndex()] : inventoryArmorDropChances[slot.getIndex()];
            if (!getItemStackFromSlot(slot).isEmpty() && chance <= 1.0F) xp += 1 + rand.nextInt(3);
        }
        return xp;
    }

    public void spawnExplosionParticle() {
        Mob m = mcc$mob();
        if (m != null) m.spawnAnim();
    }

    protected Item getDropItem() {
        return null;
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
        Item item = getDropItem();
        if (item == null) return;
        int n = rand.nextInt(3);
        if (lootingModifier > 0) n += rand.nextInt(lootingModifier + 1);
        for (int i = 0; i < n; i++) dropItem(item, 1);
    }

    /** The mod's loot table for the death drops (null: dropFewItems). */
    protected ResourceLocation getLootTable() {
        return null;
    }

    public final ResourceLocation mcc$lootTable() {
        ResourceLocation table = deathLootTable != null ? deathLootTable : getLootTable();
        return table;
    }

    /** 1.12: a loot table when the mod names one (the host's modern loot already rolled it), else dropFewItems. */
    @Override
    protected void dropLoot(boolean wasRecentlyHit, int lootingModifier, legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        if (mcc$lootTable() == null) super.dropLoot(wasRecentlyHit, lootingModifier, source);
    }

    public void setMoveForward(float amount) {
        mcc$set$moveForward(this, amount);
    }

    public void setMoveVertical(float amount) {
        mcc$set$moveVertical(this, amount);
    }

    public void setMoveStrafing(float amount) {
        mcc$set$moveStrafing(this, amount);
    }

    protected boolean canDespawn() {
        return true;
    }

    public final boolean mcc$canDespawn() {
        return canDespawn();
    }

    protected void despawnEntity() {
        Mob m = mcc$mob();
        if (m != null) m.checkDespawn();
    }

    /** The AI step: the legacy target tasks and tasks, then updateAITasks (the host's server AI step). */
    public final void mcc$serverAiStep() {
        if (isAIDisabled()) return;
        try {
            senses.clearSensingCache();
            targetTasks.onUpdateTasks();
            tasks.onUpdateTasks();
            if (navigator != null && navigator.getClass() != legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigateGround.class) navigator.mcc$tickCustom();
            updateAITasks();
            if (moveHelper != null && moveHelper.getClass() != EntityMoveHelper.class) moveHelper.onUpdateMoveHelper();
            if (lookHelper != null && lookHelper.getClass() != EntityLookHelper.class) lookHelper.onUpdateLook();
            if (jumpHelper != null && jumpHelper.getClass() != EntityJumpHelper.class) jumpHelper.doJump();
        } finally {
            org.kylepitcock.legacyconnector.v1_12_2.entity.LiveStacks1122.flushAll();
        }
    }

    protected void updateAITasks() {
    }

    @Override
    protected void updateEntityActionState() {
    }

    public int getVerticalFaceSpeed() {
        return 40;
    }

    public int getHorizontalFaceSpeed() {
        return 10;
    }

    public void faceEntity(Entity target, float maxYawIncrease, float maxPitchIncrease) {
        double dx = target.mcc$x() - mcc$x();
        double dz = target.mcc$z() - mcc$z();
        double dy;
        if (target instanceof EntityLivingBase l) {
            dy = l.mcc$y() + l.getEyeHeight() - (mcc$y() + getEyeHeight());
        } else {
            var box = target.getEntityBoundingBox();
            dy = (box.minY + box.maxY) / 2.0D - (mcc$y() + getEyeHeight());
        }
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, dist) * (180D / Math.PI));
        mcc$set$rotationPitch(this, updateRotation(mcc$get$rotationPitch(this), pitch, maxPitchIncrease));
        mcc$set$rotationYaw(this, updateRotation(mcc$get$rotationYaw(this), yaw, maxYawIncrease));
    }

    private float updateRotation(float angle, float target, float maxIncrease) {
        float f = net.minecraft.util.Mth.wrapDegrees(target - angle);
        if (f > maxIncrease) f = maxIncrease;
        if (f < -maxIncrease) f = -maxIncrease;
        return angle + f;
    }

    /** Whether the mob may spawn where it stands (no collisions; mods add light and block checks). */
    public boolean getCanSpawnHere() {
        Mob m = mcc$mob();
        if (m == null) return true;
        var region = mcc$worldOverride;
        if (region != null) {
            var pos = m.blockPosition().below();
            return region.getBlockState(pos).isValidSpawn(region, pos, m.getType());
        }
        var level = m.level();
        var below = m.blockPosition().below();
        return level.getBlockState(below).isValidSpawn(level, below, m.getType());
    }

    /** Whether the spawn spot is free of blocks, liquids and other entities. */
    public boolean isNotColliding() {
        Mob m = mcc$mob();
        if (m == null) return true;
        var region = mcc$worldOverride;
        if (region != null) return !region.containsAnyLiquid(m.getBoundingBox()) && region.noCollision(m) && region.isUnobstructed(m);
        var level = m.level();
        return !level.containsAnyLiquid(m.getBoundingBox()) && level.noCollision(m) && level.isUnobstructed(m);
    }

    public float getRenderSizeModifier() {
        return 1.0F;
    }

    public int getMaxSpawnedInChunk() {
        return 4;
    }

    public boolean isMaxGroupSize(int size) {
        return false;
    }

    public boolean canBeSteered() {
        return false;
    }

    public void enablePersistence() {
        Mob m = mcc$mob();
        if (m != null) m.setPersistenceRequired();
    }

    public void setDropChance(EntityEquipmentSlot slot, float chance) {
        if (slot.getSlotType() == EntityEquipmentSlot.Type.HAND) inventoryHandsDropChances[slot.getIndex()] = chance;
        else inventoryArmorDropChances[slot.getIndex()] = chance;
        Mob m = mcc$mob();
        if (m != null) m.setDropChance(mcc$slot(slot), chance);
    }

    public boolean canPickUpLoot() {
        Mob m = mcc$mob();
        return m != null && m.canPickUpLoot();
    }

    public void setCanPickUpLoot(boolean canPickup) {
        Mob m = mcc$mob();
        if (m != null) m.setCanPickUpLoot(canPickup);
    }

    public boolean isNoDespawnRequired() {
        Mob m = mcc$mob();
        return m != null && m.isPersistenceRequired();
    }

    public static EntityEquipmentSlot getSlotForItemStack(ItemStack stack) {
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        if (modern.isEmpty()) return EntityEquipmentSlot.MAINHAND;
        if (modern.getItem() instanceof net.minecraft.world.item.ShieldItem) return EntityEquipmentSlot.OFFHAND;
        var equipable = net.minecraft.world.item.Equipable.get(modern);
        return equipable == null ? EntityEquipmentSlot.MAINHAND : mcc$slot(equipable.getEquipmentSlot());
    }

    public boolean canEquipItem(ItemStack stack) {
        return true;
    }

    /** Right-clicking the mob (after the leash and name tag handling); true when it used the click. */
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        Mob m = mcc$mob();
        if (m instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.LivingHost1122 h && h.mcc$legacy() == this && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) {
            return h.mcc$superMobInteract(p, mcc$hand(hand)).consumesAction();
        }
        return false;
    }

    public final boolean mcc$processInteract(EntityPlayer player, EnumHand hand) {
        return processInteract(player, hand);
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        return processInteract(player, hand);
    }

    protected void updateLeashedState() {
    }

    public void clearLeashed(boolean sendPacket, boolean dropLead) {
        Mob m = mcc$mob();
        if (m != null) m.dropLeash(sendPacket, dropLead);
    }

    public boolean canBeLeashedTo(EntityPlayer player) {
        Mob m = mcc$mob();
        return m != null && m.canBeLeashed();
    }

    public boolean getLeashed() {
        Mob m = mcc$mob();
        return m != null && m.isLeashed();
    }

    public Entity getLeashHolder() {
        Mob m = mcc$mob();
        return m == null ? null : EntityBridge1122.wrap(m.getLeashHolder());
    }

    public void setLeashHolder(Entity holder, boolean sendAttachNotification) {
        Mob m = mcc$mob();
        if (m != null && holder != null && holder.mcc$entity != null) m.setLeashedTo(holder.mcc$entity, sendAttachNotification);
    }

    public void setNoAI(boolean disable) {
        mcc$noAI = disable;
        Mob m = mcc$mob();
        if (m != null) m.setNoAi(disable);
    }

    public boolean isAIDisabled() {
        Mob m = mcc$mob();
        return m != null ? m.isNoAi() : mcc$noAI;
    }

    public void setLeftHanded(boolean leftHanded) {
        Mob m = mcc$mob();
        if (m != null) m.setLeftHanded(leftHanded);
    }

    public boolean isLeftHanded() {
        Mob m = mcc$mob();
        return m != null && m.isLeftHanded();
    }

    @Override
    public boolean isServerWorld() {
        return super.isServerWorld() && !isAIDisabled();
    }

    public static Item getArmorByChance(EntityEquipmentSlot slot, int chance) {
        String material = switch (chance) {
            case 0 -> "LEATHER";
            case 1 -> "GOLDEN";
            case 2 -> "CHAINMAIL";
            case 3 -> "IRON";
            case 4 -> "DIAMOND";
            default -> null;
        };
        if (material == null) return null;
        String piece = switch (slot) {
            case HEAD -> "_HELMET";
            case CHEST -> "_CHESTPLATE";
            case LEGS -> "_LEGGINGS";
            case FEET -> "_BOOTS";
            default -> null;
        };
        if (piece == null) return null;
        try {
            return (Item) legacyconnector.v1_12_2.net.minecraft.init.Items.class.getField(material + piece).get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ spawning and equipment (area E, 1.12.2)

    /** Client interpolation targets (kept for legacy renderers and movement code; the modern entity interpolates itself). */
    public int newPosRotationIncrements;
    public double interpTargetX;
    public double interpTargetY;
    public double interpTargetZ;
    public double interpTargetYaw;
    public double interpTargetPitch;
    public float randomYawVelocity;
    public float movedDistance;
    public float prevMovedDistance;
    @Deprecated
    public boolean persistenceRequired;
    @Deprecated
    public EntityLivingBase attackTarget;

    public static boolean mcc$get$persistenceRequired(EntityLiving e) {
        return e.isNoDespawnRequired();
    }

    public static void mcc$set$persistenceRequired(EntityLiving e, boolean v) {
        if (v) e.enablePersistence();
    }

    public static EntityLivingBase mcc$get$attackTarget(EntityLiving e) {
        return e.getAttackTarget();
    }

    public static void mcc$set$attackTarget(EntityLiving e, EntityLivingBase v) {
        e.setAttackTarget(v);
    }

    /**
     * Called once when the mob is spawned naturally, by a spawner or an egg (1.12: a random follow range bonus and, one time in twenty,
     * left-handedness; the modern spawn finalization does the same). Returns the data shared with the rest of its group.
     */
    public IEntityLivingData onInitialSpawn(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty, IEntityLivingData livingdata) {
        var m = mcc$entity;
        if (m instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.MobHost1122 host && mcc$hosted()) {
            var modernDifficulty = m.level().getCurrentDifficultyAt(m.blockPosition());
            var result = host.mcc$superFinalizeSpawn(modernDifficulty, org.kylepitcock.legacyconnector.v1_12_2.entity.SpawnData1122.Group.modern(livingdata));
            var legacy = org.kylepitcock.legacyconnector.v1_12_2.entity.SpawnData1122.Group.legacy(result);
            return legacy != null ? legacy : livingdata;
        }
        var follow = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        if (follow != null) {
            follow.applyModifier(new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier("Random spawn bonus", rand.nextGaussian() * 0.05D, 1));
        }
        setLeftHanded(rand.nextFloat() < 0.05F);
        return livingdata;
    }

    /** Gives the mob random armour by difficulty (verbatim 1.12: a 15% chance scaled by the local difficulty). */
    protected void setEquipmentBasedOnDifficulty(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty) {
        if (rand.nextFloat() >= 0.15F * difficulty.getClampedAdditionalDifficulty()) return;
        int i = rand.nextInt(2);
        float f = mcc$get$world(this) != null && mcc$entity != null && mcc$entity.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 0.1F : 0.25F;
        if (rand.nextFloat() < 0.095F) ++i;
        if (rand.nextFloat() < 0.095F) ++i;
        if (rand.nextFloat() < 0.095F) ++i;
        boolean first = true;
        for (EntityEquipmentSlot slot : new EntityEquipmentSlot[] {EntityEquipmentSlot.FEET, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.CHEST,
                EntityEquipmentSlot.HEAD}) {
            ItemStack worn = getItemStackFromSlot(slot);
            if (!first && rand.nextFloat() < f) break;
            first = false;
            if (worn.isEmpty()) {
                Item item = getArmorByChance(slot, i);
                if (item != null) setItemStackToSlot(slot, new ItemStack(item));
            }
        }
    }

    /** Enchants the equipment by difficulty (the modern enchanting of spawn equipment). */
    protected void setEnchantmentBasedOnDifficulty(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty) {
        Mob m = mcc$mob();
        if (m == null || !(m.level() instanceof net.minecraft.server.level.ServerLevel sl) || difficulty == null) return;
        float f = difficulty.getClampedAdditionalDifficulty();
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            var stack = m.getItemBySlot(slot);
            if (!stack.isEmpty() && rand.nextFloat() < (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR ? 0.5F : 0.25F) * f) {
                var enchanted = net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(m.getRandom(), stack, (int) (5.0F + f * rand.nextInt(18)),
                        sl.registryAccess(), java.util.Optional.empty());
                m.setItemSlot(slot, enchanted);
            }
        }
    }

    /** The legacy data fixers of a mob's NBT; the modern game keeps no legacy fixers. */
    public static void registerFixesMob(legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer fixer, Class<?> cls) {
    }

    protected void doWaterSplashEffect() {
        var m = mcc$entity;
        if (m != null) m.playSound(net.minecraft.sounds.SoundEvents.GENERIC_SPLASH, 0.2F, 1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
    }
}
