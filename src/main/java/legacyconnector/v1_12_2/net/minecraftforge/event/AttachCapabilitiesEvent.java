package legacyconnector.v1_12_2.net.minecraftforge.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.GenericEvent;

public class AttachCapabilitiesEvent<T> extends GenericEvent<T> {
    private final T object;
    private final Map<ResourceLocation, ICapabilityProvider> caps = new LinkedHashMap<>();

    public AttachCapabilitiesEvent(Class<T> type, T object) {
        super(type);
        this.object = object;
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
}
