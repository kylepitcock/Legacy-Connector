package legacyconnector.v1_20_1.net.minecraftforge.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

/**
 * Forge 1.20.1's event for attaching capability providers to block entities, entities, stacks and levels. The runtime fires it once per object
 * ({@code Caps1201.attach}); the providers it collects answer the object's {@code getCapability} calls.
 */
public class AttachCapabilitiesEvent<T> extends Event {
    private final T object;
    private final Map<ResourceLocation, ICapabilityProvider> caps = new LinkedHashMap<>();

    public AttachCapabilitiesEvent(T object) {
        this.object = object;
    }

    private Class<?> type;

    /** Forge's constructor: the generic type (what the event is for) and the object; mods fire the event themselves for their own objects. */
    public AttachCapabilitiesEvent(Class<?> type, T object) {
        this(object);
        this.type = type;
    }

    public Class<?> getGenericType() {
        return type != null ? type : object == null ? Object.class : object.getClass();
    }

    public T getObject() {
        return object;
    }

    public void addCapability(ResourceLocation key, ICapabilityProvider cap) {
        if (caps.containsKey(key)) throw new IllegalStateException("Duplicate Capability Key: " + key + " " + cap);
        caps.put(key, cap);
    }

    public Map<ResourceLocation, ICapabilityProvider> getCapabilities() {
        return Collections.unmodifiableMap(caps);
    }

    public void addListener(Runnable listener) {
        // invalidation listeners are not supported: providers live as long as their object
    }
}
