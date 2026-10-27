package legacyconnector.v1_12_2.net.minecraft.client.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.network.NetworkManager;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/**
 * The client's connection to the server ({@code Minecraft.getConnection()}, {@code player.connection}): one handle over the game's
 * connection. Packets mods send go out as their modern counterparts (mod channel payloads through the channel bridge); the tab list
 * entries are views of the game's.
 */
public class NetHandlerPlayClient implements legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient {
    public static final NetHandlerPlayClient mcc$INSTANCE = new NetHandlerPlayClient(null, null, null, null);
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Network");
    private static final java.util.Set<String> UNSENDABLE = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Phantom: the server's player limit (redirected to {@link #mcc$get$currentServerMaxPlayers}). */
    @Deprecated
    public int currentServerMaxPlayers;
    /** The screen to return to when the connection closes. */
    public legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen guiScreenServer;
    private final com.mojang.authlib.GameProfile profile;
    private final legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ClientAdvancementManager advancementManager =
            new legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ClientAdvancementManager(null);

    /** A handler a mod makes itself (fake client worlds): it talks to the game's connection. */
    public NetHandlerPlayClient(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen screen,
            NetworkManager manager, com.mojang.authlib.GameProfile profile) {
        this.guiScreenServer = screen;
        this.profile = profile;
    }

    private static net.minecraft.client.multiplayer.ClientPacketListener modern() {
        return net.minecraft.client.Minecraft.getInstance().getConnection();
    }

    /** Sends a packet to the server: mod channel payloads through the channel bridge, vanilla packets as their modern equivalent. */
    public void sendPacket(Packet<?> packet) {
        if (packet == null) return;
        if (packet instanceof legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket fml) {
            org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122.send(fml,
                    legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.FMLOutboundHandler.OutboundTarget.TOSERVER, null);
            return;
        }
        var connection = modern();
        var modernPacket = packet.mcc$modern();
        if (modernPacket != null && connection != null) {
            connection.send(modernPacket);
        } else if (modernPacket == null && UNSENDABLE.add(packet.getClass().getName())) {
            LOG.warn("A legacy {} has no modern counterpart and is not sent", packet.getClass().getName());
        }
    }

    public NetworkManager getNetworkManager() {
        var connection = modern();
        return connection == null ? null : NetworkManager.mcc$of(connection.getConnection());
    }

    public com.mojang.authlib.GameProfile getGameProfile() {
        if (profile != null) return profile;
        var connection = modern();
        return connection == null ? net.minecraft.client.Minecraft.getInstance().getGameProfile() : connection.getLocalGameProfile();
    }

    /** The tab list entries of the players on the server. */
    public Collection<NetworkPlayerInfo> getPlayerInfoMap() {
        List<NetworkPlayerInfo> out = new ArrayList<>();
        var connection = modern();
        if (connection == null) return out;
        for (var info : connection.getOnlinePlayers()) out.add(NetworkPlayerInfo.mcc$of(info));
        return out;
    }

    public NetworkPlayerInfo getPlayerInfo(UUID id) {
        var connection = modern();
        return connection == null || id == null ? null : NetworkPlayerInfo.mcc$of(connection.getPlayerInfo(id));
    }

    public NetworkPlayerInfo getPlayerInfo(String name) {
        var connection = modern();
        return connection == null || name == null ? null : NetworkPlayerInfo.mcc$of(connection.getPlayerInfo(name));
    }

    public legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ClientAdvancementManager getAdvancementManager() {
        return advancementManager;
    }

    /** The integrated server's limit in single player; servers do not tell the client theirs any more (20, 1.12's default). */
    public static int mcc$get$currentServerMaxPlayers(NetHandlerPlayClient h) {
        var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        return server != null ? server.getMaxPlayers() : 20;
    }

    public static void mcc$set$currentServerMaxPlayers(NetHandlerPlayClient h, int value) {
        // the server owns it
    }

    /** A chat packet a mod hands to the client: chat and system messages go to the chat, game info to the action bar. */
    public void handleChat(legacyconnector.v1_12_2.net.minecraft.network.play.server.SPacketChat packet) {
        if (packet == null || packet.getChatComponent() == null) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        var text = packet.getChatComponent().mcc$modern();
        Runnable show = () -> {
            if (packet.getType() == legacyconnector.v1_12_2.net.minecraft.util.text.ChatType.GAME_INFO) mc.gui.setOverlayMessage(text, false);
            else mc.gui.getChat().addMessage(text);
        };
        if (mc.isSameThread()) show.run();
        else mc.execute(show);
    }

    /** The connection was closed: leaves the server with the reason. */
    @Override
    public void onDisconnect(ITextComponent reason) {
        var connection = modern();
        if (connection != null) {
            connection.getConnection().disconnect(reason == null ? net.minecraft.network.chat.Component.empty() : reason.mcc$modern());
        }
    }
}
