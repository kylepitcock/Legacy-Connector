package legacyconnector.v1_6_4.facade.net.minecraft.server.management;

<<<<<<< HEAD
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
=======
import com.mojang.authlib.GameProfile;
import java.util.LinkedHashSet;
import java.util.Set;
import legacyconnector.v1_6_4.net.minecraft.util.ChatMessageComponent;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;

/**
 * Facade of the 1.7.10 shim's {@code ServerConfigurationManager} for 1.6.4 and older mods: the operator, whitelist and login members that took player
 * names (1.7.10 asks for profiles), over the server's player list. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class ServerConfigurationManager {
    private ServerConfigurationManager() {}

    private static net.minecraft.server.players.PlayerList list(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self) {
        return self.getServerInstance().mcc$modern().getPlayerList();
    }

    /** The profile of an online player, else the offline-mode profile of the name (what a 1.6.4 server without accounts used). */
    public static GameProfile profileOf(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, String name) {
        var online = list(self).getPlayerByName(name);
        return online != null ? online.getGameProfile() : new GameProfile(net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(name), name);
    }

    public static void removeOp(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, String name) {
        self.removeOp(profileOf(self, name));
    }

    public static void addToWhiteList(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, String name) {
        self.addWhitelistedPlayer(profileOf(self, name));
    }

    public static void removeFromWhitelist(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, String name) {
        self.removePlayerFromWhitelist(profileOf(self, name));
    }

    public static boolean isWhiteListEnabled(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self) {
        return list(self).isUsingWhitelist();
    }

    /** The names on the whitelist. */
    public static Set<String> getWhiteListedPlayers(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self) {
        return new LinkedHashSet<>(java.util.Arrays.asList(list(self).getWhiteListNames()));
    }

    /** The names of the online players, comma separated. */
    public static String getPlayerListAsString(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self) {
        return String.join(", ", list(self).getPlayerNamesArray());
    }

    /** {@code allowUserToConnect(address, name)}: null when the player may join, else the reason shown to them. */
    public static String allowUserToConnect(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, java.net.SocketAddress address,
            String name) {
        var refusal = list(self).canPlayerLogin(address, profileOf(self, name));
        return refusal == null ? null : refusal.getString();
    }

    public static EntityPlayerMP createPlayerForUser(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, String name) {
        return self.createPlayerForUser(profileOf(self, name));
    }

    /** {@code transferPlayerToDimension(player, dimension)}: the same teleport the 1.7.10 form does without a teleporter. */
    public static void transferPlayerToDimension(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, EntityPlayerMP player,
            int dimension) {
        self.transferPlayerToDimension(player, dimension, null);
    }

    /** {@code sendChatMsg(ChatMessageComponent)}: a system message to every player. */
    public static void sendChatMsg(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager self, ChatMessageComponent message) {
        if (message != null) self.sendChatMsg(message.mcc$toLegacy());
    }
>>>>>>> dd1fcea2 (Stubs: server/player-list members (names and profiles), grass plants, chat events as strings, chat formatting names, openGui)
}
