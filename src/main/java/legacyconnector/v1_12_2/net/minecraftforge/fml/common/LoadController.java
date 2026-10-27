package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * FML's loading state machine. Only its per-mod event buses are used by mods: {@code eventChannels} maps a mod id to the Guava bus
 * that delivers that mod's lifecycle events (preInit, init, ...), and libraries reach it by reflection through
 * {@code Loader.modController} to register their own lifecycle handlers for a mod (SuperMartijn642's Core Lib).
 */
public class LoadController {
    /** Mod id to the container's lifecycle bus, read live (containers are created while mods are discovered). */
    private final Map<String, com.google.common.eventbus.EventBus> eventChannels = new AbstractMap<>() {
        @Override
        public com.google.common.eventbus.EventBus get(Object key) {
            ModContainer c = key instanceof String id ? Loader.instance().getIndexedModList().get(id) : null;
            return c instanceof org.kylepitcock.legacyconnector.v1_12_2.LegacyModContainer1122 legacy ? legacy.mcc$bus() : null;
        }

        @Override
        public Set<Entry<String, com.google.common.eventbus.EventBus>> entrySet() {
            Map<String, com.google.common.eventbus.EventBus> out = new LinkedHashMap<>();
            for (ModContainer c : Loader.instance().getModList()) {
                if (c instanceof org.kylepitcock.legacyconnector.v1_12_2.LegacyModContainer1122 legacy) out.put(c.getModId(), legacy.mcc$bus());
            }
            return out.entrySet();
        }
    };

    /** The container whose event is being delivered. */
    public ModContainer activeContainer() {
        return Loader.instance().activeModContainer();
    }

    public LoaderState getState() {
        return Loader.instance().getLoaderState();
    }
}
