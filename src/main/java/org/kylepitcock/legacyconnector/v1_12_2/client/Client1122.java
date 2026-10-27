package org.kylepitcock.legacyconnector.v1_12_2.client;

import net.neoforged.bus.api.IEventBus;

/** Client-only registrations for 1.12.2 content that are not rendering: container screens and key bindings. */
public final class Client1122 {
    private Client1122() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientGui1122::onRegisterScreens);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) -> {
            for (Object key : legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding.mcc$modernMappings()) {
                try {
                    e.register((net.minecraft.client.KeyMapping) key);
                } catch (RuntimeException ex) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/Keys").warn("Could not register key binding {}", ((net.minecraft.client.KeyMapping) key).getName(), ex);
                }
            }
        });
    }
}
