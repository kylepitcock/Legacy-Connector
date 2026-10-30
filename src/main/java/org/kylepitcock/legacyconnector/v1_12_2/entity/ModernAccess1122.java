package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.lang.reflect.Field;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

/** Protected state of modern living entities that 1.12 code reads as public fields (looked up once; missing ones read neutral). */
public final class ModernAccess1122 {
    private ModernAccess1122() {}

    private static Field find(Class<?> owner, String name) {
        try {
            return ObfuscationReflectionHelper.findField(owner, name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final Field JUMPING = find(LivingEntity.class, "jumping");
    private static final Field LAST_HURT_BY_PLAYER_TIME = find(LivingEntity.class, "lastHurtByPlayerTime");

    public static boolean jumping(LivingEntity e) {
        try {
            return JUMPING != null && JUMPING.getBoolean(e);
        } catch (IllegalAccessException ex) {
            return false;
        }
    }

    public static int lastHurtByPlayerTime(LivingEntity e) {
        try {
            return LAST_HURT_BY_PLAYER_TIME == null ? 0 : LAST_HURT_BY_PLAYER_TIME.getInt(e);
        } catch (IllegalAccessException ex) {
            return 0;
        }
    }

    public static void setLastHurtByPlayerTime(LivingEntity e, int ticks) {
        try {
            if (LAST_HURT_BY_PLAYER_TIME != null) LAST_HURT_BY_PLAYER_TIME.setInt(e, ticks);
        } catch (IllegalAccessException ex) {
            // keeps the modern value
        }
    }

    private static final Field DEAD = find(LivingEntity.class, "dead");
    private static final Field ANIM_STEP = find(LivingEntity.class, "animStep");
    private static final Field ANIM_STEP_O = find(LivingEntity.class, "animStepO");
    private static final Field BOARDING_COOLDOWN = find(net.minecraft.world.entity.Entity.class, "boardingCooldown");
    private static final Field PISTON_DELTAS = find(net.minecraft.world.entity.Entity.class, "pistonDeltas");
    private static final Field PISTON_DELTAS_GAME_TIME = find(net.minecraft.world.entity.Entity.class, "pistonDeltasGameTime");
    private static final Field HAS_GLOWING_TAG = find(net.minecraft.world.entity.Entity.class, "hasGlowingTag");

    private static boolean getBoolean(Field f, Object o) {
        try {
            return f != null && f.getBoolean(o);
        } catch (IllegalAccessException ex) {
            return false;
        }
    }

    private static void setBoolean(Field f, Object o, boolean v) {
        try {
            if (f != null) f.setBoolean(o, v);
        } catch (IllegalAccessException ex) {
            // keeps the modern value
        }
    }

    private static float getFloat(Field f, Object o) {
        try {
            return f == null ? 0.0F : f.getFloat(o);
        } catch (IllegalAccessException ex) {
            return 0.0F;
        }
    }

    private static void setFloat(Field f, Object o, float v) {
        try {
            if (f != null) f.setFloat(o, v);
        } catch (IllegalAccessException ex) {
            // keeps the modern value
        }
    }

    private static int getInt(Field f, Object o) {
        try {
            return f == null ? 0 : f.getInt(o);
        } catch (IllegalAccessException ex) {
            return 0;
        }
    }

    private static void setInt(Field f, Object o, int v) {
        try {
            if (f != null) f.setInt(o, v);
        } catch (IllegalAccessException ex) {
            // keeps the modern value
        }
    }

    private static final Field NO_JUMP_DELAY = find(LivingEntity.class, "noJumpDelay");

    /** LivingEntity.noJumpDelay (1.12 EntityLivingBase.jumpTicks: ticks until the entity may jump again). */
    public static int noJumpDelay(LivingEntity e) {
        return getInt(NO_JUMP_DELAY, e);
    }

    public static void setNoJumpDelay(LivingEntity e, int v) {
        setInt(NO_JUMP_DELAY, e, v);
    }

    /** LivingEntity.dead: the death was handled (1.12 EntityLivingBase.dead). */
    public static boolean dead(LivingEntity e) {
        return getBoolean(DEAD, e);
    }

    public static void setDead(LivingEntity e, boolean v) {
        setBoolean(DEAD, e, v);
    }

    /** LivingEntity.animStep / animStepO (1.12 onGroundSpeedFactor / prevOnGroundSpeedFactor). */
    public static float animStep(LivingEntity e, boolean previous) {
        return getFloat(previous ? ANIM_STEP_O : ANIM_STEP, e);
    }

    public static void setAnimStep(LivingEntity e, boolean previous, float v) {
        setFloat(previous ? ANIM_STEP_O : ANIM_STEP, e, v);
    }

    /** Entity.boardingCooldown (1.12 rideCooldown). */
    public static int boardingCooldown(net.minecraft.world.entity.Entity e) {
        return getInt(BOARDING_COOLDOWN, e);
    }

    public static void setBoardingCooldown(net.minecraft.world.entity.Entity e, int v) {
        setInt(BOARDING_COOLDOWN, e, v);
    }

    /** Entity.pistonDeltas: the live array of the modern entity (or null). */
    public static double[] pistonDeltas(net.minecraft.world.entity.Entity e) {
        try {
            return PISTON_DELTAS == null ? null : (double[]) PISTON_DELTAS.get(e);
        } catch (IllegalAccessException ex) {
            return null;
        }
    }

    public static long pistonDeltasGameTime(net.minecraft.world.entity.Entity e) {
        try {
            return PISTON_DELTAS_GAME_TIME == null ? 0L : PISTON_DELTAS_GAME_TIME.getLong(e);
        } catch (IllegalAccessException ex) {
            return 0L;
        }
    }

    public static void setPistonDeltasGameTime(net.minecraft.world.entity.Entity e, long v) {
        try {
            if (PISTON_DELTAS_GAME_TIME != null) PISTON_DELTAS_GAME_TIME.setLong(e, v);
        } catch (IllegalAccessException ex) {
            // keeps the modern value
        }
    }

    /** Entity.hasGlowingTag (1.12 glowing). */
    public static boolean glowing(net.minecraft.world.entity.Entity e) {
        return getBoolean(HAS_GLOWING_TAG, e);
    }

    private static final Field HOOK_LIFE = find(net.minecraft.world.entity.projectile.FishingHook.class, "life");
    private static final Field HOOK_NIBBLE = find(net.minecraft.world.entity.projectile.FishingHook.class, "nibble");
    private static final java.lang.reflect.Method HOOK_PULL = method(net.minecraft.world.entity.projectile.FishingHook.class, "pullEntity",
            net.minecraft.world.entity.Entity.class);
    private static final java.lang.reflect.Method CAN_HIT_ENTITY = method(net.minecraft.world.entity.projectile.Projectile.class, "canHitEntity",
            net.minecraft.world.entity.Entity.class);

    private static java.lang.reflect.Method method(Class<?> owner, String name, Class<?>... params) {
        try {
            return ObfuscationReflectionHelper.findMethod(owner, name, params);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** FishingHook.life (ticks the hook lay on the ground, 1.12 ticksInGround) and nibble (ticks a fish is biting, 1.12 ticksCatchable). */
    public static int hookTicks(net.minecraft.world.entity.projectile.FishingHook h, boolean nibble) {
        return getInt(nibble ? HOOK_NIBBLE : HOOK_LIFE, h);
    }

    public static void setHookTicks(net.minecraft.world.entity.projectile.FishingHook h, boolean nibble, int v) {
        setInt(nibble ? HOOK_NIBBLE : HOOK_LIFE, h, v);
    }

    /** FishingHook.pullEntity: pulls the entity towards the hook's owner. */
    public static void pullEntity(net.minecraft.world.entity.projectile.FishingHook h, net.minecraft.world.entity.Entity target) {
        try {
            if (HOOK_PULL != null && target != null) HOOK_PULL.invoke(h, target);
        } catch (ReflectiveOperationException e) {
            // nothing pulled
        }
    }

    /** Projectile.canHitEntity: whether the projectile may hit the entity (not its owner while leaving, not spectators). */
    public static boolean canHitEntity(net.minecraft.world.entity.projectile.Projectile p, net.minecraft.world.entity.Entity target) {
        try {
            return CAN_HIT_ENTITY != null && target != null && (boolean) CAN_HIT_ENTITY.invoke(p, target);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static final java.lang.reflect.Method WATER_SPLASH = method(net.minecraft.world.entity.Entity.class, "doWaterSplashEffect");

    /** Entity.doWaterSplashEffect: the splash sound and particles of an entity falling into water. */
    public static void waterSplash(net.minecraft.world.entity.Entity e) {
        try {
            if (WATER_SPLASH != null && e != null) WATER_SPLASH.invoke(e);
        } catch (ReflectiveOperationException ex) {
            // no splash
        }
    }

    private static final Field FALLING_HURTS = find(net.minecraft.world.entity.item.FallingBlockEntity.class, "hurtEntities");
    private static final Field FALLING_DAMAGE_MAX = find(net.minecraft.world.entity.item.FallingBlockEntity.class, "fallDamageMax");
    private static final Field FALLING_DAMAGE_PER_DISTANCE = find(net.minecraft.world.entity.item.FallingBlockEntity.class, "fallDamagePerDistance");

    /** FallingBlockEntity.hurtEntities (1.12 EntityFallingBlock.hurtEntities). */
    public static boolean fallingHurts(net.minecraft.world.entity.item.FallingBlockEntity e) {
        return getBoolean(FALLING_HURTS, e);
    }

    public static void setFallingHurts(net.minecraft.world.entity.item.FallingBlockEntity e, boolean v) {
        setBoolean(FALLING_HURTS, e, v);
    }

    /** FallingBlockEntity.fallDamageMax (1.12 fallHurtMax). */
    public static int fallDamageMax(net.minecraft.world.entity.item.FallingBlockEntity e) {
        return getInt(FALLING_DAMAGE_MAX, e);
    }

    public static void setFallDamageMax(net.minecraft.world.entity.item.FallingBlockEntity e, int v) {
        setInt(FALLING_DAMAGE_MAX, e, v);
    }

    /** FallingBlockEntity.fallDamagePerDistance (1.12 fallHurtAmount). */
    public static float fallDamagePerDistance(net.minecraft.world.entity.item.FallingBlockEntity e) {
        return getFloat(FALLING_DAMAGE_PER_DISTANCE, e);
    }

    public static void setFallDamagePerDistance(net.minecraft.world.entity.item.FallingBlockEntity e, float v) {
        setFloat(FALLING_DAMAGE_PER_DISTANCE, e, v);
    }

    private static final java.lang.reflect.Method FOLLOW_MOMMY = method(net.minecraft.world.entity.animal.horse.AbstractHorse.class, "followMommy");

    /** AbstractHorse.followMommy: a foal walks towards the nearest adult horse. */
    public static void followMommy(net.minecraft.world.entity.animal.horse.AbstractHorse h) {
        try {
            if (FOLLOW_MOMMY != null && h != null) FOLLOW_MOMMY.invoke(h);
        } catch (ReflectiveOperationException e) {
            // stays put
        }
    }

    private static final Class<?> ILLAGER_SPELL = spellClass();
    private static final java.lang.reflect.Method SET_CASTING_SPELL = ILLAGER_SPELL == null ? null
            : method(net.minecraft.world.entity.monster.SpellcasterIllager.class, "setIsCastingSpell", ILLAGER_SPELL);

    private static Class<?> spellClass() {
        for (Class<?> c : net.minecraft.world.entity.monster.SpellcasterIllager.class.getDeclaredClasses()) if (c.isEnum()) return c;
        return null;
    }

    /** SpellcasterIllager.setIsCastingSpell with the spell of that ordinal (NONE, SUMMON_VEX, FANGS, WOLOLO, DISAPPEAR, BLINDNESS, as in 1.12). */
    public static void setCastingSpell(net.minecraft.world.entity.monster.SpellcasterIllager illager, int ordinal) {
        if (SET_CASTING_SPELL == null || illager == null) return;
        Object[] spells = ILLAGER_SPELL.getEnumConstants();
        try {
            SET_CASTING_SPELL.invoke(illager, spells[Math.max(0, Math.min(ordinal, spells.length - 1))]);
        } catch (ReflectiveOperationException e) {
            // the spell stays
        }
    }
}
