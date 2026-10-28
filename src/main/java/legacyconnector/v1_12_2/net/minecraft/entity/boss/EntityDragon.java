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
