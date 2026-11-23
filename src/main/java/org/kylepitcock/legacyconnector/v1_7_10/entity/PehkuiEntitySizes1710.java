package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;

/** Loaded reflectively by {@link EntitySizes1710} only when the optional Pehkui mod is present. */
public final class PehkuiEntitySizes1710 {
    /** Entities whose eye height a legacy mod set explicitly (otherwise it follows the entity height). */
    private static final Set<Entity> EXPLICIT_EYE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private PehkuiEntitySizes1710() {}

    public static void setSize(Entity entity, float width, float height) {
        EntityDimensions own = entity.getType().getDimensions();
        if (own.width() <= 0 || own.height() <= 0) return;
        float h = height / own.height();
        set(ScaleTypes.HITBOX_WIDTH, entity, width / own.width());
        set(ScaleTypes.HITBOX_HEIGHT, entity, h);
        // 1.7.10's setSize only changed the hitbox. The model follows it only for a size change that scales the whole body (Fisk's shrink and grow
        // powers): a flying hero's hitbox is 0.6 x 0.6 while the body is the same, and a model scaled by that came out squashed (#151)
        float w = width / own.width();
        boolean uniform = Math.abs(w - h) <= 0.02F * Math.max(w, h);
        set(ScaleTypes.MODEL_WIDTH, entity, uniform ? w : 1.0F);
        set(ScaleTypes.MODEL_HEIGHT, entity, uniform ? h : 1.0F);
        if (!EXPLICIT_EYE.contains(entity)) set(ScaleTypes.EYE_HEIGHT, entity, h);
    }

    public static void setEyeHeight(Entity entity, float eye) {
        float own = entity.getType().getDimensions().eyeHeight();
        if (own <= 0) return;
        float scale = eye / own;
        if (Math.abs(scale - 1.0F) < 1.0E-4F) EXPLICIT_EYE.remove(entity);
        else EXPLICIT_EYE.add(entity);
        set(ScaleTypes.EYE_HEIGHT, entity, scale);
    }

    private static void set(ScaleType type, Entity entity, float value) {
        if (Math.abs(value - 1.0F) < 1.0E-4F) value = 1.0F;
        ScaleData data = type.getScaleData(entity);
        if (Math.abs(data.getTargetScale() - value) < 1.0E-5F && Math.abs(data.getScale() - value) < 1.0E-5F) return;
        data.setPersistence(Boolean.FALSE);
        data.setScaleTickDelay(0);
        data.setScale(value);
        data.setTargetScale(value);
    }
}
