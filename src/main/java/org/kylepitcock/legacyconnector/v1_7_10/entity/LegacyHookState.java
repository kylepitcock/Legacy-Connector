package org.kylepitcock.legacyconnector.v1_7_10.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.damagesource.DamageSource;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/**
 * Per-host state of the modern hooks that are handed to the legacy object (the fall being processed) and the checks for
 * whether a legacy class changed a hook at all (otherwise the host keeps the vanilla behaviour).
 */
final class LegacyHookState {
    /** The vanilla causeFallDamage of the host. */
    interface FallFunction {
        boolean apply(float distance, float multiplier, DamageSource source);
    }

    boolean inFall;
    private float fallMultiplier = 1.0F;
    private DamageSource fallSource;
    private boolean fallResult;

    /** Runs the legacy {@code fall} for a modern landing; its base hands it back through {@link #superFall}. */
    boolean fall(Entity legacy, float distance, float multiplier, DamageSource source) {
        fallMultiplier = multiplier;
        fallSource = source;
        fallResult = false;
        inFall = true;
        try {
            legacy.mcc$fall(distance);
        } finally {
            inFall = false;
            fallSource = null;
        }
        return fallResult;
    }

    void superFall(float distance, FallFunction vanilla, DamageSource fallback) {
        boolean was = inFall;
        inFall = true;
        try {
            fallResult = vanilla.apply(distance, fallMultiplier, fallSource != null ? fallSource : fallback);
        } finally {
            inFall = was;
        }
    }

    static EntityLightningBolt wrapBolt(net.minecraft.world.entity.LightningBolt bolt) {
        return EntityBridge.wrap(bolt) instanceof EntityLightningBolt b ? b : null;
    }

    /** Whether the legacy class places its rider itself (updateRiderPosition or getMountedYOffset changed). */
    static boolean customRider(Entity legacy) {
        Class<?> c = legacy.getClass();
        // legacy carts seat their rider by the 1.7.10 rule (the host's box sits lower than the legacy posY)
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart) return true;
        return LegacyHosts.overrides(c, "updateRiderPosition") || LegacyHosts.overrides(c, "getMountedYOffset");
    }

    static boolean customStepSound(Entity legacy) {
        return LegacyHosts.overrides(legacy.getClass(), "playStepSound", int.class, int.class, int.class, legacyconnector.v1_7_10.net.minecraft.block.Block.class);
    }

    /** Legacy carts interpolate the server position themselves (EntityMinecart's turnProgress). */
    static boolean customLerp(Entity legacy) {
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart) return true;
        return LegacyHosts.overrides(legacy.getClass(), "setPositionAndRotation2", double.class, double.class, double.class, float.class, float.class, int.class);
    }
}
