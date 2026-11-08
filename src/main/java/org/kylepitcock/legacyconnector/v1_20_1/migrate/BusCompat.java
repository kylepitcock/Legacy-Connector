package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;

/** {@code Mod.EventBusSubscriber.Bus.bus()} of Forge 1.20.1 (called through {@code mc-calls.tsv}); NeoForge's {@code Bus} enum has no such method. */
public final class BusCompat {
    private BusCompat() {}

    public static Supplier<IEventBus> bus(EventBusSubscriber.Bus bus) {
        if (bus == EventBusSubscriber.Bus.MOD) return () -> ModLoadingContext.get().getActiveContainer().getEventBus();
        return () -> NeoForge.EVENT_BUS;
    }
}
