package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;

/**
 * 1.7.10 mods resized any entity with {@code Entity.setSize(width, height)} (and a player's eye height with the {@code eyeHeight}
 * field): Fisk's Heroes shrinks and grows its heroes, other mods do the same to mobs. The modern game takes an entity's size from its
 * type and pose, so the sizes mods ask for are expressed as Pehkui scales relative to the type's own size (hitbox, eye height and
 * model; Pehkui also keeps the sneaking and swimming poses in proportion). A size equal to the type's own resets the scales.
 */
public final class EntitySizesPehkui1710 {
    /** Entities whose eye height a mod set explicitly (otherwise it follows the height). */
    private static final Set<Entity> EXPLICIT_EYE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private EntitySizesPehkui1710() {}

    /** {@code Entity.setSize} on an entity that is not a host: the box, the model and (unless set explicitly) the eye height change now. */
    public static void setSize(Entity entity, float width, float height) {
        EntityDimensions own = entity.getType().getDimensions();
        if (own.width() <= 0 || own.height() <= 0) return;
        float h = height / own.height();
        set(ScaleTypes.HITBOX_WIDTH, entity, width / own.width());
        set(ScaleTypes.HITBOX_HEIGHT, entity, h);
        set(ScaleTypes.MODEL_WIDTH, entity, width / own.width());
        set(ScaleTypes.MODEL_HEIGHT, entity, h);
        if (!EXPLICIT_EYE.contains(entity)) set(ScaleTypes.EYE_HEIGHT, entity, h);
    }

    /** The {@code eyeHeight} field of a player: the camera height above the feet. */
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
