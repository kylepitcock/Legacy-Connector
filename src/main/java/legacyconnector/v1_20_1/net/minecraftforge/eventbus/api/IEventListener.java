package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

import net.neoforged.bus.api.Event;

/** Forge 1.20.1's raw listener of one event class. */
@FunctionalInterface
public interface IEventListener {
    void invoke(Event event);
}
