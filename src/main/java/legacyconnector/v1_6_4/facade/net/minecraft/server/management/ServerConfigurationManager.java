package legacyconnector.v1_6_4.facade.net.minecraft.server.management;

import legacyconnector.v1_6_4.net.minecraft.util.ChatMessageComponent;

/** Facade of the 1.7.10 shim's {@code ServerConfigurationManager} for 1.5.2 / 1.6.4 mods: chat broadcasts. Never instantiated. */
public final class ServerConfigurationManager {
    private ServerConfigurationManager() {}

    /** ServerConfigurationManager.sendChatMsg(ChatMessageComponent): the message to every player. */
    public static void sendChatMsg(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, ChatMessageComponent message) {
        if (message != null) self.sendChatMsg(message.mcc$toLegacy());
    }

    /** ServerConfigurationManager.areCommandsAllowed(name): operators, and everybody when the world allows cheats (single player, open to LAN). */
    public static boolean areCommandsAllowed(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, String name) {
        if (legacyconnector.v1_6_4.forward.Forward164.isPlayerOpped(self, name)) return true;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null && server.getWorldData().isAllowCommands() && !server.isDedicatedServer();
    }

    /** 1.5.2: ServerConfigurationManager.sendChatMsg(String). */
    public static void sendChatMsg(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, String message) {
        if (message != null) self.sendChatMsg(ChatMessageComponent.createFromText(message).mcc$toLegacy());
    }
}
