package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;
import legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_7_10.net.minecraft.network.NetworkManager;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** A 1.6.4 {@link INetworkManager} over the 1.7.10 shim's connection and handler: one per connection, sending through its handler. */
public final class Manager164 implements INetworkManager {
    private static final Map<NetworkManager, Manager164> BY_CONNECTION = Collections.synchronizedMap(new WeakHashMap<>());

    private final NetworkManager connection;
    private INetHandler handler;

    private Manager164(NetworkManager connection, INetHandler handler) {
        this.connection = connection;
        this.handler = handler;
    }

    /** The manager of the connection {@code handler} belongs to. */
    public static Manager164 of(INetHandler handler) {
        NetworkManager connection = null;
        if (handler instanceof NetHandlerPlayServer s) connection = s.getNetworkManager();
        else if (handler instanceof NetHandlerPlayClient c) connection = c.getNetworkManager();
        return of(connection, handler);
    }

    public static Manager164 of(NetworkManager connection, INetHandler handler) {
        if (connection == null || connection == NetworkManager.MCC_DUMMY) return new Manager164(connection, handler);
        Manager164 m = BY_CONNECTION.computeIfAbsent(connection, c -> new Manager164(c, handler));
        if (m.handler == null) m.handler = handler;
        return m;
    }

    @Override
    public void setNetHandler(INetHandler handler) {
        this.handler = handler;
    }

    @Override
    public void addToSendQueue(Packet packet) {
        if (packet == null) return;
        if (handler instanceof NetHandlerPlayServer s) s.sendPacket(packet);
        else NetHandlerPlayClient.INSTANCE.addToSendQueue(packet);
    }

    @Override
    public void wakeThreads() {
    }

    @Override
    public void processReadPackets() {
    }

    @Override
    public SocketAddress getSocketAddress() {
        return connection == null ? null : connection.getSocketAddress();
    }

    @Override
    public void serverShutdown() {
        closeConnections();
    }

    @Override
    public int packetSize() {
        return 0;
    }

    @Override
    public void networkShutdown(String reason, Object... args) {
        if (connection != null) connection.closeChannel(new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText(reason));
    }

    @Override
    public void closeConnections() {
        if (connection != null) connection.closeChannel(null);
    }
}
