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
}
