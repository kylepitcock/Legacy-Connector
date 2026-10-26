package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.Type;

/**
 * FML's factory of mod containers by mod annotation (language adapters register their own {@code @Mod}-like annotation
 * here). Registrations are kept, but mod discovery already ran in the loader before any mod code, so they add no mods.
 */
public class ModContainerFactory {
    public static Map<Type, Constructor<? extends ModContainer>> modTypes = new ConcurrentHashMap<>();
    private static final ModContainerFactory INSTANCE = new ModContainerFactory();

    public static ModContainerFactory instance() {
        return INSTANCE;
    }

    public void registerContainerType(Type type, Class<? extends ModContainer> container) {
        if (type == null || container == null) return;
        for (Constructor<?> c : container.getConstructors()) {
            if (c.getParameterCount() == 3) {
                @SuppressWarnings("unchecked")
                Constructor<? extends ModContainer> ctor = (Constructor<? extends ModContainer>) c;
                modTypes.put(type, ctor);
                return;
            }
        }
    }
}
