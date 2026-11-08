package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;

/** Native identity with explicit original-superclass entrypoints. */
public interface B173EntityHost {
    Entity legacy();
    default Entity prepareRenderView(float partial){syncView();return legacy();}
    boolean callbackActive();
    void syncView();
    void flushView();
    void mcc$baseTick();
    void baseMove(double x,double y,double z);
    boolean baseDamage(Entity attacker,int amount);
    void baseMarkDead();
    default int originalTickCount(){return baseTickCount();}
    int baseTickCount();
    default void publishRenderState(){}
}
