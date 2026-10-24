package legacyconnector.v1_7_10.cpw.mods.fml.common.network;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;

/** FML network events; the custom packet ones are posted to {@link FMLEventChannel} buses. */
public class FMLNetworkEvent<T extends INetHandler> extends Event {
    public final T handler;
    public final Object manager = null;

    FMLNetworkEvent(T handler) {
        this.handler = handler;
    }

    public static class CustomPacketEvent<S extends INetHandler> extends FMLNetworkEvent<S> {
        public final FMLProxyPacket packet;
        /** Set by a handler to answer the sender. */
        public FMLProxyPacket reply;
        private final Side side;

        CustomPacketEvent(S handler, FMLProxyPacket packet, Side side) {
            super(handler);
            this.packet = packet;
            this.side = side;
        }

        public Side side() {
            return side;
        }
    }

    public static class ClientCustomPacketEvent extends CustomPacketEvent<INetHandler> {
        public ClientCustomPacketEvent(Object manager, FMLProxyPacket packet) {
            super(legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient.INSTANCE, packet, Side.CLIENT);
        }
    }

    /** A player connected to this (integrated or dedicated) server. */
    public static class ServerConnectionFromClientEvent extends FMLNetworkEvent<INetHandler> {
        public final legacyconnector.v1_7_10.net.minecraft.network.NetworkManager manager;

        public ServerConnectionFromClientEvent(INetHandler handler, legacyconnector.v1_7_10.net.minecraft.network.NetworkManager manager) {
            super(handler);
            this.manager = manager;
        }
    }

    /** A player left this server. */
    public static class ServerDisconnectionFromClientEvent extends FMLNetworkEvent<INetHandler> {
        public final legacyconnector.v1_7_10.net.minecraft.network.NetworkManager manager;

        public ServerDisconnectionFromClientEvent(INetHandler handler, legacyconnector.v1_7_10.net.minecraft.network.NetworkManager manager) {
            super(handler);
            this.manager = manager;
        }
    }

    public static class ServerCustomPacketEvent extends CustomPacketEvent<INetHandler> {
        public ServerCustomPacketEvent(Object manager, FMLProxyPacket packet, INetHandler handler) {
            super(handler, packet, Side.SERVER);
        }
    }
}
