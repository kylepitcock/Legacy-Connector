package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import legacyconnector.v1_6_4.cpw.mods.fml.common.IPlayerTracker;
import legacyconnector.v1_6_4.cpw.mods.fml.common.network.IConnectionHandler;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet1Login;
import legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLNetworkEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.Player;
import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.6.4's {@code IPlayerTracker}s and {@code IConnectionHandler}s, driven by the 1.7.10 shim's player and connection events on the FML bus:
 * login, logout, dimension change and respawn of a player; joining and leaving a server on the client.
 */
public final class Players164 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Players164");
    private static final List<IPlayerTracker> TRACKERS = new CopyOnWriteArrayList<>();
    private static final List<IConnectionHandler> CONNECTIONS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean LISTENING = new AtomicBoolean();

    private Players164() {}

    /** Target of the redirected {@code GameRegistry.registerPlayerTracker}. */
    public static void registerPlayerTracker(IPlayerTracker tracker) {
        if (tracker == null) return;
        TRACKERS.add(tracker);
        listen();
    }

    public static void registerConnectionHandler(IConnectionHandler handler) {
        if (handler == null) return;
        CONNECTIONS.add(handler);
        listen();
    }

    private static void listen() {
        if (LISTENING.compareAndSet(false, true)) FMLCommonHandler.instance().bus().register(new Listener());
    }

    /** The event bus listener; public because the bus reflects on it. */
    public static final class Listener {
        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
            for (IPlayerTracker t : TRACKERS) guard("onPlayerLogin", () -> t.onPlayerLogin(e.player));
            if (e.player instanceof EntityPlayerMP mp) {
                NetHandlerPlayServer handler = new NetHandlerPlayServer(mp);
                // connectionReceived ran in the login phase of the old protocol and could refuse the player with a message; here it runs as the player joins
                for (IConnectionHandler c : CONNECTIONS) {
                    guard("connectionReceived", () -> {
                        String refusal = c.connectionReceived(null, Manager164.of(handler));
                        if (refusal != null && !refusal.isEmpty()) mp.mcc$serverPlayer().connection.disconnect(net.minecraft.network.chat.Component.literal(refusal));
                    });
                }
                for (IConnectionHandler c : CONNECTIONS) {
                    guard("playerLoggedIn", () -> c.playerLoggedIn((Player) mp, handler, Manager164.of(handler)));
                }
            }
        }

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
            for (IPlayerTracker t : TRACKERS) guard("onPlayerLogout", () -> t.onPlayerLogout(e.player));
            if (e.player instanceof EntityPlayerMP mp) {
                NetHandlerPlayServer handler = new NetHandlerPlayServer(mp);
                for (IConnectionHandler c : CONNECTIONS) guard("connectionClosed", () -> c.connectionClosed(Manager164.of(handler)));
            }
        }

        @SubscribeEvent
        public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
            for (IPlayerTracker t : TRACKERS) guard("onPlayerChangedDimension", () -> t.onPlayerChangedDimension(e.player));
        }

        @SubscribeEvent
        public void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
            for (IPlayerTracker t : TRACKERS) guard("onPlayerRespawn", () -> t.onPlayerRespawn(e.player));
        }

        @SubscribeEvent
        public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent e) {
            for (IConnectionHandler c : CONNECTIONS) {
                // connectionOpened: the old client announced the connection to a remote server (address, port) or to its own integrated server
                guard("connectionOpened", () -> {
                    var manager = Manager164.of(e.manager, NetHandlerPlayClient.INSTANCE);
                    if (e.isLocal) c.connectionOpened(NetHandlerPlayClient.INSTANCE, legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.getServer(), manager);
                    else {
                        String address = ClientAddress.current();
                        int colon = address.lastIndexOf(':');
                        c.connectionOpened(NetHandlerPlayClient.INSTANCE, colon < 0 ? address : address.substring(0, colon), colon < 0 ? 25565 : parsePort(address.substring(colon + 1)), manager);
                    }
                });
                guard("clientLoggedIn", () -> c.clientLoggedIn(NetHandlerPlayClient.INSTANCE, Manager164.of(e.manager, NetHandlerPlayClient.INSTANCE), new Packet1Login()));
            }
        }

        @SubscribeEvent
        public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
            for (IConnectionHandler c : CONNECTIONS) {
                guard("connectionClosed", () -> c.connectionClosed(Manager164.of(e.manager, NetHandlerPlayClient.INSTANCE)));
            }
        }
    }

    private static int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return 25565;
        }
    }

    /** The client's current server address, kept apart so the server side never loads client classes. */
    private static final class ClientAddress {
        static String current() {
            var data = net.minecraft.client.Minecraft.getInstance().getCurrentServer();
            return data == null || data.ip == null ? "" : data.ip;
        }
    }

    private static void guard(String what, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException | LinkageError e) {
            LOG.error("A 1.6.4 player or connection callback ({}) failed", what, e);
        }
    }
}
