package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The loot function types by name and by class. */
public class LootFunctionManager {
    private static final Map<ResourceLocation, LootFunction.Serializer<?>> NAME_TO_SERIALIZER_MAP = new HashMap<>();
    private static final Map<Class<?>, LootFunction.Serializer<?>> CLASS_TO_SERIALIZER_MAP = new HashMap<>();

    public static synchronized <T extends LootFunction> void registerFunction(LootFunction.Serializer<? extends T> serializer) {
        ResourceLocation name = serializer.getFunctionName();
        Class<?> clazz = serializer.getFunctionClass();
        if (NAME_TO_SERIALIZER_MAP.containsKey(name)) throw new IllegalArgumentException("Can't re-register item function name " + name);
        if (CLASS_TO_SERIALIZER_MAP.containsKey(clazz)) throw new IllegalArgumentException("Can't re-register item function class " + clazz.getName());
        NAME_TO_SERIALIZER_MAP.put(name, serializer);
        CLASS_TO_SERIALIZER_MAP.put(clazz, serializer);
    }

    public static synchronized LootFunction.Serializer<?> getSerializerForName(ResourceLocation location) {
        LootFunction.Serializer<?> serializer = NAME_TO_SERIALIZER_MAP.get(location);
        if (serializer == null) throw new IllegalArgumentException("Unknown loot item function '" + location + "'");
        return serializer;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends LootFunction> LootFunction.Serializer<T> getSerializerFor(T function) {
        LootFunction.Serializer<?> serializer = function == null ? null : CLASS_TO_SERIALIZER_MAP.get(function.getClass());
        if (serializer == null) throw new IllegalArgumentException("Unknown loot item function " + function);
        return (LootFunction.Serializer<T>) serializer;
    }
}
