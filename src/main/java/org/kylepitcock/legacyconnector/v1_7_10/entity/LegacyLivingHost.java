package org.kylepitcock.legacyconnector.v1_7_10.entity;

import net.minecraft.world.damagesource.DamageSource;

/** Extra super-calls for hosts of legacy living entities. */
public interface LegacyLivingHost extends LegacyHost {
    void mcc$superAiStep();

    void mcc$superDie(DamageSource source);

    /** Vanilla equipment drops for the death being processed. */
    void mcc$superDropEquipment();

    void mcc$superSetTarget(net.minecraft.world.entity.LivingEntity target);

    /** Vanilla damage application after armour and absorption (LivingEntity.actuallyHurt). */
    void mcc$superActuallyHurt(DamageSource source, float amount);

    /** Vanilla movement for one tick (LivingEntity.travel). */
    void mcc$superTravel(float strafe, float forward);

    /** Vanilla push of an entity this one touches (LivingEntity.doPush). */
    void mcc$superDoPush(net.minecraft.world.entity.Entity other);

    /** Vanilla damage reduction by Resistance and enchantments (LivingEntity.getDamageAfterMagicAbsorb). */
    float mcc$superMagicAbsorb(DamageSource source, float amount);

    /** Vanilla air loss under water (LivingEntity.decreaseAirSupply). */
    int mcc$superDecreaseAirSupply(int air);

    /** Vanilla tick while dead (LivingEntity.tickDeath). */
    void mcc$superTickDeath();
}
