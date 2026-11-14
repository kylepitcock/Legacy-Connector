package legacyconnector.v1_7_10.net.minecraft.entity;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public abstract class EntityLivingBase extends Entity {
    /** Minecraft 1.5.2 and older: the entity's texture as a string path (read by {@code RenderLiving}, see Textures152). */
    public String texture = DEFAULT_TEXTURE;
    /** The default of {@link #texture}, compared by identity: a mod that sets its own path makes {@code Render} use it. */
    public static final String DEFAULT_TEXTURE = new String("/mob/char.png");

    /** Minecraft 1.5.2: overridable by mods that switch texture at run time. */
    public String getTexture() {
        return texture;
    }

    /** Minecraft 1.4.7's numeric drop (a mod's reflection finds it by name; the bridge turns an override into {@code getDropItem}). */
    protected int getDropItemId() {
        return 0;
    }

    @Deprecated
    public float rotationYawHead;
    @Deprecated
    public float renderYawOffset;
    @Deprecated
    public int hurtTime;
    /** Phantom field: ticks since a player (or a machine that says so) last hit it; 0 means no experience and rare drops. */
    @Deprecated
    public int recentlyHit;
    @Deprecated
    public int deathTime;
    @Deprecated
    public float prevHealth;
    private final Random mcc$rng = new Random();

    private legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.BaseAttributeMap mcc$attributeMap;
    public int attackTime;
    protected int entityAge;
    protected float randomYawVelocity;
    /** Phantom field: accesses are redirected to {@code mcc$get$moveForward} (the modern forward input). */
    @Deprecated
    public float moveForward;
    private float mcc$local_moveForward;
    /** Phantom field: accesses are redirected to {@code mcc$get$moveStrafing} (the modern strafe input). */
    @Deprecated
    public float moveStrafing;
    private float mcc$local_moveStrafing;

    /**
     * Phantom field: the legacy {@code isJumping} input. Modern mobs reset their jumping flag from their jump control right
     * after the AI step, so a flag the legacy AI sets is handed to that control at the end of the step ({@link #mcc$applyJump}).
     */
    @Deprecated
    public boolean isJumping;
    private boolean mcc$local_isJumping;
    private boolean mcc$jumpPulse;
    /** Phantom field: the horizontal speed in the air (the host's flying speed reads it). */
    @Deprecated
    public float jumpMovementFactor;
    private float mcc$local_jumpMovementFactor = 0.02F;

    public static boolean mcc$get$isJumping(EntityLivingBase e) {
        return e.mcc$local_isJumping;
    }

    public static void mcc$set$isJumping(EntityLivingBase e, boolean v) {
        e.mcc$local_isJumping = v;
        if (v) e.mcc$jumpPulse = true;
    }

    public static float mcc$get$jumpMovementFactor(EntityLivingBase e) {
        return e.mcc$local_jumpMovementFactor;
    }

    public static void mcc$set$jumpMovementFactor(EntityLivingBase e, float v) {
        e.mcc$local_jumpMovementFactor = v;
    }

    /** The speed in the air a host reads for its movement (legacy {@code jumpMovementFactor}). */
    public final float mcc$airSpeed() {
        return mcc$local_jumpMovementFactor;
    }

    /** One jump for the coming movement step (the legacy AI asked for it this tick). */
    public final void mcc$pulseJump() {
        mcc$jumpPulse = true;
    }

    /** Called at the end of the legacy AI step: passes what the legacy AI asked (isJumping) on to the modern jump control. */
    public final void mcc$applyJump() {
        if ((mcc$local_isJumping || mcc$jumpPulse) && mcc$entity instanceof net.minecraft.world.entity.Mob m) m.getJumpControl().jump();
        mcc$jumpPulse = false;
    }

    public static float mcc$get$moveForward(EntityLivingBase e) {
        return e.mcc$entity instanceof LivingEntity l ? l.zza : e.mcc$local_moveForward;
    }

    public static void mcc$set$moveForward(EntityLivingBase e, float v) {
        if (e.mcc$entity instanceof LivingEntity l) l.zza = v;
        else e.mcc$local_moveForward = v;
    }

    public static float mcc$get$moveStrafing(EntityLivingBase e) {
        return e.mcc$entity instanceof LivingEntity l ? l.xxa : e.mcc$local_moveStrafing;
    }

    public static void mcc$set$moveStrafing(EntityLivingBase e, float v) {
        if (e.mcc$entity instanceof LivingEntity l) l.xxa = v;
        else e.mcc$local_moveStrafing = v;
    }

    public EntityLivingBase(World world) {
        super(world);
        applyEntityAttributes();
        mcc$applyLegacyMaxHealth();
        // Not the overridable setHealth: mods mirror health into watcher ids they only add later in their constructor.
        if (mcc$entity instanceof LivingEntity l) l.setHealth(l.getMaxHealth());
    }

    protected EntityLivingBase(LivingEntity modern) {
        super(modern);
    }

    public int getArrowCountInEntity() {
        return mcc$entity == null ? 0 : mcc$living().getArrowCount();
    }

    public void setArrowCountInEntity(int count) {
        if (mcc$entity != null) mcc$living().setArrowCount(count);
    }

    /** Ticks an arm swing takes: 6, shorter with Haste, longer with Mining Fatigue (mods reflect this private method). */
    protected int getArmSwingAnimationEnd() {
        if (mcc$entity == null) return 6;
        var living = mcc$living();
        var haste = living.getEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED);
        if (haste != null) return 6 - (1 + haste.getAmplifier());
        var fatigue = living.getEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
        return fatigue != null ? 6 + (1 + fatigue.getAmplifier()) * 2 : 6;
    }

    public LivingEntity mcc$living() {
        return (LivingEntity) mcc$entity;
    }

    public static float mcc$get$rotationYawHead(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : e.mcc$living().getYHeadRot();
    }

    public static void mcc$set$rotationYawHead(EntityLivingBase e, float v) {
        if (e.mcc$entity != null) e.mcc$living().setYHeadRot(v);
    }

    public static float mcc$get$renderYawOffset(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : e.mcc$living().yBodyRot;
    }

    public static void mcc$set$renderYawOffset(EntityLivingBase e, float v) {
        if (e.mcc$entity != null) e.mcc$living().yBodyRot = v;
    }

    public static int mcc$get$recentlyHit(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.lastHurtByPlayerTime(e.mcc$living());
    }

    public static void mcc$set$recentlyHit(EntityLivingBase e, int v) {
        if (e.mcc$entity != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setLastHurtByPlayerTime(e.mcc$living(), v);
    }

    public static int mcc$get$hurtTime(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : e.mcc$living().hurtTime;
    }

    public static void mcc$set$hurtTime(EntityLivingBase e, int v) {
        if (e.mcc$entity != null) e.mcc$living().hurtTime = v;
    }

    public static int mcc$get$deathTime(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : e.mcc$living().deathTime;
    }

    public static void mcc$set$deathTime(EntityLivingBase e, int v) {
        if (e.mcc$entity != null) e.mcc$living().deathTime = v;
    }

    public static float mcc$get$prevHealth(EntityLivingBase e) {
        return e.mcc$base$getHealth();
    }

    public static void mcc$set$prevHealth(EntityLivingBase e, float v) {
    }

    /** How far the arm swing has got, 0..1 (renderers pose the arm with it). */
    public float getSwingProgress(float partialTicks) {
        return mcc$entity == null ? 0 : mcc$living().getAttackAnim(partialTicks);
    }

    public float getHealth() {
        return mcc$base$getHealth();
    }

    /** The value of {@link #getHealth()}, for the phantom field accessor (mods override the getter to read the field). */
    private float mcc$base$getHealth() {
        return mcc$entity == null ? 0 : mcc$living().getHealth();
    }


    public void setHealth(float health) {
        if (mcc$entity != null) mcc$living().setHealth(health);
    }

    /**
     * 1.5.2 and 1.4.7 mobs declared their maximum health by overriding {@code int getMaxHealth()}; the forward layer bridges such an override to this
     * method. The default is the current maximum (the attribute), so a class without an override changes nothing.
     */
    public float mcc$legacyMaxHealth() {
        return mcc$entity == null ? 20 : mcc$living().getMaxHealth();
    }

    private void mcc$applyLegacyMaxHealth() {
        if (!(mcc$entity instanceof LivingEntity l)) return;
        float legacy;
        try {
            legacy = mcc$legacyMaxHealth();
        } catch (RuntimeException e) {
            return;   // the override reads a field of the mod's constructor that is not set yet
        }
        net.minecraft.world.entity.ai.attributes.AttributeInstance max = l.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (max != null && legacy > 0 && legacy != l.getMaxHealth()) max.setBaseValue(legacy);
    }

    public final float getMaxHealth() {
        return mcc$entity == null ? 20 : mcc$living().getMaxHealth();
    }

    public void heal(float amount) {
        if (mcc$entity != null) mcc$living().heal(amount);
    }

    public float getAbsorptionAmount() {
        return mcc$entity == null ? 0 : mcc$living().getAbsorptionAmount();
    }

    public void setAbsorptionAmount(float amount) {
        if (mcc$entity != null) mcc$living().setAbsorptionAmount(amount);
    }

    public Random getRNG() {
        return rand;
    }

    public boolean isChild() {
        return mcc$entity != null && mcc$living().isBaby();
    }

    public int getAge() {
        return mcc$entity == null ? 0 : mcc$living().getNoActionTime();
    }

    public boolean isEntityUndead() {
        return mcc$entity != null && mcc$living().isInvertedHealAndHarm();
    }

    public boolean isOnLadder() {
        return mcc$entity != null && mcc$living().onClimbable();
    }

    public boolean isPlayerSleeping() {
        return mcc$entity != null && mcc$living().isSleeping();
    }

    public int getTotalArmorValue() {
        return mcc$entity == null ? 0 : mcc$living().getArmorValue();
    }

    /** Ray from the eyes along the look direction, against blocks (null on a miss). */
    public legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition rayTrace(double distance, float partialTicks) {
        var eye = mcc$entity.getEyePosition(partialTicks);
        var look = mcc$entity.getViewVector(partialTicks);
        return ((legacyconnector.v1_7_10.net.minecraft.world.World) legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(mcc$entity.level())).rayTraceBlocks(
                legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(eye.x, eye.y, eye.z),
                legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(eye.x + look.x * distance, eye.y + look.y * distance, eye.z + look.z * distance));
    }

    public ItemStack getHeldItem() {
        return mcc$entity == null ? null : ItemStack.mcc$wrap(mcc$living().getMainHandItem());
    }

    /** 0 = held item, 1-4 = armor from boots to helmet. */
    /** Vanilla's per-tick copy of the equipment (mods diff it against the current items to notice equip changes). */
    public ItemStack[] previousEquipment = new ItemStack[5];

    /** Called after the living-update event handlers ran, where vanilla's own update refreshed the copy. */
    /** The modern stacks previousEquipment was last copied from (an unchanged slot is not wrapped again). */
    private final net.minecraft.world.item.ItemStack[] mcc$previousModern = new net.minecraft.world.item.ItemStack[5];

    /**
     * Called after every LivingUpdateEvent (for every living entity, every tick): only slots whose modern stack changed are wrapped
     * and copied again (wrapping all five slots each tick was 0.75% of the server tick in ATM10).
     */
    public void mcc$syncPreviousEquipment() {
        if (mcc$entity == null) return;
        for (int i = 0; i < 5; i++) {
            net.minecraft.world.item.ItemStack modern = mcc$living().getItemBySlot(mcc$slot(i));
            net.minecraft.world.item.ItemStack last = mcc$previousModern[i];
            if (last != null && net.minecraft.world.item.ItemStack.matches(last, modern)) continue;
            mcc$previousModern[i] = modern.copy();
            ItemStack current = ItemStack.mcc$wrap(modern);
            previousEquipment[i] = current == null ? null : current.copy();
        }
    }

    public ItemStack getEquipmentInSlot(int slot) {
        if (mcc$entity == null) return null;
        return ItemStack.mcc$wrap(mcc$living().getItemBySlot(mcc$slot(slot)));
    }

    public void setCurrentItemOrArmor(int slot, ItemStack stack) {
        if (mcc$entity != null) mcc$living().setItemSlot(mcc$slot(slot), ItemStack.mcc$toModern(stack));
    }

    public ItemStack[] getLastActiveItems() {
        ItemStack[] out = new ItemStack[5];
        for (int i = 0; i < 5; i++) out[i] = getEquipmentInSlot(i);
        return out;
    }

    protected static EquipmentSlot mcc$slot(int legacy) {
        return switch (legacy) {
            case 1 -> EquipmentSlot.FEET;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.CHEST;
            case 4 -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    public void setPositionAndUpdate(double x, double y, double z) {
        if (mcc$entity != null) mcc$entity.teleportTo(x, y, z);
        else setPosition(x, y, z);
    }

    public boolean canEntityBeSeen(Entity other) {
        return mcc$entity != null && other.mcc$entity != null && mcc$living().hasLineOfSight(other.mcc$entity);
    }

    public void swingItem() {
        if (mcc$entity != null) mcc$living().swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public EntityLivingBase getAITarget() {
        if (mcc$entity == null) return null;
        var t = mcc$living().getLastHurtByMob();
        return t == null ? null : (EntityLivingBase) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(t);
    }

    public EntityLivingBase getLastAttacker() {
        if (mcc$entity == null) return null;
        var t = mcc$living().getLastHurtMob();
        return t == null ? null : (EntityLivingBase) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(t);
    }

    public void knockBack(Entity source, float strength, double dx, double dz) {
        if (mcc$entity != null) mcc$living().knockback(0.4F, dx, dz);
    }

    public void renderBrokenItemStack(ItemStack stack) {
        if (mcc$entity != null && stack != null) mcc$living().playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK, 0.8F, 0.8F);
    }

    public boolean isPotionActive(int potionId) {
        return mcc$entity != null && org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge.isActive(mcc$living(), potionId);
    }

    public boolean isPotionActive(legacyconnector.v1_7_10.net.minecraft.potion.Potion potion) {
        return potion != null && isPotionActive(potion.id);
    }

    public legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect getActivePotionEffect(legacyconnector.v1_7_10.net.minecraft.potion.Potion potion) {
        if (mcc$entity == null || potion == null) return null;
        var holder = org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge.effectOrNull(potion.id);
        var modern = holder == null ? null : mcc$living().getEffect(holder);
        return modern == null ? null : legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect.mcc$wrap(modern, mcc$living());
    }

    public java.util.Collection<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> getActivePotionEffects() {
        java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> out = new java.util.ArrayList<>();
        if (mcc$entity == null) return out;
        for (var modern : mcc$living().getActiveEffects()) {
            var e = legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect.mcc$wrap(modern, mcc$living());
            if (e != null) out.add(e);
        }
        return out;
    }

    public void removePotionEffect(int potionId) {
        if (mcc$entity != null) org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge.remove(mcc$living(), potionId);
    }

    public void clearActivePotions() {
        if (mcc$entity != null) mcc$living().removeAllEffects();
    }

    /** Experience dropped on death; reflected on by mods that boost it (EnderCore). */
    protected int getExperiencePoints(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        return 0;
    }

    public void addPotionEffect(legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect effect) {
        if (mcc$entity != null && effect != null) mcc$living().addEffect(effect.mcc$modern());
    }

    public boolean attackEntityAsMob(Entity target) {
        return mcc$entity != null && target.mcc$entity != null && mcc$living().doHurtTarget(target.mcc$entity);
    }

    private legacyconnector.v1_7_10.net.minecraft.util.CombatTracker mcc$combatTracker;

    public legacyconnector.v1_7_10.net.minecraft.util.CombatTracker getCombatTracker() {
        if (mcc$combatTracker == null) mcc$combatTracker = new legacyconnector.v1_7_10.net.minecraft.util.CombatTracker(this);
        return mcc$combatTracker;
    }

    /**
     * Takes damage that got past invulnerability: armour, resistance and absorption reduce it, the rest comes off health
     * (the modern actuallyHurt, which hosts route here so mods' overrides apply). Without a host only absorption is applied.
     */
    protected void damageEntity(DamageSource source, float amount) {
        if (mcc$entity == null || source == null) return;
        var modern = source.mcc$modern(mcc$entity.level());
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h) {
            h.mcc$superActuallyHurt(modern, amount);
            return;
        }
        if (isEntityInvulnerable()) return;
        LivingEntity l = mcc$living();
        float absorbed = Math.min(amount, l.getAbsorptionAmount());
        l.setAbsorptionAmount(l.getAbsorptionAmount() - absorbed);
        float rest = amount - absorbed;
        if (rest > 0.0F) {
            l.getCombatTracker().recordDamage(modern, rest);
            l.setHealth(l.getHealth() - rest);
        }
    }

    public final void mcc$damageEntity(DamageSource source, float amount) {
        damageEntity(source, amount);
    }

    public void onDeath(DamageSource source) {
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h) {
            h.mcc$superDie(source.mcc$modern(mcc$entity.level()));
        }
    }

    public void onLivingUpdate() {
        if (attackTime > 0) --attackTime;
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h) h.mcc$superAiStep();
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(7, Integer.valueOf(0));
        dataWatcher.addObject(8, Byte.valueOf((byte) 0));
        dataWatcher.addObject(9, Byte.valueOf((byte) 0));
        dataWatcher.addObject(6, Float.valueOf(1.0F));
    }

    // ------------------------------------------------------------------ attributes

    protected void applyEntityAttributes() {
        getAttributeMap().registerAttribute(SharedMonsterAttributes.maxHealth);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.knockbackResistance);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.movementSpeed);
    }

    public legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.BaseAttributeMap getAttributeMap() {
        if (mcc$attributeMap == null) {
            mcc$attributeMap = new legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.ServersideAttributeMap();
            mcc$attributeMap.mcc$bind(() -> mcc$entity instanceof LivingEntity l ? l : null);
        }
        return mcc$attributeMap;
    }

    public legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttributeInstance getEntityAttribute(
            legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttribute attribute) {
        return getAttributeMap().getAttributeInstance(attribute);
    }

    public EnumCreatureAttribute getCreatureAttribute() {
        if (mcc$entity instanceof LivingEntity l && !mcc$hosted()) {
            if (l.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) return EnumCreatureAttribute.UNDEAD;
            if (l.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD)) return EnumCreatureAttribute.ARTHROPOD;
        }
        return EnumCreatureAttribute.UNDEFINED;
    }

    // ------------------------------------------------------------------ movement

    /** Vanilla movement for one tick; legacy flying/swimming mobs override this. */
    public void moveEntityWithHeading(float strafe, float forward) {
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h) h.mcc$superTravel(strafe, forward);
    }

    public void moveFlying(float strafe, float forward, float friction) {
        if (mcc$entity != null) mcc$entity.moveRelative(friction, new net.minecraft.world.phys.Vec3(strafe, 0, forward));
    }

    /**
     * The speed walking accelerates with. Mobs on the old AI (isAIEnabled() false: HBM's glyphids) walked at 0.1 whatever their
     * movement speed attribute, which only set how hard they pushed forward; the new AI's move helper sets it per move.
     */
    public float getAIMoveSpeed() {
        if (!isAIEnabled()) return 0.1F;
        return mcc$entity instanceof LivingEntity l ? l.getSpeed() : 0.1F;
    }

    public void setAIMoveSpeed(float speed) {
        if (mcc$entity instanceof LivingEntity l) l.setSpeed(speed);
    }

    public void setJumping(boolean jumping) {
        mcc$local_isJumping = jumping;
        if (jumping) mcc$jumpPulse = true;
        if (mcc$entity instanceof LivingEntity l && !(l instanceof net.minecraft.world.entity.Mob)) l.setJumping(jumping);
    }

    protected void jump() {
        if (mcc$entity instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyMobHost m) m.mcc$jump();
    }

    public float getRotationYawHead() {
        return mcc$get$rotationYawHead(this);
    }

    public void setRotationYawHead(float yaw) {
        mcc$set$rotationYawHead(this, yaw);
    }

    public boolean isAIEnabled() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return mcc$hosted() ? isEntityAlive() : super.canBeCollidedWith();
    }

    @Override
    public boolean canBePushed() {
        return mcc$hosted() ? isEntityAlive() : super.canBePushed();
    }

    // ------------------------------------------------------------------ combat

    public void setRevengeTarget(EntityLivingBase target) {
        if (mcc$entity instanceof LivingEntity l) l.setLastHurtByMob(target == null ? null : target.mcc$living());
    }

    public int getRevengeTimer() {
        return mcc$entity instanceof LivingEntity l ? l.getLastHurtByMobTimestamp() : 0;
    }

    public void setLastAttacker(Entity target) {
        if (mcc$entity instanceof LivingEntity l && target != null && target.mcc$entity instanceof LivingEntity t) l.setLastHurtMob(t);
    }

    /** The 1.4.7 name of {@link #setLastAttacker}. */
    public void setLastAttackingEntity(Entity target) {
        setLastAttacker(target);
    }

    /** 1.4.7: the factor Speed and Slowness effects put on the walking speed (+20% per Speed level, -15% per Slowness level). */
    public float getSpeedModifier() {
        float factor = 1.0F;
        legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect speed = getActivePotionEffect(legacyconnector.v1_7_10.net.minecraft.potion.Potion.moveSpeed);
        if (speed != null) factor *= 1.0F + 0.2F * (speed.getAmplifier() + 1);
        legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect slow = getActivePotionEffect(legacyconnector.v1_7_10.net.minecraft.potion.Potion.moveSlowdown);
        if (slow != null) factor *= 1.0F - 0.15F * (slow.getAmplifier() + 1);
        return factor;
    }

    /** 1.4.7: the URL the entity's skin is downloaded from (null for the texture its renderer names). */
    public String skinUrl;

    /** The entity's team on its level's scoreboard (players and mobs a team lists by UUID). */
    public legacyconnector.v1_7_10.net.minecraft.scoreboard.Team getTeam() {
        if (mcc$entity == null || !(mcc$entity.getTeam() instanceof net.minecraft.world.scores.PlayerTeam team)) return null;
        return legacyconnector.v1_7_10.net.minecraft.scoreboard.Scoreboard.mcc$wrap(team.getScoreboard()).getTeam(team.getName());
    }

    public boolean isOnSameTeam(EntityLivingBase other) {
        return mcc$entity != null && other != null && other.mcc$entity != null && mcc$entity.isAlliedTo(other.mcc$entity);
    }

    public boolean canBreatheUnderwater() {
        // a hosted entity answers with the legacy default: the host asks this method itself
        return !mcc$hosted() && mcc$entity instanceof LivingEntity l && l.canBreatheUnderwater();
    }

    // ------------------------------------------------------------------ death, drops, sounds (bridged for the host)

    protected void dropFewItems(boolean recentlyHit, int looting) {
    }

    protected void dropEquipment(boolean recentlyHit, int looting) {
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h) h.mcc$superDropEquipment();
    }

    protected void dropRareDrop(int looting) {
    }

    public final void mcc$dropLoot(boolean recentlyHit, int looting) {
        dropFewItems(recentlyHit, looting);
        dropEquipment(recentlyHit, looting);
        if (recentlyHit && getRNG().nextFloat() < 0.025F + looting * 0.01F) dropRareDrop(looting);
    }

    protected String getHurtSound() {
        return "game.neutral.hurt";
    }

    protected String getDeathSound() {
        return "game.neutral.die";
    }

    protected float getSoundVolume() {
        return 1.0F;
    }

    protected float getSoundPitch() {
        float base = isChild() ? 1.5F : 1.0F;
        return (getRNG().nextFloat() - getRNG().nextFloat()) * 0.2F + base;
    }

    public final String mcc$hurtSound() {
        return getHurtSound();
    }

    public final String mcc$deathSound() {
        return getDeathSound();
    }

    public final float mcc$soundVolume() {
        return getSoundVolume();
    }

    public final float mcc$soundPitch() {
        return getSoundPitch();
    }

    public boolean isClientWorld() {
        return mcc$entity != null && mcc$entity.level().isClientSide();
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getItemIcon(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int pass) {
        return stack.getIconIndex();
    }

    public EntityLivingBase func_94060_bK() {
        return null;
    }

    /** A dead mob cannot move or act. */
    protected boolean isMovementBlocked() {
        return getHealth() <= 0.0F;
    }

    protected void updateArmSwingProgress() {
    }

    // ------------------------------------------------------------------ collisions, pickups, teams

    /** Pushes away an entity this one touches. Hosts route the modern push here so mods' overrides run. */
    protected void collideWithEntity(Entity other) {
        if (other == null) return;
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h && other.mcc$entity != null) {
            h.mcc$superDoPush(other.mcc$entity);
            return;
        }
        other.applyEntityCollision(this);
    }

    public final void mcc$collideWithEntity(Entity other) {
        collideWithEntity(other);
    }

    /** Shows {@code entity} (an item or orb) flying into this entity as it is picked up. */
    public void onItemPickup(Entity entity, int count) {
        if (entity != null && entity.mcc$entity != null && mcc$entity instanceof LivingEntity l && !l.level().isClientSide()) l.take(entity.mcc$entity, count);
    }

    /** Credits {@code score} points for killing {@code killed}; only players keep a score. */
    public void addToPlayerScore(Entity killed, int score) {
    }

    /** The tick the entity last hit another one (see {@link #setLastAttacker}). */
    public int getLastAttackerTime() {
        return mcc$entity instanceof LivingEntity l ? l.getLastHurtMobTimestamp() : 0;
    }

    public boolean isOnTeam(legacyconnector.v1_7_10.net.minecraft.scoreboard.Team team) {
        var mine = getTeam();
        return mine != null && mine.isSameTeam(team);
    }

    /** Whether the name tag is always drawn (mobs: when set with a name tag; never for others). */
    public boolean getAlwaysRenderNameTagForRender() {
        return false;
    }

    protected boolean isPlayer() {
        return false;
    }

    // ------------------------------------------------------------------ potions

    /** Forge: removes every effect {@code stack} cures (milk cures all vanilla effects). */
    public void curePotionEffects(ItemStack stack) {
        if (!(mcc$entity instanceof LivingEntity l) || l.level().isClientSide()) return;
        var cure = legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect.mcc$cureFor(stack);
        if (cure != null) l.removeEffectsCuredBy(cure);
    }

    /** Whether the effect can be applied (the undead are immune to regeneration and poison). */
    public boolean isPotionApplicable(legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect effect) {
        if (effect == null) return false;
        if (mcc$entity instanceof LivingEntity l && !mcc$hosted()) {
            var holder = org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge.effectOrNull(effect.getPotionID());
            return holder == null || l.canBeAffected(effect.mcc$modern());
        }
        if (getCreatureAttribute() == EnumCreatureAttribute.UNDEAD) {
            int id = effect.getPotionID();
            return id != legacyconnector.v1_7_10.net.minecraft.potion.Potion.regeneration.id && id != legacyconnector.v1_7_10.net.minecraft.potion.Potion.poison.id;
        }
        return true;
    }

    /**
     * Damage after the Resistance effect and protection enchantments (the modern getDamageAfterMagicAbsorb). Hosts route the modern
     * calculation here so mods' overrides run.
     */
    protected float applyPotionDamageCalculations(DamageSource source, float amount) {
        if (!(mcc$entity instanceof LivingEntity l) || source == null) return amount;
        var modern = source.mcc$modern(l.level());
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h) return h.mcc$superMagicAbsorb(modern, amount);
        return org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.damageAfterMagicAbsorb(l, modern, amount);
    }

    public final float mcc$applyPotionDamageCalculations(DamageSource source, float amount) {
        return applyPotionDamageCalculations(source, amount);
    }

    /** Air left after one tick under water ({@code air - 1}, or {@code air} when Respiration saves the breath). */
    protected int decreaseAirSupply(int air) {
        if (!(mcc$entity instanceof LivingEntity l)) return air - 1;
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h) return h.mcc$superDecreaseAirSupply(air);
        return org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.decreaseAirSupply(l, air);
    }

    public final int mcc$decreaseAirSupply(int air) {
        return decreaseAirSupply(air);
    }

    /**
     * One tick of lying dead: after 20 ticks the body disappears in a puff of smoke and drops its experience (the modern tickDeath).
     * Hosts route the modern tickDeath here so mods' death animations run.
     */
    protected void onDeathUpdate() {
        if (mcc$host() instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyLivingHost h) h.mcc$superTickDeath();
        else if (mcc$entity instanceof LivingEntity l) org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.tickDeath(l);
    }

    public final void mcc$onDeathUpdate() {
        onDeathUpdate();
    }
}
