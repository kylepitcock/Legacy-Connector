package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Forge 1.20.1's event for registering a mod's own capability interfaces. Forge needed it to create the capability objects; here they exist as soon as
 * {@code CapabilityManager.get} names them, so the event only has to be fired for the mods that listen to it.
 */
public class RegisterCapabilitiesEvent extends Event implements IModBusEvent {
    public void register(Class<?> type) {
        // the capability exists once named
    }
}
