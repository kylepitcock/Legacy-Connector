package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.potion.Potion;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.LiveStacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.LivingHost1122;

/**
 * 1.12.2 living entity: health, held items and equipment, potion effects, attributes, combat and the living hooks a host runs
 * (onLivingUpdate, damageEntity, onDeath, travel, ...). Held and equipped stacks are live ({@link LiveStacks1122}): a mod that shrinks
 * the stack in a hand changes the hand.
 */
public abstract class EntityLivingBase extends Entity {
    /** Vanilla's data keys of living entities: their values are the modern entity's (see {@link org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122}). */
    public static final legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<Byte> HAND_STATES = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122.of(6, legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers.BYTE, EntityLivingBase.class, (byte) 0, e -> {
        var l = e.mcc$living();
        if (l == null || !l.isUsingItem()) return (byte) 0;
        return (byte) (l.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND ? 3 : 1);
    }, null);
    public static final legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<Float> HEALTH = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122.of(7, legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers.FLOAT, EntityLivingBase.class, 0.0F, e -> e.mcc$living().getHealth(),
            (e, v) -> e.mcc$living().setHealth(v));
    public static final legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<Integer> POTION_EFFECTS = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122.of(8, legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers.VARINT, EntityLivingBase.class, 0,
            e -> net.minecraft.world.item.alchemy.PotionContents.getColorOptional(e.mcc$living().getActiveEffects()).orElse(0), null);
    public static final legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<Boolean> HIDE_PARTICLES = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122.of(9, legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers.BOOLEAN, EntityLivingBase.class, false,
            e -> !e.mcc$living().getActiveEffects().isEmpty() && net.minecraft.world.entity.LivingEntity.areAllEffectsAmbient(e.mcc$living().getActiveEffects()),
            null);
    public static final legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<Integer> ARROW_COUNT_IN_ENTITY = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122.of(10, legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers.VARINT, EntityLivingBase.class, 0, e -> e.mcc$living().getArrowCount(),
            (e, v) -> e.mcc$living().setArrowCount(v));

    @Deprecated
    public float rotationYawHead;
    private float mcc$local_rotationYawHead;
    @Deprecated
    public float prevRotationYawHead;
    private float mcc$local_prevRotationYawHead;
    @Deprecated
    public float renderYawOffset;
    private float mcc$local_renderYawOffset;
    @Deprecated
    public float prevRenderYawOffset;
    private float mcc$local_prevRenderYawOffset;
    @Deprecated
    public int hurtTime;
    private int mcc$local_hurtTime;
    @Deprecated
    public int maxHurtTime;
    @Deprecated
    public int deathTime;
    private int mcc$local_deathTime;
    @Deprecated
    public float moveForward;
    private float mcc$local_moveForward;
    @Deprecated
    public float moveStrafing;
    private float mcc$local_moveStrafing;
    @Deprecated
    public float moveVertical;
    private float mcc$local_moveVertical;
    @Deprecated
    public boolean isSwingInProgress;
    private boolean mcc$local_isSwingInProgress;
    @Deprecated
    public float swingProgress;
    private float mcc$local_swingProgress;
    @Deprecated
    public float prevSwingProgress;
    private float mcc$local_prevSwingProgress;
    @Deprecated
    public int swingProgressInt;
    private int mcc$local_swingProgressInt;
    @Deprecated
    public EnumHand swingingHand;
    @Deprecated
    public float limbSwing;
    private float mcc$local_limbSwing;
    @Deprecated
    public float limbSwingAmount;
    private float mcc$local_limbSwingAmount;
    @Deprecated
    public float prevLimbSwingAmount;
    private float mcc$local_prevLimbSwingAmount;
    @Deprecated
    public boolean isJumping;
    private boolean mcc$local_isJumping;
    @Deprecated
    public float jumpMovementFactor;
    private float mcc$local_jumpMovementFactor = 0.02F;
    @Deprecated
    public int recentlyHit;
    @Deprecated
    public int maxHurtResistantTime;
    @Deprecated
    public float attackedAtYaw;
    @Deprecated
    public int arrowHitTimer;
    @Deprecated
    public float cameraPitch;
    @Deprecated
    public float prevCameraPitch;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer attackingPlayer;
    @Deprecated
    public boolean dead;
    private boolean mcc$local_dead;
    @Deprecated
    public float onGroundSpeedFactor;
    private float mcc$local_onGroundSpeedFactor;
    @Deprecated
    public float prevOnGroundSpeedFactor;
    private float mcc$local_prevOnGroundSpeedFactor;

    /** Ticks since the entity last took damage or acted (EntityLiving's despawn timer). */
    protected int idleTime;
    protected int scoreValue;
    protected float lastDamage;
    protected ItemStack activeItemStack = ItemStack.EMPTY;
    protected int activeItemStackUseCount;

    private legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AbstractAttributeMap mcc$attributeMap;
    private DamageSource mcc$lastDamageSource;

