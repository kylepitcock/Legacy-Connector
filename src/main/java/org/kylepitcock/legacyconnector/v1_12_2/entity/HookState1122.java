package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/**
 * Per-host state of the modern hooks handed to the legacy object (the fall being processed), the shared hook bodies of the hosts,
 * and the checks for whether a legacy class changed a hook at all (otherwise the host keeps the vanilla behaviour).
 */
final class HookState1122 {
    /** The vanilla causeFallDamage of the host. */
    interface FallFunction {
        boolean apply(float distance, float multiplier, DamageSource source);
    }

    boolean inFall;
    private DamageSource fallSource;
    private boolean fallResult;

    /** Runs the legacy {@code fall} for a modern landing; its base hands it back through {@link #superFall}. */
    boolean fall(Entity legacy, float distance, float multiplier, DamageSource source) {
        fallSource = source;
        fallResult = false;
        inFall = true;
        try {
            legacy.fall(distance, multiplier);
        } finally {
            inFall = false;
            fallSource = null;
        }
        return fallResult;
    }

    void superFall(float distance, float multiplier, FallFunction vanilla, DamageSource fallback) {
        boolean was = inFall;
        inFall = true;
        try {
            fallResult = vanilla.apply(distance, multiplier, fallSource != null ? fallSource : fallback);
        } finally {
            inFall = was;
        }
    }

    static EnumHand hand(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    static InteractionResult result(EnumActionResult r, boolean client) {
        if (r == null) return InteractionResult.PASS;
        return switch (r) {
            case SUCCESS -> InteractionResult.sidedSuccess(client);
            case FAIL -> InteractionResult.FAIL;
            default -> InteractionResult.PASS;
        };
    }

    /** Entity.processInitialInteract: right-clicking the entity. */
    static InteractionResult interact(Entity legacy, Player player, InteractionHand hand, boolean client) {
        if (!(EntityBridge1122.wrap(player) instanceof EntityPlayer p)) return InteractionResult.PASS;
        try {
            return legacy.processInitialInteract(p, hand(hand)) ? InteractionResult.sidedSuccess(client) : InteractionResult.PASS;
        } finally {
            LiveStacks1122.flushAll();
        }
    }

    /** Entity.applyPlayerInteraction: right-clicking a point on the entity (armor stands). */
    static InteractionResult interactAt(Entity legacy, Player player, net.minecraft.world.phys.Vec3 vec, InteractionHand hand) {
        if (!Hosts1122.overrides(legacy.getClass(), "applyPlayerInteraction", EntityPlayer.class,
                legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d.class, EnumHand.class)) {
            return InteractionResult.PASS;
        }
        if (!(EntityBridge1122.wrap(player) instanceof EntityPlayer p)) return InteractionResult.PASS;
        try {
            return result(legacy.applyPlayerInteraction(p, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d.mcc$of(vec), hand(hand)),
                    player.level().isClientSide());
        } finally {
            LiveStacks1122.flushAll();
        }
    }

    static void startTracking(Entity legacy, net.minecraft.server.level.ServerPlayer player) {
        if (EntityBridge1122.wrap(player) instanceof EntityPlayerMP p) legacy.addTrackingPlayer(p);
    }

    static void stopTracking(Entity legacy, net.minecraft.server.level.ServerPlayer player) {
        if (EntityBridge1122.wrap(player) instanceof EntityPlayerMP p) legacy.removeTrackingPlayer(p);
    }

    static legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt wrapBolt(net.minecraft.world.entity.LightningBolt bolt) {
        return EntityBridge1122.wrap(bolt) instanceof legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt b ? b : null;
    }

    /** Whether the legacy class places its passengers itself (updatePassenger or getMountedYOffset changed). */
    static boolean customRider(Entity legacy) {
        Class<?> c = legacy.getClass();
        return Hosts1122.overrides(c, "updatePassenger", Entity.class) || Hosts1122.overrides(c, "getMountedYOffset");
    }

    /** Whether the legacy class interpolates the server position itself (boats and carts do). */
    static boolean customLerp(Entity legacy) {
        return Hosts1122.overrides(legacy.getClass(), "setPositionAndRotationDirect", double.class, double.class, double.class, float.class, float.class,
                int.class, boolean.class);
    }
}
