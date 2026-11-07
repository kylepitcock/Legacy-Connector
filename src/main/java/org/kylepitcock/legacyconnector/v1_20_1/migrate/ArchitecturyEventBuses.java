package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Optional;
import java.util.function.Consumer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * {@code dev.architectury.platform.forge.EventBuses} of Architectury 9 (1.20.1). Architectury 13 on NeoForge asks the mod container for the bus
 * ({@code EventBusesHooks}), so a mod no longer registers it; registering is accepted and ignored, the other calls read the container.
 * The translator renames the class here ({@code lib-classes.tsv}).
 */
public final class ArchitecturyEventBuses {
    private ArchitecturyEventBuses() {
    }

    public static void registerModEventBus(String modId, IEventBus bus) {
        // nothing to do: the mod container owns the bus
    }

    public static void onRegistered(String modId, Consumer<IEventBus> action) {
        getModEventBus(modId).ifPresent(action);
    }

    public static Optional<IEventBus> getModEventBus(String modId) {
        return ModList.get().getModContainerById(modId).map(c -> c.getEventBus());
    }
}
