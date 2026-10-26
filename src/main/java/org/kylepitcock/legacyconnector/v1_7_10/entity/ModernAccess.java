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

    private static final class Horses {
        static final Method SET_TYPE_VARIANT = find(net.minecraft.world.entity.animal.horse.Horse.class, "setTypeVariant", int.class);
    }

    /** A horse's coat colour and markings as one value (colour | markings << 8, as 1.7.10 stored it). */
    public static void setHorseTypeVariant(net.minecraft.world.entity.animal.horse.Horse horse, int variant) {
        if (Horses.SET_TYPE_VARIANT != null) {
            call(Horses.SET_TYPE_VARIANT, horse, null, variant);
        } else {
            horse.setVariant(net.minecraft.world.entity.animal.horse.Variant.byId(variant & 255));
        }
    }

    private static java.lang.reflect.Field findField(Class<?> owner, String name) {
        try {
            return ObfuscationReflectionHelper.findField(owner, name);
        } catch (RuntimeException e) {
            LOG.warn("Modern member {}.{} not found: {}", owner.getSimpleName(), name, e.toString());
            return null;
        }
    }

    private static void set(java.lang.reflect.Field f, Object target, Object value) {
        if (f == null) return;
        try {
            f.set(target, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // keeps the modern default
        }
    }

    private static final class Falling {
        static final java.lang.reflect.Field BLOCK_STATE = findField(net.minecraft.world.entity.item.FallingBlockEntity.class, "blockState");
        static final java.lang.reflect.Field HURT_ENTITIES = findField(net.minecraft.world.entity.item.FallingBlockEntity.class, "hurtEntities");
        static final java.lang.reflect.Field BLOCKS_BUILDING = findField(net.minecraft.world.entity.Entity.class, "blocksBuilding");
    }

    /** What a modern falling block entity is (its private constructor's job, for one made through its type). */
    public static void setFallingBlockState(net.minecraft.world.entity.item.FallingBlockEntity entity, net.minecraft.world.level.block.state.BlockState state) {
        set(Falling.BLOCK_STATE, entity, state);
        set(Falling.BLOCKS_BUILDING, entity, true);
    }

    public static void setFallingBlockHurts(net.minecraft.world.entity.item.FallingBlockEntity entity, boolean hurts) {
        if (hurts) entity.setHurtsEntities(2.0F, 40);
        else set(Falling.HURT_ENTITIES, entity, false);
    }

    private static final class Zombies {
        static final Method START_CONVERTING = find(net.minecraft.world.entity.monster.ZombieVillager.class, "startConverting", java.util.UUID.class, int.class);
    }

    /** Starts curing a zombie villager (it turns back into a villager after {@code ticks}). */
    public static void startZombieConversion(net.minecraft.world.entity.monster.ZombieVillager zombie, int ticks) {
        call(Zombies.START_CONVERTING, zombie, null, null, ticks);
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
