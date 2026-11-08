package org.kylepitcock.legacyconnector.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client entry point for Legacy Connector's own settings: the Config button of the mod list. (No button on the Options screen.) */
public final class LegacyConnectorClient {
    private LegacyConnectorClient() {}

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) (c, parent) -> new LegacyConnectorConfigScreen(parent));
    }
}
