package org.kylepitcock.legacyconnector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client entry points for Legacy Connector's own settings: the mod list Config button and a button on the Options screen. */
public final class LegacyConnectorClient {
    private LegacyConnectorClient() {}

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) (c, parent) -> new LegacyConnectorConfigScreen(parent));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            if (e.getScreen() instanceof OptionsScreen screen) {
                e.addListener(Button.builder(Component.literal("Legacy Connector..."),
                        b -> Minecraft.getInstance().setScreen(new LegacyConnectorConfigScreen(screen))).bounds(6, 6, 120, 20).build());
            }
        });
    }
}
