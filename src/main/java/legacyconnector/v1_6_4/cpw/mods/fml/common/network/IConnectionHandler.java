package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import legacyconnector.v1_6_4.net.minecraft.network.NetLoginHandler;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet1Login;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.Player;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/**
 * FML 1.6's connection callbacks. Called here: {@code playerLoggedIn} when a player joins the server, {@code clientLoggedIn} when this
 * client joins a server, {@code connectionClosed} when either leaves. {@code connectionReceived} and {@code connectionOpened} belong to
 * the login phase of the old protocol, which the modern connection does not expose to mods; they are never called.
 */
public interface IConnectionHandler {
    void playerLoggedIn(Player player, INetHandler netHandler, INetworkManager manager);

    String connectionReceived(NetLoginHandler netHandler, INetworkManager manager);

    void connectionOpened(INetHandler netClientHandler, String server, int port, INetworkManager manager);

    void connectionOpened(INetHandler netClientHandler, MinecraftServer server, INetworkManager manager);

    void connectionClosed(INetworkManager manager);

    void clientLoggedIn(INetHandler clientHandler, INetworkManager manager, Packet1Login login);
}
