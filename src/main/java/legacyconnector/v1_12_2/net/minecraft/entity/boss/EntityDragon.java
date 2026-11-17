package legacyconnector.v1_12_2.net.minecraft.entity.boss;

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

/** 1.12.2 ender dragon. A view reads the modern dragon (death ticks, body part); its fight and phase managers are the modern
 * game's (not modelled as legacy objects). */
public class EntityDragon extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving implements legacyconnector.v1_12_2.net.minecraft.entity.IEntityMultiPart, legacyconnector.v1_12_2.net.minecraft.entity.monster.IMob {
    public EntityDragon(World world) {
        super(world);
        setSize(16.0F, 8.0F);
    }

    protected EntityDragon(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.boss.enderdragon.EnderDragon mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon m ? m : null;
    }

    /** The phases of the fight (the modern dragon's). */
    public legacyconnector.v1_12_2.net.minecraft.entity.boss.dragon.phase.PhaseManager getPhaseManager() {
        return new legacyconnector.v1_12_2.net.minecraft.entity.boss.dragon.phase.PhaseManager(this);
    }

    /** The dragon fight of the end this dragon belongs to, or null. */
    public legacyconnector.v1_12_2.net.minecraft.world.end.DragonFightManager getFightManager() {
        var m = mcc$self();
        var fight = m == null ? null : m.getDragonFight();
        return fight == null ? null : new legacyconnector.v1_12_2.net.minecraft.world.end.DragonFightManager(fight);
    }

    @Deprecated
    public int deathTicks;
    private int mcc$deathTicks;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.entity.MultiPartEntityPart dragonPartBody;

    public static int mcc$get$deathTicks(EntityDragon e) {
        var m = e.mcc$self();
        return m != null && !e.mcc$hosted() ? m.dragonDeathTime : e.mcc$deathTicks;
    }

    public static void mcc$set$deathTicks(EntityDragon e, int v) {
        var m = e.mcc$self();
        if (m != null && !e.mcc$hosted()) m.dragonDeathTime = v;
        e.mcc$deathTicks = v;
    }

    public static legacyconnector.v1_12_2.net.minecraft.entity.MultiPartEntityPart mcc$get$dragonPartBody(EntityDragon e) {
        return null;
    }

    public static void mcc$set$dragonPartBody(EntityDragon e, legacyconnector.v1_12_2.net.minecraft.entity.MultiPartEntityPart v) {
    }

    /** Phantom: the wing-flap animation phase ({@link #mcc$get$animTime}; the modern dragon's {@code flapTime}). */
    @Deprecated
    public float animTime;
    /** Phantom: the animation phase of the previous tick ({@link #mcc$get$prevAnimTime}; the modern {@code oFlapTime}). */
    @Deprecated
    public float prevAnimTime;
    private float mcc$animTime;
    private float mcc$prevAnimTime;

    public static float mcc$get$animTime(EntityDragon e) {
        var m = e.mcc$self();
        return m != null && !e.mcc$hosted() ? m.flapTime : e.mcc$animTime;
    }

    public static void mcc$set$animTime(EntityDragon e, float v) {
        var m = e.mcc$self();
        if (m != null && !e.mcc$hosted()) m.flapTime = v;
        e.mcc$animTime = v;
    }

    public static float mcc$get$prevAnimTime(EntityDragon e) {
        var m = e.mcc$self();
        return m != null && !e.mcc$hosted() ? m.oFlapTime : e.mcc$prevAnimTime;
    }

    public static void mcc$set$prevAnimTime(EntityDragon e, float v) {
        var m = e.mcc$self();
        if (m != null && !e.mcc$hosted()) m.oFlapTime = v;
        e.mcc$prevAnimTime = v;
    }

    /** The recorded movement of the body at {@code index} ticks ago, smoothed to the partial tick: {yaw, y, z-wobble} as the parts follow it. */
    public double[] getMovementOffsets(int index, float partialTicks) {
        var m = mcc$self();
        if (m != null) return m.getLatencyPos(index, partialTicks);
        return new double[] {0.0D, mcc$y(), 0.0D};
    }

    /**
     * The height of a head or neck part above the body (1.12): landing and take-off scale with the distance to the podium, a
     * stationary phase spaces the parts evenly, otherwise the part follows the recorded height difference.
     */
    public float getHeadPartYOffset(int part, double[] spineStart, double[] partPos) {
        var m = mcc$self();
        double offset;
        if (m != null) {
            var phase = m.getPhaseManager().getCurrentPhase();
            var type = phase.getPhase();
            if (type == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.LANDING
                    || type == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.TAKEOFF) {
                var podium = m.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        net.minecraft.world.level.levelgen.feature.EndPodiumFeature.getLocation(net.minecraft.core.BlockPos.ZERO));
                float f = Math.max((float) Math.sqrt(m.distanceToSqr(podium.getCenter())) / 4.0F, 1.0F);
                offset = (double) ((float) part / f);
                return (float) offset;
            }
            if (phase.isSitting()) return (float) part;
        }
        offset = part == 6 ? 0.0D : partPos[1] - spineStart[1];
        return (float) offset;
    }

    /** Damage to one of its parts: the head takes full damage, other parts a quarter (verbatim 1.12), as the whole dragon. */
    public boolean attackEntityFromPart(legacyconnector.v1_12_2.net.minecraft.entity.MultiPartEntityPart part, DamageSource source, float damage) {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) return attackEntityFrom(source, damage);
        return attackEntityFrom(source, damage / 4.0F + 1.0F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(200.0D);
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity[] getParts() {
        var m = mcc$self();
        if (m == null) return null;
        var parts = m.getSubEntities();
        var out = new legacyconnector.v1_12_2.net.minecraft.entity.Entity[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(parts[i]);
        return out;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.ENDER_DRAGON_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.ENDER_DRAGON_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.ENDER_DRAGON_DEATH);
    }

    @Override
    public World getWorld() {
        return mcc$get$world(this);
    }
}
