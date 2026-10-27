package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide;

/** Pose facts vanilla models read from an entity, taken from the modern entity a legacy one stands for. */
public final class EntityPose1122 {
    private EntityPose1122() {}

    public static boolean sneaking(Entity entity) {
        return entity != null && entity.mcc$entity != null && entity.mcc$entity.isCrouching();
    }

    public static int elytraTicks(Entity entity) {
        return entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l.getFallFlyingTicks() : 0;
    }

    /** The side of the arm that swings: the main arm, or the other one when the off hand swings. */
    public static EnumHandSide swingingSide(Entity entity) {
        if (entity == null || !(entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l)) return EnumHandSide.RIGHT;
        EnumHandSide main = EnumHandSide.mcc$of(l.getMainArm());
        return l.swingingArm == net.minecraft.world.InteractionHand.OFF_HAND ? main.opposite() : main;
    }

    public static boolean child(Entity entity) {
        return entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l && l.isBaby();
    }
}
