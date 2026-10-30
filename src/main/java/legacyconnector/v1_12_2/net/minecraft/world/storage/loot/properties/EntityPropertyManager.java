package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.properties;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 registry of the entity property types loot conditions understand (vanilla's "on_fire" and the ones mods register). */
public class EntityPropertyManager {
    private static final Map<ResourceLocation, EntityProperty.Serializer<?>> NAME_TO_SERIALIZER_MAP = new HashMap<>();
    private static final Map<Class<? extends EntityProperty>, EntityProperty.Serializer<?>> CLASS_TO_SERIALIZER_MAP = new HashMap<>();

    static {
        registerProperty(new EntityOnFire.Serializer());
    }

    public static <T extends EntityProperty> void registerProperty(EntityProperty.Serializer<? extends T> serializer) {
        ResourceLocation name = serializer.getName();
        Class<T> clazz = (Class<T>) serializer.getPropertyClass();
        synchronized (NAME_TO_SERIALIZER_MAP) {
            if (NAME_TO_SERIALIZER_MAP.containsKey(name)) throw new IllegalArgumentException("Can't re-register entity property name " + name);
            if (CLASS_TO_SERIALIZER_MAP.containsKey(clazz)) throw new IllegalArgumentException("Can't re-register entity property class " + clazz.getName());
            NAME_TO_SERIALIZER_MAP.put(name, serializer);
            CLASS_TO_SERIALIZER_MAP.put(clazz, serializer);
        }
    }

    public static EntityProperty.Serializer<?> getSerializerForName(ResourceLocation name) {
        EntityProperty.Serializer<?> serializer;
        synchronized (NAME_TO_SERIALIZER_MAP) {
            serializer = NAME_TO_SERIALIZER_MAP.get(name);
        }
        if (serializer == null) throw new IllegalArgumentException("Unknown loot entity property type '" + name + "'");
        return serializer;
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityProperty> EntityProperty.Serializer<T> getSerializerFor(T property) {
        EntityProperty.Serializer<?> serializer;
        synchronized (NAME_TO_SERIALIZER_MAP) {
            serializer = CLASS_TO_SERIALIZER_MAP.get(property.getClass());
        }
        if (serializer == null) throw new IllegalArgumentException("Unknown loot entity property " + property);
        return (EntityProperty.Serializer<T>) serializer;
    }
}
