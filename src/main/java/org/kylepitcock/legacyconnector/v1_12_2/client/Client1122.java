package org.kylepitcock.legacyconnector.v1_12_2.client;

import net.neoforged.bus.api.IEventBus;

/** Client-only registrations for 1.12.2 content that are not rendering: container screens, key bindings and particles. */
public final class Client1122 {
    private Client1122() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientGui1122::onRegisterScreens);
        LegacyParticles1122.register();
        ScreenEvents1122.register();
        // 1.12 told its resource reload listeners again after every resource reload
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e) -> e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
                    if (legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourceManager()
                            instanceof legacyconnector.v1_12_2.net.minecraft.client.resources.SimpleReloadableResourceManager m) m.mcc$reloaded();
                }));
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
