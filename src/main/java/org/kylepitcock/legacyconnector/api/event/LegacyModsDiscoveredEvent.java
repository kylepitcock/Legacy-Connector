package org.kylepitcock.legacyconnector.api.event;

import java.util.List;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.kylepitcock.legacyconnector.api.LegacyMod;

/** Phase {@code DISCOVERED}: all legacy mods are known, none is constructed. Posted on the mod event buses. */
public class LegacyModsDiscoveredEvent extends Event implements IModBusEvent {
    private final List<LegacyMod> mods;

    public LegacyModsDiscoveredEvent(List<LegacyMod> mods) {
        this.mods = List.copyOf(mods);
    }

    /** Every legacy mod, in load order. */
    public List<LegacyMod> getMods() {
        return mods;
    }
}
