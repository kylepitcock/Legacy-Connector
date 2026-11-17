package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.lang.reflect.Constructor;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.ModCandidate;
import org.objectweb.asm.Type;

/**
 * Makes mod containers for classes carrying a mod annotation. FML knew {@code @Mod}; mods and coremods registered container types for
 * annotations of their own. The layer discovers {@code @Mod} classes itself, so the registered types are kept and offered through
 * {@link #build}, which constructs a container the way FML did.
 */
public class ModContainerFactory {
    private static final ModContainerFactory INSTANCE = new ModContainerFactory();
    private final Map<Type, Constructor<? extends ModContainer>> modTypes = new java.util.concurrent.ConcurrentHashMap<>();

    public static ModContainerFactory instance() {
        return INSTANCE;
    }

    public void registerContainerType(Type type, Class<? extends ModContainer> container) {
        try {
            modTypes.put(type, container.getConstructor(String.class, ModCandidate.class, Map.class));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/ModContainerFactory")
                    .error("Cannot register mod container type {}: it has no (String, ModCandidate, Map) constructor", container.getName(), e);
        }
    }

    /** A container of the type registered for {@code annotationDescriptor} (an ASM type descriptor), or null. */
    public ModContainer build(String annotationDescriptor, String className, ModCandidate candidate, Map<String, Object> values) {
        Constructor<? extends ModContainer> ctor = modTypes.get(Type.getType(annotationDescriptor));
        if (ctor == null) return null;
        try {
            return ctor.newInstance(className, candidate, values);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/ModContainerFactory").error("Unable to construct {} container", className, e);
            return null;
        }
    }
}