    public EntityLivingBase(World world) {
        super(world);
        try {
            applyEntityAttributes();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("applyEntityAttributes of {} failed: {}", getClass().getName(), e.toString());
        }
        // the base value, not the overridable setHealth: mods mirror health into data keys they only add later in their constructor
        if (mcc$entity instanceof LivingEntity l && mcc$hosted()) l.setHealth(l.getMaxHealth());
        this.preventEntitySpawning = true;
    }

    protected EntityLivingBase(LivingEntity modern) {
        super(modern);
    }

    public final LivingEntity mcc$living() {
        return mcc$entity instanceof LivingEntity l ? l : null;
    }

    private LivingHost1122 mcc$livingHost() {
        return mcc$host() instanceof LivingHost1122 h ? h : null;
    }

    // ------------------------------------------------------------------ phantom fields

    /** dead: the death was handled (the modern flag). */
    public static boolean mcc$get$dead(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.dead(l) : e.mcc$local_dead;
    }

    public static void mcc$set$dead(EntityLivingBase e, boolean v) {
        var l = e.mcc$living();
        if (l != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setDead(l, v);
        else e.mcc$local_dead = v;
    }

    /** onGroundSpeedFactor / prevOnGroundSpeedFactor: the modern animation step. */
    public static float mcc$get$onGroundSpeedFactor(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.animStep(l, false) : e.mcc$local_onGroundSpeedFactor;
    }

    public static void mcc$set$onGroundSpeedFactor(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setAnimStep(l, false, v);
        else e.mcc$local_onGroundSpeedFactor = v;
    }

    public static float mcc$get$prevOnGroundSpeedFactor(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.animStep(l, true) : e.mcc$local_prevOnGroundSpeedFactor;
    }

    public static void mcc$set$prevOnGroundSpeedFactor(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setAnimStep(l, true, v);
        else e.mcc$local_prevOnGroundSpeedFactor = v;
    }

    public static float mcc$get$rotationYawHead(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.yHeadRot : e.mcc$local_rotationYawHead;
    }

    public static void mcc$set$rotationYawHead(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.yHeadRot = v;
        else e.mcc$local_rotationYawHead = v;
    }

    public static float mcc$get$prevRotationYawHead(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.yHeadRotO : e.mcc$local_prevRotationYawHead;
    }

    public static void mcc$set$prevRotationYawHead(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.yHeadRotO = v;
        else e.mcc$local_prevRotationYawHead = v;
    }

    public static float mcc$get$renderYawOffset(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.yBodyRot : e.mcc$local_renderYawOffset;
    }

    public static void mcc$set$renderYawOffset(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.yBodyRot = v;
        else e.mcc$local_renderYawOffset = v;
    }

    public static float mcc$get$prevRenderYawOffset(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.yBodyRotO : e.mcc$local_prevRenderYawOffset;
    }

    public static void mcc$set$prevRenderYawOffset(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.yBodyRotO = v;
        else e.mcc$local_prevRenderYawOffset = v;
    }

    public static int mcc$get$hurtTime(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.hurtTime : e.mcc$local_hurtTime;
    }

    public static void mcc$set$hurtTime(EntityLivingBase e, int v) {
        var l = e.mcc$living();
        if (l != null) l.hurtTime = v;
        else e.mcc$local_hurtTime = v;
    }

    public static int mcc$get$maxHurtTime(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.hurtDuration : 10;
    }

    public static void mcc$set$maxHurtTime(EntityLivingBase e, int v) {
        var l = e.mcc$living();
        if (l != null) l.hurtDuration = v;
    }

    public static int mcc$get$deathTime(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.deathTime : e.mcc$local_deathTime;
    }

    public static void mcc$set$deathTime(EntityLivingBase e, int v) {
        var l = e.mcc$living();
        if (l != null) l.deathTime = v;
        else e.mcc$local_deathTime = v;
    }

    public static float mcc$get$moveForward(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.zza : e.mcc$local_moveForward;
    }

    public static void mcc$set$moveForward(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.zza = v;
        else e.mcc$local_moveForward = v;
    }

    public static float mcc$get$moveStrafing(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.xxa : e.mcc$local_moveStrafing;
    }

    public static void mcc$set$moveStrafing(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.xxa = v;
        else e.mcc$local_moveStrafing = v;
    }

    public static float mcc$get$moveVertical(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.yya : e.mcc$local_moveVertical;
    }

    public static void mcc$set$moveVertical(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.yya = v;
        else e.mcc$local_moveVertical = v;
    }

    public static boolean mcc$get$isSwingInProgress(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.swinging : e.mcc$local_isSwingInProgress;
    }

    public static void mcc$set$isSwingInProgress(EntityLivingBase e, boolean v) {
        var l = e.mcc$living();
        if (l != null) l.swinging = v;
        else e.mcc$local_isSwingInProgress = v;
    }

    public static float mcc$get$swingProgress(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.attackAnim : e.mcc$local_swingProgress;
    }

    public static void mcc$set$swingProgress(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.attackAnim = v;
        else e.mcc$local_swingProgress = v;
    }

    public static float mcc$get$prevSwingProgress(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.oAttackAnim : e.mcc$local_prevSwingProgress;
    }

    public static void mcc$set$prevSwingProgress(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.oAttackAnim = v;
        else e.mcc$local_prevSwingProgress = v;
    }

    public static int mcc$get$swingProgressInt(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.swingTime : e.mcc$local_swingProgressInt;
    }

    public static void mcc$set$swingProgressInt(EntityLivingBase e, int v) {
        var l = e.mcc$living();
        if (l != null) l.swingTime = v;
        else e.mcc$local_swingProgressInt = v;
    }

    public static EnumHand mcc$get$swingingHand(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null && l.swingingArm == net.minecraft.world.InteractionHand.OFF_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    public static void mcc$set$swingingHand(EntityLivingBase e, EnumHand v) {
        var l = e.mcc$living();
        if (l != null) l.swingingArm = mcc$hand(v);
    }

    public static float mcc$get$limbSwing(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.walkAnimation.position() : e.mcc$local_limbSwing;
    }

    public static void mcc$set$limbSwing(EntityLivingBase e, float v) {
        e.mcc$local_limbSwing = v;
    }

    public static float mcc$get$limbSwingAmount(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.walkAnimation.speed() : e.mcc$local_limbSwingAmount;
    }

    public static void mcc$set$limbSwingAmount(EntityLivingBase e, float v) {
        var l = e.mcc$living();
        if (l != null) l.walkAnimation.setSpeed(v);
        else e.mcc$local_limbSwingAmount = v;
    }

    public static float mcc$get$prevLimbSwingAmount(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? l.walkAnimation.speed(0.0F) : e.mcc$local_prevLimbSwingAmount;
    }

    public static void mcc$set$prevLimbSwingAmount(EntityLivingBase e, float v) {
        e.mcc$local_prevLimbSwingAmount = v;
    }

    public static boolean mcc$get$isJumping(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.jumping(l) : e.mcc$local_isJumping;
    }

    public static void mcc$set$isJumping(EntityLivingBase e, boolean v) {
        var l = e.mcc$living();
        if (l != null) l.setJumping(v);
        else e.mcc$local_isJumping = v;
    }

    public static float mcc$get$jumpMovementFactor(EntityLivingBase e) {
        return e.mcc$local_jumpMovementFactor;
    }

    public static void mcc$set$jumpMovementFactor(EntityLivingBase e, float v) {
        e.mcc$local_jumpMovementFactor = v;
    }

    public static int mcc$get$recentlyHit(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.lastHurtByPlayerTime(l) : 0;
    }

    public static void mcc$set$recentlyHit(EntityLivingBase e, int v) {
        var l = e.mcc$living();
        if (l != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setLastHurtByPlayerTime(l, v);
    }

    public static int mcc$get$maxHurtResistantTime(EntityLivingBase e) {
        return 20;
    }

    public static void mcc$set$maxHurtResistantTime(EntityLivingBase e, int v) {
    }

    public static legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer mcc$get$attackingPlayer(EntityLivingBase e) {
        var l = e.mcc$living();
        return l != null && EntityBridge1122.wrap(l.getLastHurtByMob()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer p
                && org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.lastHurtByPlayerTime(l) > 0 ? p : null;
    }

    public static void mcc$set$attackingPlayer(EntityLivingBase e, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer v) {
        var l = e.mcc$living();
        if (l != null && v != null && v.mcc$entity instanceof net.minecraft.world.entity.player.Player p) {
            l.setLastHurtByPlayer(p);
        }
    }

    // ------------------------------------------------------------------ hooks a host runs (the base hands them back to vanilla)

    @Override
    public void onUpdate() {
        super.onUpdate();
    }

    /** One tick of the living update: movement input, AI (EntityLiving), travel, pushing. */
    public void onLivingUpdate() {
        var h = mcc$livingHost();
        if (h != null) h.mcc$superAiStep();
    }

    /** EntityLiving runs the AI here; the base does nothing (players). */
    protected void updateEntityActionState() {
    }

    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source != null) mcc$lastDamageSource = source;
        return super.attackEntityFrom(source, amount);
    }

    /**
     * Takes damage that got past invulnerability: armour, resistance and absorption reduce it, the rest comes off health (the modern
     * actuallyHurt, which hosts route here so mods' overrides apply). Without a host only absorption is applied.
     */
    protected void damageEntity(DamageSource source, float amount) {
        LivingEntity l = mcc$living();
        if (l == null || source == null) return;
        var modern = source.mcc$modern(l.level());
        var h = mcc$livingHost();
        if (h != null) {
            h.mcc$superActuallyHurt(modern, amount);
            return;
        }
        if (isEntityInvulnerable(source)) return;
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

    public void onDeath(DamageSource cause) {
        var h = mcc$livingHost();
        if (h != null && cause != null) h.mcc$superDie(cause.mcc$modern(mcc$entity.level()));
    }

    /** The loot of a death: the loot table (when the mod gives one) or dropFewItems, then the equipment. */
    protected void dropLoot(boolean wasRecentlyHit, int lootingModifier, DamageSource source) {
        dropFewItems(wasRecentlyHit, lootingModifier);
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
    }

    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        var h = mcc$livingHost();
        if (h != null) h.mcc$superDropEquipment();
    }

    public final void mcc$dropLoot(boolean recentlyHit, int looting, DamageSource source) {
        dropLoot(recentlyHit, looting, source);
        dropEquipment(recentlyHit, looting);
    }

    protected boolean canDropLoot() {
        return !isChild();
    }

    protected int getExperiencePoints(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return 0;
    }

    public final int mcc$experience() {
        return getExperiencePoints(null);
    }

    /** One tick of lying dead (the modern tickDeath). */
    protected void onDeathUpdate() {
        var h = mcc$livingHost();
        if (h != null) h.mcc$superTickDeath();
        else if (mcc$living() != null) org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.tickDeath(mcc$living());
    }

    public final void mcc$onDeathUpdate() {
        onDeathUpdate();
    }

    /** Damage after armour (the modern getDamageAfterArmorAbsorb). */
    protected float applyArmorCalculations(DamageSource source, float amount) {
        LivingEntity l = mcc$living();
        if (l == null || source == null) return amount;
        var modern = source.mcc$modern(l.level());
        var h = mcc$livingHost();
        if (h != null) return h.mcc$superArmorAbsorb(modern, amount);
        return org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.damageAfterArmorAbsorb(l, modern, amount);
    }

    public final float mcc$applyArmorCalculations(DamageSource source, float amount) {
        return applyArmorCalculations(source, amount);
    }

    /** Damage after Resistance and protection enchantments (the modern getDamageAfterMagicAbsorb). */
    protected float applyPotionDamageCalculations(DamageSource source, float amount) {
        LivingEntity l = mcc$living();
        if (l == null || source == null) return amount;
        var modern = source.mcc$modern(l.level());
        var h = mcc$livingHost();
        if (h != null) return h.mcc$superMagicAbsorb(modern, amount);
        return org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.damageAfterMagicAbsorb(l, modern, amount);
    }

    public final float mcc$applyPotionDamageCalculations(DamageSource source, float amount) {
        return applyPotionDamageCalculations(source, amount);
    }

    protected int decreaseAirSupply(int air) {
        LivingEntity l = mcc$living();
        if (l == null) return air - 1;
        var h = mcc$livingHost();
        if (h != null) return h.mcc$superDecreaseAirSupply(air);
        return org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.decreaseAirSupply(l, air);
    }

    public final int mcc$decreaseAirSupply(int air) {
        return decreaseAirSupply(air);
    }

    /** One tick of movement from the given input (the modern travel). */
    public void travel(float strafe, float vertical, float forward) {
        var h = mcc$livingHost();
        if (h != null) h.mcc$superTravel(strafe, vertical, forward);
        else if (mcc$entity instanceof LivingEntity l) l.travel(new net.minecraft.world.phys.Vec3(strafe, vertical, forward));
    }

    protected void jump() {
        var h = mcc$livingHost();
        if (h != null) h.mcc$superJump();
        else if (mcc$entity instanceof LivingEntity l) l.jumpFromGround();
    }

    public final void mcc$jump() {
        jump();
    }

    protected float getJumpUpwardsMotion() {
        return 0.42F;
    }

    protected void handleJumpWater() {
        mcc$set$motionY(this, mcc$get$motionY(this) + 0.03999999910593033D);
    }

    protected void handleJumpLava() {
        mcc$set$motionY(this, mcc$get$motionY(this) + 0.03999999910593033D);
    }

    protected float getWaterSlowDown() {
        return 0.8F;
    }

    public boolean attackEntityAsMob(Entity target) {
        LivingEntity l = mcc$living();
        if (l == null || target == null || target.mcc$entity == null) return false;
        var h = mcc$livingHost();
        if (h != null) return h.mcc$superDoHurtTarget(target.mcc$entity);
        return l.doHurtTarget(target.mcc$entity);
    }

    /** Pushes away an entity this one touches. Hosts route the modern push here when a mod changes it. */
    protected void collideWithEntity(Entity other) {
        if (other == null) return;
        var h = mcc$livingHost();
        if (h != null && other.mcc$entity != null) {
            h.mcc$superDoPush(other.mcc$entity);
            return;
        }
        other.applyEntityCollision(this);
    }

    public final void mcc$collideWithEntity(Entity other) {
        collideWithEntity(other);
    }

    protected void collideWithNearbyEntities() {
    }

    @Override
    protected void outOfWorld() {
        var host = mcc$host();
        if (host != null) host.mcc$superOutOfWorld();
        else if (mcc$living() != null) mcc$living().hurt(mcc$living().damageSources().fellOutOfWorld(), 4.0F);
    }

    // ------------------------------------------------------------------ attributes

    protected void applyEntityAttributes() {
        var map = getAttributeMap();
        map.registerAttribute(SharedMonsterAttributes.MAX_HEALTH);
        map.registerAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        map.registerAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        map.registerAttribute(SharedMonsterAttributes.ARMOR);
        map.registerAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AbstractAttributeMap getAttributeMap() {
        if (mcc$attributeMap == null) {
            var map = new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeMap();
            map.mcc$bind(this::mcc$living);
            mcc$attributeMap = map;
        }
        return mcc$attributeMap;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttributeInstance getEntityAttribute(
            legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute attribute) {
        return getAttributeMap().getAttributeInstance(attribute);
    }

    public EnumCreatureAttribute getCreatureAttribute() {
        LivingEntity l = mcc$living();
        if (l != null && !mcc$hosted()) {
            if (l.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) return EnumCreatureAttribute.UNDEAD;
            if (l.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD)) return EnumCreatureAttribute.ARTHROPOD;
            if (l.getType().is(net.minecraft.tags.EntityTypeTags.ILLAGER)) return EnumCreatureAttribute.ILLAGER;
        }
        return EnumCreatureAttribute.UNDEFINED;
    }

    // ------------------------------------------------------------------ health

    public float getHealth() {
        var l = mcc$living();
        return l != null ? l.getHealth() : 0;
    }

    public final float getMaxHealth() {
        var l = mcc$living();
        return l != null ? l.getMaxHealth() : 20.0F;
    }

    public void setHealth(float health) {
        var l = mcc$living();
        if (l != null) l.setHealth(health);
    }

    public void heal(float amount) {
        var l = mcc$living();
        if (l != null) l.heal(amount);
    }

    public float getAbsorptionAmount() {
        var l = mcc$living();
        return l != null ? l.getAbsorptionAmount() : 0;
    }

    public void setAbsorptionAmount(float amount) {
        var l = mcc$living();
        if (l != null) l.setAbsorptionAmount(amount);
    }

    public int getTotalArmorValue() {
        var l = mcc$living();
        return l != null ? l.getArmorValue() : 0;
    }

    public boolean isChild() {
        var l = mcc$living();
        return l != null && l.isBaby();
    }

    public boolean isEntityUndead() {
        if (mcc$hosted()) return getCreatureAttribute() == EnumCreatureAttribute.UNDEAD;
        var l = mcc$living();
        return l != null && l.isInvertedHealAndHarm();
    }

    public boolean isOnLadder() {
        var l = mcc$living();
        return l != null && l.onClimbable();
    }

    public boolean isElytraFlying() {
        var l = mcc$living();
        return l != null && l.isFallFlying();
    }

    public int getTicksElytraFlying() {
        var l = mcc$living();
        return l != null ? l.getFallFlyingTicks() : 0;
    }

    public boolean isPlayerSleeping() {
        var l = mcc$living();
        return l != null && l.isSleeping();
    }

    @Override
    public boolean isEntityAlive() {
        var l = mcc$living();
        return l != null ? l.isAlive() : super.isEntityAlive();
    }

    public boolean canBreatheUnderwater() {
        // a hosted entity answers with the legacy default: the host asks this method itself
        return !mcc$hosted() && mcc$entity instanceof LivingEntity l && l.canBreatheUnderwater();
    }

    public java.util.Random getRNG() {
        return rand;
    }

    public int getIdleTime() {
        var l = mcc$living();
        return l != null ? l.getNoActionTime() : idleTime;
    }

    public boolean isServerWorld() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    protected boolean isPlayer() {
        return false;
    }

    protected boolean isMovementBlocked() {
        return getHealth() <= 0.0F;
    }

    public boolean canBeHitWithPotion() {
        return true;
    }

    public boolean attackable() {
        var l = mcc$living();
        return l == null || l.attackable();
    }

    public void knockBack(Entity source, float strength, double xRatio, double zRatio) {
        var l = mcc$living();
        if (l != null) l.knockback(strength, xRatio, zRatio);
    }

    public boolean canEntityBeSeen(Entity other) {
        var l = mcc$living();
        return l != null && other != null && other.mcc$entity != null && l.hasLineOfSight(other.mcc$entity);
    }

    public EntityLivingBase getRevengeTarget() {
        var l = mcc$living();
        return l == null ? null : EntityBridge1122.wrap(l.getLastHurtByMob()) instanceof EntityLivingBase t ? t : null;
    }

    public int getRevengeTimer() {
        var l = mcc$living();
        return l != null ? l.getLastHurtByMobTimestamp() : 0;
    }

    public void setRevengeTarget(EntityLivingBase target) {
        var l = mcc$living();
        if (l != null) l.setLastHurtByMob(target == null ? null : target.mcc$living());
    }

    public EntityLivingBase getLastAttackedEntity() {
        var l = mcc$living();
        return l == null ? null : EntityBridge1122.wrap(l.getLastHurtMob()) instanceof EntityLivingBase t ? t : null;
    }

    public int getLastAttackedEntityTime() {
        var l = mcc$living();
        return l != null ? l.getLastHurtMobTimestamp() : 0;
    }

    public void setLastAttackedEntity(Entity target) {
        var l = mcc$living();
        if (l != null && target != null) l.setLastHurtMob(target.mcc$entity);
    }

    public EntityLivingBase getAttackingEntity() {
        var l = mcc$living();
        return l == null ? null : EntityBridge1122.wrap(l.getKillCredit()) instanceof EntityLivingBase t ? t : null;
    }

    public DamageSource getLastDamageSource() {
        var l = mcc$living();
        if (l != null && l.getLastDamageSource() != null) return DamageSource.mcc$of(l.getLastDamageSource());
        return mcc$lastDamageSource;
    }

    public int getArrowCountInEntity() {
        var l = mcc$living();
        return l != null ? l.getArrowCount() : 0;
    }

    public void setArrowCountInEntity(int count) {
        var l = mcc$living();
        if (l != null) l.setArrowCount(count);
    }

    public float getSwingProgress(float partialTicks) {
        var l = mcc$living();
        return l != null ? l.getAttackAnim(partialTicks) : 0;
    }

    public void swingArm(EnumHand hand) {
        var l = mcc$living();
        if (l != null) l.swing(mcc$hand(hand), true);
    }

    protected void updateArmSwingProgress() {
    }

    public void renderBrokenItemStack(ItemStack stack) {
        var l = mcc$living();
        if (l != null && stack != null && !stack.isEmpty()) l.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK, 0.8F, 0.8F + l.level().random.nextFloat() * 0.4F);
    }

    public void setJumping(boolean jumping) {
        var l = mcc$living();
        if (l != null) l.setJumping(jumping);
        else mcc$local_isJumping = jumping;
    }

    public float getAIMoveSpeed() {
        var l = mcc$living();
        return l != null ? l.getSpeed() : 0.1F;
    }

    public void setAIMoveSpeed(float speed) {
        var l = mcc$living();
        if (l != null) l.setSpeed(speed);
    }

    @Override
    public float getRotationYawHead() {
        return mcc$get$rotationYawHead(this);
    }

    @Override
    public void setRotationYawHead(float yaw) {
        mcc$set$rotationYawHead(this, yaw);
    }

    @Override
    public void setRenderYawOffset(float offset) {
        mcc$set$renderYawOffset(this, offset);
    }

    @Override
    public boolean canBeCollidedWith() {
        return mcc$hosted() ? !mcc$get$isDead(this) : super.canBeCollidedWith();
    }

    @Override
    public boolean canBePushed() {
        return mcc$hosted() ? isEntityAlive() && !isOnLadder() : super.canBePushed();
    }

    public void onItemPickup(Entity entity, int count) {
        if (entity != null && entity.mcc$entity != null && mcc$entity instanceof LivingEntity l && !l.level().isClientSide()) l.take(entity.mcc$entity, count);
    }

    public void dismountEntity(Entity vehicle) {
        if (mcc$entity == null) return;
        if (vehicle != null && vehicle.mcc$entity != null && mcc$entity instanceof LivingEntity l) {
            var spot = vehicle.mcc$entity.getDismountLocationForPassenger(l);
            mcc$entity.dismountTo(spot.x, spot.y, spot.z);
        }
    }

    public boolean attemptTeleport(double x, double y, double z) {
        var l = mcc$living();
        return l != null && l.randomTeleport(x, y, z, true);
    }

    public net.minecraft.world.entity.HumanoidArm mcc$arm() {
        var l = mcc$living();
        return l != null ? l.getMainArm() : net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide getPrimaryHand() {
        return mcc$arm() == net.minecraft.world.entity.HumanoidArm.LEFT ? legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide.LEFT
                : legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide.RIGHT;
    }

    // ------------------------------------------------------------------ sounds (bridged for the host)

    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getHurtSound(DamageSource source) {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.GENERIC_HURT);
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getDeathSound() {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.GENERIC_DEATH);
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getFallSound(int height) {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(height > 4 ? net.minecraft.sounds.SoundEvents.GENERIC_BIG_FALL
                : net.minecraft.sounds.SoundEvents.GENERIC_SMALL_FALL);
    }

    protected float getSoundVolume() {
        return 1.0F;
    }

    protected float getSoundPitch() {
        return isChild() ? (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.5F : (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F;
    }

    protected void playHurtSound(DamageSource source) {
        var sound = getHurtSound(source);
        if (sound != null) playSound(sound, getSoundVolume(), getSoundPitch());
    }

    public final legacyconnector.v1_12_2.net.minecraft.util.SoundEvent mcc$hurtSound(DamageSource source) {
        return getHurtSound(source);
    }

    public final legacyconnector.v1_12_2.net.minecraft.util.SoundEvent mcc$deathSound() {
        return getDeathSound();
    }

    public final float mcc$soundVolume() {
        return getSoundVolume();
    }

    public final float mcc$soundPitch() {
        return getSoundPitch();
    }

    // ------------------------------------------------------------------ held items and equipment (live stacks)

    private ItemStack mcc$slotView(net.minecraft.world.entity.EquipmentSlot slot) {
        LivingEntity l = mcc$living();
        if (l == null) return ItemStack.EMPTY;
        return LiveStacks1122.view(l, slot, new LiveStacks1122.Slot() {
            @Override
            public net.minecraft.world.item.ItemStack get() {
                return l.getItemBySlot(slot);
            }

            @Override
            public void set(net.minecraft.world.item.ItemStack stack) {
                l.setItemSlot(slot, stack);
            }
        });
    }

    private void mcc$slotPut(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
        LivingEntity l = mcc$living();
        if (l == null) return;
        LiveStacks1122.put(l, slot, new LiveStacks1122.Slot() {
            @Override
            public net.minecraft.world.item.ItemStack get() {
                return l.getItemBySlot(slot);
            }

            @Override
            public void set(net.minecraft.world.item.ItemStack s) {
                l.setItemSlot(slot, s);
            }
        }, stack == null ? ItemStack.EMPTY : stack);
    }

    public ItemStack getHeldItem(EnumHand hand) {
        return mcc$slotView(hand == EnumHand.OFF_HAND ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    public ItemStack getHeldItemMainhand() {
        return getHeldItem(EnumHand.MAIN_HAND);
    }

    public ItemStack getHeldItemOffhand() {
        return getHeldItem(EnumHand.OFF_HAND);
    }

    public void setHeldItem(EnumHand hand, ItemStack stack) {
        mcc$slotPut(hand == EnumHand.OFF_HAND ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND, stack);
    }

    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) {
        return slot == null ? ItemStack.EMPTY : mcc$slotView(mcc$slot(slot));
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack stack) {
        if (slot != null) mcc$slotPut(mcc$slot(slot), stack);
    }

    public boolean hasItemInSlot(EntityEquipmentSlot slot) {
        return !getItemStackFromSlot(slot).isEmpty();
    }

    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (EntityEquipmentSlot s : new EntityEquipmentSlot[] {EntityEquipmentSlot.FEET, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.CHEST,
                EntityEquipmentSlot.HEAD}) {
            out.add(getItemStackFromSlot(s));
        }
        return out;
    }

    @Override
    public Iterable<ItemStack> getHeldEquipment() {
        return java.util.List.of(getHeldItemMainhand(), getHeldItemOffhand());
    }

    public void setActiveHand(EnumHand hand) {
        var l = mcc$living();
        if (l != null) l.startUsingItem(mcc$hand(hand));
    }

    public EnumHand getActiveHand() {
        var l = mcc$living();
        return l == null || l.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
    }

    public boolean isHandActive() {
        var l = mcc$living();
        return l != null && l.isUsingItem();
    }

    public ItemStack getActiveItemStack() {
        var l = mcc$living();
        if (l == null || !l.isUsingItem()) return ItemStack.EMPTY;
        return getHeldItem(getActiveHand());
    }

    public int getItemInUseCount() {
        var l = mcc$living();
        return l == null ? 0 : l.getUseItemRemainingTicks();
    }

    public int getItemInUseMaxCount() {
        var l = mcc$living();
        return l == null || !l.isUsingItem() ? 0 : l.getTicksUsingItem();
    }

    public void stopActiveHand() {
        var l = mcc$living();
        if (l != null) l.releaseUsingItem();
    }

    public void resetActiveHand() {
        var l = mcc$living();
        if (l != null) l.stopUsingItem();
    }

    public boolean isActiveItemStackBlocking() {
        var l = mcc$living();
        return l != null && l.isBlocking();
    }

    protected void onItemUseFinish() {
    }

    protected void updateActiveHand() {
    }

    public static net.minecraft.world.InteractionHand mcc$hand(EnumHand hand) {
        return hand == EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
    }

    public static net.minecraft.world.entity.EquipmentSlot mcc$slot(EntityEquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
        };
    }

    public static EntityEquipmentSlot mcc$slot(net.minecraft.world.entity.EquipmentSlot slot) {
        return switch (slot) {
            case OFFHAND -> EntityEquipmentSlot.OFFHAND;
            case FEET -> EntityEquipmentSlot.FEET;
            case LEGS -> EntityEquipmentSlot.LEGS;
            case CHEST -> EntityEquipmentSlot.CHEST;
            case HEAD -> EntityEquipmentSlot.HEAD;
            default -> EntityEquipmentSlot.MAINHAND;
        };
    }

    // ------------------------------------------------------------------ potion effects

    public void addPotionEffect(PotionEffect effect) {
        var l = mcc$living();
        if (l == null || effect == null) return;
        var modern = effect.mcc$toModern();
        if (modern != null) l.addEffect(modern);
    }

    public boolean isPotionActive(Potion potion) {
        var l = mcc$living();
        var holder = potion == null ? null : potion.mcc$modern();
        return l != null && holder != null && l.hasEffect(holder);
    }

    public PotionEffect getActivePotionEffect(Potion potion) {
        var l = mcc$living();
        var holder = potion == null ? null : potion.mcc$modern();
        if (l == null || holder == null) return null;
        var e = l.getEffect(holder);
        return e == null ? null : PotionEffect.mcc$fromModern(e);
    }

    public PotionEffect removeActivePotionEffect(Potion potion) {
        PotionEffect old = getActivePotionEffect(potion);
        removePotionEffect(potion);
        return old;
    }

    public void removePotionEffect(Potion potion) {
        var l = mcc$living();
        var holder = potion == null ? null : potion.mcc$modern();
        if (l != null && holder != null) l.removeEffect(holder);
    }

    public java.util.Collection<PotionEffect> getActivePotionEffects() {
        java.util.List<PotionEffect> out = new java.util.ArrayList<>();
        var l = mcc$living();
        if (l != null) {
            for (var e : l.getActiveEffects()) {
                var p = PotionEffect.mcc$fromModern(e);
                if (p != null) out.add(p);
            }
        }
        return out;
    }

    public java.util.Map<Potion, PotionEffect> getActivePotionMap() {
        java.util.Map<Potion, PotionEffect> out = new java.util.LinkedHashMap<>();
        for (PotionEffect e : getActivePotionEffects()) out.put(e.getPotion(), e);
        return out;
    }

    public void clearActivePotions() {
        var l = mcc$living();
        if (l != null) l.removeAllEffects();
    }

    /** Forge: removes every effect {@code stack} cures (milk cures all vanilla effects). */
    public void curePotionEffects(ItemStack stack) {
        var l = mcc$living();
        if (l == null || l.level().isClientSide()) return;
        if (stack != null && stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.MILK_BUCKET) {
            l.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        }
    }

    public boolean isPotionApplicable(PotionEffect effect) {
        if (effect == null) return false;
        var l = mcc$living();
        if (l != null && !mcc$hosted()) {
            var modern = effect.mcc$toModern();
            return modern == null || l.canBeAffected(modern);
        }
        if (getCreatureAttribute() == EnumCreatureAttribute.UNDEAD) {
            Potion p = effect.getPotion();
            return p != legacyconnector.v1_12_2.net.minecraft.init.MobEffects.REGENERATION && p != legacyconnector.v1_12_2.net.minecraft.init.MobEffects.POISON;
        }
        return true;
    }

    protected void onNewPotionEffect(PotionEffect effect) {
    }

    protected void onChangedPotionEffect(PotionEffect effect, boolean reapply) {
    }

    protected void onFinishedPotionEffect(PotionEffect effect) {
    }

    protected void updatePotionEffects() {
    }

    protected void markPotionsDirty() {
    }

    public void setPartying(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, boolean partying) {
    }

    @Override
    public void notifyDataManagerChange(legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<?> key) {
    }

    // ------------------------------------------------------------------ combat and Forge attributes (area E, 1.12.2)

    /** Forge's swim speed attribute (NeoForge's swim speed). */
    public static final legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute SWIM_SPEED =
            new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.RangedAttribute(null, "forge.swimSpeed", 1.0D, 0.0D, 1024.0D).setShouldWatch(true);
    @Deprecated
    public EntityLivingBase revengeTarget;
    @Deprecated
    public int ticksSinceLastSwing;
    private int mcc$local_ticksSinceLastSwing;
    @Deprecated
    public int ticksElytraFlying;
    private legacyconnector.v1_12_2.net.minecraft.util.CombatTracker mcc$combatTracker;

    public static EntityLivingBase mcc$get$revengeTarget(EntityLivingBase e) {
        return e.getRevengeTarget();
    }

    public static void mcc$set$revengeTarget(EntityLivingBase e, EntityLivingBase v) {
        e.setRevengeTarget(v);
    }

    /** Ticks since the last attack (the modern attack strength ticker for players; kept locally for other entities). */
    public static int mcc$get$ticksSinceLastSwing(EntityLivingBase e) {
        if (e.mcc$entity instanceof net.minecraft.world.entity.player.Player p) return (int) (p.getAttackStrengthScale(0.0F) * p.getCurrentItemAttackStrengthDelay());
        return e.mcc$local_ticksSinceLastSwing;
    }

    public static void mcc$set$ticksSinceLastSwing(EntityLivingBase e, int v) {
        if (e.mcc$entity instanceof net.minecraft.world.entity.player.Player p && v == 0) p.resetAttackStrengthTicker();
        e.mcc$local_ticksSinceLastSwing = v;
    }

    public static int mcc$get$ticksElytraFlying(EntityLivingBase e) {
        return e.getTicksElytraFlying();
    }

    public static void mcc$set$ticksElytraFlying(EntityLivingBase e, int v) {
        // counted by the modern entity
    }

    public legacyconnector.v1_12_2.net.minecraft.util.CombatTracker getCombatTracker() {
        if (mcc$combatTracker == null) mcc$combatTracker = new legacyconnector.v1_12_2.net.minecraft.util.CombatTracker(this);
        return mcc$combatTracker;
    }

    /** Plays the sound of putting on a piece of equipment. */
    protected void playEquipSound(ItemStack stack) {
        var l = mcc$living();
        if (l == null || stack == null || stack.isEmpty()) return;
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        var equippable = modern.getItem() instanceof net.minecraft.world.item.ArmorItem armor ? armor.getEquipSound().value() : net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC.value();
        l.playSound(equippable, 1.0F, 1.0F);
    }
}
