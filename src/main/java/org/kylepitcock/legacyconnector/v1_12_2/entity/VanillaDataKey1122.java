package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.function.BiConsumer;
import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializer;

/**
 * A data key vanilla 1.12.2 declared on its own entity classes (EntityLivingBase.HEALTH, EntityItem.ITEM, ...). Its value lives in the
 * modern entity, so {@code dataManager.get/set} with it read and write the modern state instead of a legacy entry; the id is vanilla's,
 * so mod keys created after it never collide.
 */
public final class VanillaDataKey1122<T> extends DataParameter<T> {
    private final Class<? extends Entity> owner;
    private final Function<Entity, T> getter;
    private final BiConsumer<Entity, T> setter;
    private final T fallback;

    private VanillaDataKey1122(int id, DataSerializer<T> serializer, Class<? extends Entity> owner, T fallback, Function<Entity, T> getter,
            BiConsumer<Entity, T> setter) {
        super(id, serializer);
        this.owner = owner;
        this.getter = getter;
        this.setter = setter;
        this.fallback = fallback;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Entity, T> DataParameter<T> of(int id, DataSerializer<T> serializer, Class<E> owner, T fallback, Function<E, T> getter,
            BiConsumer<E, T> setter) {
        return new VanillaDataKey1122<>(id, serializer, owner, fallback, (Function<Entity, T>) getter, (BiConsumer<Entity, T>) setter);
    }

    /** Whether the entity's value comes from the modern state (an entity of the declaring class with a modern counterpart). */
    public boolean handles(Entity entity) {
        return entity != null && entity.mcc$entity != null && owner.isInstance(entity);
    }

    public T read(Entity entity) {
        try {
            T value = getter.apply(entity);
            return value != null ? value : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public void write(Entity entity, T value) {
        if (setter == null) return;
        try {
            setter.accept(entity, value);
        } catch (RuntimeException e) {
            // a value the modern entity cannot take
        }
    }
}
