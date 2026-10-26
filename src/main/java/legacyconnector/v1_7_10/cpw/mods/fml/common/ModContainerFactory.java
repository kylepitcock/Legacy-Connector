package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.lang.reflect.Constructor;
import java.util.Map;
import legacyconnector.v1_7_10.cpw.mods.fml.common.discovery.ModCandidate;
import org.objectweb.asm.Type;

/**
 * Makes mod containers for classes carrying a mod annotation. FML knew {@code @Mod}; coremods registered container
 * types for their own annotations (CoFH's {@code @ChildMod}). The layer discovers {@code @Mod} classes itself, so the
 * registered types are kept and offered through {@link #build}, which builds a container the way FML did.
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
            FMLLog.log(org.apache.logging.log4j.Level.ERROR, e, "Critical error : cannot register mod container type %s, it has an invalid constructor", container.getName());
        }
    }

    /** A container of the type registered for {@code annotation} (an ASM type descriptor), or null. */
    public ModContainer build(String annotationDescriptor, String className, ModCandidate candidate, Map<String, Object> values) {
        Constructor<? extends ModContainer> ctor = modTypes.get(Type.getType(annotationDescriptor));
        if (ctor == null) return null;
        try {
            return ctor.newInstance(className, candidate, values);
        } catch (Exception e) {
            FMLLog.log(org.apache.logging.log4j.Level.ERROR, e, "Unable to construct %s container", className);
            return null;
        }
    }
}
