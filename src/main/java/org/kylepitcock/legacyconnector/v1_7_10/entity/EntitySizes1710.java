package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;

/**
 * 1.7.10 mods resized any entity with {@code Entity.setSize(width, height)} (and a player's eye height with the {@code eyeHeight}
 * field): Fisk's Heroes shrinks and grows its heroes, other mods do the same to mobs. The modern game takes an entity's size from its
 * type and pose, so the sizes mods ask for are kept here for entities that are not legacy hosts (players, vanilla mobs) and
 * {@code mixin/LivingEntityDimensionsMixin} substitutes them whenever the game asks for the entity's dimensions. A size equal to the
 * type's own is dropped again, which gives back the poses of the modern game (sneaking, swimming).
 */
public final class EntitySizes1710 {
    private static final Map<Entity, float[]> SIZES = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile boolean any;

    private EntitySizes1710() {}

    /** {@code Entity.setSize} on an entity that is not a host: the box changes now. */
    public static void setSize(Entity entity, float width, float height) {
        EntityDimensions own = entity.getType().getDimensions();
        float[] entry = SIZES.get(entity);
        float eye = entry == null ? Float.NaN : entry[2];
        if (entry != null && entry[0] == width && entry[1] == height) return;
        entry = new float[] {width, height, eye};
        SIZES.put(entity, entry);
        any = true;
        dropIfDefault(entity, entry);
        entity.refreshDimensions();
    }

    /** The {@code eyeHeight} field of a player: the camera height above the feet. */
    public static void setEyeHeight(Entity entity, float eye) {
        float[] entry = SIZES.get(entity);
        if (entry == null) {
            EntityDimensions own = entity.getType().getDimensions();
            if (Math.abs(eye - own.eyeHeight()) < 1.0E-4F) return;
            entry = new float[] {own.width(), own.height(), Float.NaN};
            SIZES.put(entity, entry);
            any = true;
        }
        if (entry[2] == eye) return;
        entry[2] = eye;
        dropIfDefault(entity, entry);
        entity.refreshDimensions();
    }

    private static void dropIfDefault(Entity entity, float[] entry) {
        EntityDimensions own = entity.getType().getDimensions();
        if (Math.abs(entry[0] - own.width()) < 1.0E-4F && Math.abs(entry[1] - own.height()) < 1.0E-4F
                && (Float.isNaN(entry[2]) || Math.abs(entry[2] - own.eyeHeight()) < 1.0E-4F)) {
            SIZES.remove(entity);
        }
    }

    /** What {@code LivingEntity.getDimensions} answers: the size a mod asked for, if any. */
    public static EntityDimensions apply(Entity entity, EntityDimensions original) {
        if (!any) return original;
        float[] entry = SIZES.get(entity);
        if (entry == null) return original;
        float ratio = original.height() > 0 ? original.eyeHeight() / original.height() : 0.9F;
        EntityDimensions out = EntityDimensions.scalable(entry[0], entry[1]);
        return out.withEyeHeight(Float.isNaN(entry[2]) ? entry[1] * ratio : entry[2]);
    }
}
