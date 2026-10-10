package org.kylepitcock.mccompat.v1_7_10.bridge;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** 1.7.10 numeric potion ids to modern effect holders. */
public final class PotionBridge {
    private static final java.util.List<Holder<MobEffect>> BY_ID = java.util.Arrays.asList(
            null, MobEffects.MOVEMENT_SPEED, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SPEED, MobEffects.DIG_SLOWDOWN, MobEffects.DAMAGE_BOOST,
            MobEffects.HEAL, MobEffects.HARM, MobEffects.JUMP, MobEffects.CONFUSION, MobEffects.REGENERATION, MobEffects.DAMAGE_RESISTANCE,
            MobEffects.FIRE_RESISTANCE, MobEffects.WATER_BREATHING, MobEffects.INVISIBILITY, MobEffects.BLINDNESS, MobEffects.NIGHT_VISION,
            MobEffects.HUNGER, MobEffects.WEAKNESS, MobEffects.POISON, MobEffects.WITHER, MobEffects.HEALTH_BOOST, MobEffects.ABSORPTION,
            MobEffects.SATURATION);

    private PotionBridge() {}

    public static Holder<MobEffect> effect(int id) {
        if (id <= 0 || id >= BY_ID.size()) throw new IllegalArgumentException("Unknown legacy potion id " + id + " (mod potions are not supported yet)");
        return BY_ID.get(id);
    }

    public static boolean isActive(LivingEntity entity, int id) {
        return id > 0 && id < BY_ID.size() && entity.hasEffect(BY_ID.get(id));
    }

    public static void remove(LivingEntity entity, int id) {
        if (id > 0 && id < BY_ID.size()) entity.removeEffect(BY_ID.get(id));
    }
}
