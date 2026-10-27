package org.kylepitcock.legacyconnector.v1_12_2.entity;

import net.minecraft.world.damagesource.DamageSource;

/** Extra super-calls for hosts of 1.12.2 living entities. */
public interface LivingHost1122 extends Host1122 {
    void mcc$superAiStep();

    void mcc$superDie(DamageSource source);

    /** Vanilla equipment drops for the death being processed. */
    void mcc$superDropEquipment();

    void mcc$superSetTarget(net.minecraft.world.entity.LivingEntity target);

    /** Vanilla damage application after armour and absorption (LivingEntity.actuallyHurt). */
    void mcc$superActuallyHurt(DamageSource source, float amount);

    /** Vanilla movement for one tick (LivingEntity.travel). */
    void mcc$superTravel(float strafe, float vertical, float forward);

    /** Vanilla push of an entity this one touches (LivingEntity.doPush). */
    void mcc$superDoPush(net.minecraft.world.entity.Entity other);

    /** Vanilla damage reduction by Resistance and enchantments (LivingEntity.getDamageAfterMagicAbsorb). */
    float mcc$superMagicAbsorb(DamageSource source, float amount);

    /** Vanilla damage reduction by armour (LivingEntity.getDamageAfterArmorAbsorb). */
    float mcc$superArmorAbsorb(DamageSource source, float amount);

    /** Vanilla air loss under water (LivingEntity.decreaseAirSupply). */
    int mcc$superDecreaseAirSupply(int air);

    /** Vanilla tick while dead (LivingEntity.tickDeath). */
    void mcc$superTickDeath();

    /** Vanilla melee hit (Mob.doHurtTarget). */
    boolean mcc$superDoHurtTarget(net.minecraft.world.entity.Entity target);

    /** Vanilla jump (LivingEntity.jumpFromGround). */
    void mcc$superJump();

    /** Vanilla interaction (Mob.mobInteract). */
    net.minecraft.world.InteractionResult mcc$superMobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand);
}
