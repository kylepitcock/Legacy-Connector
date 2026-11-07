package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;

/** Explicit native-base operations; legacy superclass calls never redispatch the original override. */
public interface B173LivingHost {
    LivingEntity legacy();
    boolean callbackActive();
    void syncView();
    void flushView();
    void mcc$baseTick();
    void baseMovement();
    void baseAI();
    void serverAIPhase();
    boolean baseDamage(Entity attacker,int amount);
    void baseKnockback(Entity attacker,int amount,double x,double z);
    void baseOnLanding(float distance);
    boolean baseCanSpawn();
    void baseMarkDead();
    int baseTickCount();
    int baseMovementCount();
    int baseAICount();
}
