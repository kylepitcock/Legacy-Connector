package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Forge 1.20.1's capability registry: {@code CapabilityManager.get(new CapabilityToken<IFoo>(){})}. */
public final class CapabilityManager {
    private static final Map<String, Capability<?>> CAPABILITIES = new ConcurrentHashMap<>();

    private CapabilityManager() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Capability<T> get(CapabilityToken<T> token) {
        ensureBuiltins();
        String type = token.getType();
        return (Capability<T>) CAPABILITIES.computeIfAbsent(type, Capability::new);
    }

    private static boolean builtins;

    private static void ensureBuiltins() {
        if (!builtins) {
            builtins = true;
            // touching the class registers the capabilities NeoForge has counterparts for
            ForgeCapabilities.ENERGY.getName();
        }
    }

    /** Capabilities that NeoForge has a counterpart for are put in by {@link ForgeCapabilities}. */
    static <T> Capability<T> builtin(String type, Capability<T> cap) {
        CAPABILITIES.put(type, cap);
        return cap;
    }
}
