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
}
