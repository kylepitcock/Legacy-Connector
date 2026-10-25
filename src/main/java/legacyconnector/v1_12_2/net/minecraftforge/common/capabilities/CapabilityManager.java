package legacyconnector.v1_12_2.net.minecraftforge.common.capabilities;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/** Registered capability types and the {@link CapabilityInject} members waiting for them. */
public enum CapabilityManager {
    INSTANCE;

    private final Map<String, Capability<?>> providers = new java.util.IdentityHashMap<>();
    private final Map<String, List<java.lang.reflect.Member>> injections = new HashMap<>();

    public synchronized <T> void register(Class<T> type, Capability.IStorage<T> storage, Callable<? extends T> factory) {
        Capability<T> cap = new Capability<>(type.getName().intern(), storage, factory);
        providers.put(cap.getName(), cap);
        List<java.lang.reflect.Member> list = injections.get(cap.getName());
        if (list != null) for (var m : new ArrayList<>(list)) inject(cap, m);
    }

    public synchronized <T> void register(Class<T> type, Capability.IStorage<T> storage, Class<? extends T> implementation) {
        register(type, storage, () -> {
            var c = implementation.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        });
    }

    /** Called by the platform for each annotated member found in a mod. */
    public synchronized void mcc$addInjection(String capabilityType, java.lang.reflect.Member member) {
        injections.computeIfAbsent(capabilityType, k -> new ArrayList<>()).add(member);
        Capability<?> cap = providers.get(capabilityType.intern());
        if (cap != null) inject(cap, member);
    }

    /** Forge wrote the capability into the mods' public static final fields; so do we. */
    private static void setFinalStatic(Field f, Object value) throws ReflectiveOperationException {
        Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        unsafe.ensureClassInitialized(f.getDeclaringClass());
        unsafe.putObjectVolatile(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    private static void inject(Capability<?> cap, java.lang.reflect.Member member) {
        try {
            if (member instanceof Field f) {
                f.setAccessible(true);
                if (java.lang.reflect.Modifier.isFinal(f.getModifiers())) setFinalStatic(f, cap);
                else f.set(null, cap);
            } else if (member instanceof Method m) {
                m.setAccessible(true);
                m.invoke(null, cap);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Capabilities").warn("Cannot inject the capability {} into {}: {}", cap.getName(), member, e.toString());
        }
    }
}
