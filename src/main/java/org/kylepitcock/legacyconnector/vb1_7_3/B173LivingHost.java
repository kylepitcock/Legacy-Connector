package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;

/** Explicit native-base operations; legacy superclass calls never redispatch the original override. */
public interface B173LivingHost extends B173EntityHost {
    LivingEntity legacy();
    boolean callbackActive();
    void syncView();
    void flushView();
    void mcc$baseTick();
    void baseMovement();
    void baseTravel(float sideways,float forward);
    void baseMove(double x,double y,double z);
    boolean baseIsOnLadder();
    boolean baseInsideWall();
    void baseFall(double deltaY,boolean landed);
    void baseSetPositionAndAngles(double x,double y,double z,float yaw,float pitch);
    void baseSetRotation(float yaw,float pitch);
    void baseInterpolate(double x,double y,double z,float yaw,float pitch,int steps);
    void baseEntityStatus(byte status);
    void baseSetPath(legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path path);
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
