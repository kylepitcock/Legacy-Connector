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
}
