package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.network.NetworkManager;

/** The client's end of the connection to the server, as legacy code sees it (client only). */
public final class ClientNetwork1710 {
    private ClientNetwork1710() {}

    /** The legacy view of the client's connection, or null when not connected. */
    public static NetworkManager manager() {
        var listener = net.minecraft.client.Minecraft.getInstance().getConnection();
        return listener == null ? null : NetworkManager.mcc$of(listener.getConnection());
    }

    /** Sends a modern serverbound packet over the client's connection (dropped when not connected). */
    public static void send(net.minecraft.network.protocol.Packet<?> packet) {
        var listener = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (listener != null) listener.send(packet);
    }
}
