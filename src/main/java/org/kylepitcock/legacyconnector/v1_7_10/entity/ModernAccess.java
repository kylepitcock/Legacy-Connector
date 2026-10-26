package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.lang.reflect.Method;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protected members of modern vanilla entities that legacy calls on a view need (a host reaches its own through super). Looked up
 * once; a missing member falls back to a neutral answer.
 */
public final class ModernAccess {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Entities");

    private ModernAccess() {}

    private static Method find(Class<?> owner, String name, Class<?>... params) {
        try {
            return ObfuscationReflectionHelper.findMethod(owner, name, params);
        } catch (RuntimeException e) {
            LOG.warn("Modern member {}.{} not found: {}", owner.getSimpleName(), name, e.toString());
            return null;
        }
    }

    private static final class Living {
        static final Method MAGIC_ABSORB = find(LivingEntity.class, "getDamageAfterMagicAbsorb", DamageSource.class, float.class);
        static final Method ARMOR_ABSORB = find(LivingEntity.class, "getDamageAfterArmorAbsorb", DamageSource.class, float.class);
        static final Method DECREASE_AIR = find(LivingEntity.class, "decreaseAirSupply", int.class);
        static final Method TICK_DEATH = find(LivingEntity.class, "tickDeath");
    }

    private static final class Hanging {
        static final Method SET_DIRECTION = find(net.minecraft.world.entity.decoration.HangingEntity.class, "setDirection", net.minecraft.core.Direction.class);
    }

    /** Turns a hanging entity (painting, item frame) to face {@code direction}, recomputing its box. */
    public static void setHangingDirection(net.minecraft.world.entity.decoration.HangingEntity entity, net.minecraft.core.Direction direction) {
        call(Hanging.SET_DIRECTION, entity, null, direction);
    }

    public static void tickDeath(LivingEntity entity) {
        call(Living.TICK_DEATH, entity, null);
    }

    private static Object call(Method m, Object target, Object fallback, Object... args) {
        if (m == null) return fallback;
        try {
            return m.invoke(target, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return fallback;
        }
    }

    public static float damageAfterMagicAbsorb(LivingEntity entity, DamageSource source, float amount) {
        return (Float) call(Living.MAGIC_ABSORB, entity, amount, source, amount);
    }

    public static float damageAfterArmorAbsorb(LivingEntity entity, DamageSource source, float amount) {
        return (Float) call(Living.ARMOR_ABSORB, entity, amount, source, amount);
    }

    public static int decreaseAirSupply(LivingEntity entity, int air) {
        return (Integer) call(Living.DECREASE_AIR, entity, air - 1, air);
    }
}
