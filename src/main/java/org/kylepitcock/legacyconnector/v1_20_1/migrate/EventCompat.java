package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * Forge 1.20.1 event accessors whose NeoForge 1.21.1 counterparts changed shape (called through {@code mc-calls.tsv}):
 * game events and attributes became registry holders, and the eye height moved into {@link EntityDimensions}.
 * Found by the contributor kentvillamor08's target-API verifier run on a real 1.20.1 mod.
 */
public final class EventCompat {
    private EventCompat() {}

    // ---- VanillaGameEvent ----

    /** {@code VanillaGameEvent.getVanillaEvent()} returned the {@link GameEvent}; 1.21.1 returns its holder. */
    public static GameEvent vanillaEvent(VanillaGameEvent event) {
        return event.getVanillaEvent().value();
    }

    // ---- EntityAttributeModificationEvent ----

    private static Holder<Attribute> holder(Attribute attribute) {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }

    public static void attributeAdd(EntityAttributeModificationEvent event, EntityType<? extends LivingEntity> type, Attribute attribute, double value) {
        event.add(type, holder(attribute), value);
    }

    public static void attributeAdd(EntityAttributeModificationEvent event, EntityType<? extends LivingEntity> type, Attribute attribute) {
        event.add(type, holder(attribute));
    }

    public static boolean attributeHas(EntityAttributeModificationEvent event, EntityType<? extends LivingEntity> type, Attribute attribute) {
        return event.has(type, holder(attribute));
    }

    // ---- EntityEvent.Size ----

    public static float oldEyeHeight(EntityEvent.Size event) {
        return event.getOldSize().eyeHeight();
    }

    public static float newEyeHeight(EntityEvent.Size event) {
        return event.getNewSize().eyeHeight();
    }

    public static void setNewEyeHeight(EntityEvent.Size event, float eyeHeight) {
        event.setNewSize(event.getNewSize().withEyeHeight(eyeHeight));
    }

    /**
     * {@code setNewSize(size, updateEyeHeight)}: with the flag the eye height is scaled with the height change like
     * Forge 1.20.1 did; without it the previous new eye height is kept.
     */
    public static void setNewSize(EntityEvent.Size event, EntityDimensions size, boolean updateEyeHeight) {
        if (updateEyeHeight) {
            EntityDimensions old = event.getOldSize();
            float eye = old.height() > 0 ? old.eyeHeight() * (size.height() / old.height()) : size.eyeHeight();
            event.setNewSize(size.withEyeHeight(eye));
        } else {
            event.setNewSize(size.withEyeHeight(event.getNewSize().eyeHeight()));
        }
    }
}
