package legacyconnector.v1_6_4.net.minecraft.network;

import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * 1.6.4's connection handle, as packet handlers and connection handlers receive it. {@code NetHandler} is 1.7.10's
 * {@link INetHandler} and {@code Packet} its {@link Packet} here (the loader's class renames), so this interface
 * is the only 1.6.4-shaped type of a connection; the implementation is {@code bridge.Manager164}.
 */
public interface INetworkManager {
    void setNetHandler(INetHandler handler);

    void addToSendQueue(Packet packet);

    void wakeThreads();

    void processReadPackets();

    java.net.SocketAddress getSocketAddress();

    void serverShutdown();

    int packetSize();

    void networkShutdown(String reason, Object... args);

    void closeConnections();
}
