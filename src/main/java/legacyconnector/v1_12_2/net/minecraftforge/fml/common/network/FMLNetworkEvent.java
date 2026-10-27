package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import legacyconnector.v1_12_2.net.minecraft.network.INetHandler;
import legacyconnector.v1_12_2.net.minecraft.network.NetworkManager;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/** FML's network events: custom packets (posted on their {@link FMLEventChannel}'s bus) and connections (on the game bus). */
public class FMLNetworkEvent<T extends INetHandler> extends Event {
    private final T handler;
    private final NetworkManager manager;
    private final Class<T> type;

    FMLNetworkEvent(T thing, Class<T> type, NetworkManager manager) {
        this.handler = thing;
        this.type = type;
        this.manager = manager;
    }

    public T getHandler() {
        return handler;
    }

    public NetworkManager getManager() {
        return manager;
    }

    public Class<T> getHandlerType() {
        return type;
    }

    public static class ClientConnectedToServerEvent extends FMLNetworkEvent<INetHandler> {
        private final boolean isLocal;
        private final String connectionType;

        public ClientConnectedToServerEvent(NetworkManager manager, String connectionType) {
            super(legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl.MessageContext.CLIENT_HANDLER, INetHandler.class, manager);
            this.isLocal = manager != null && manager.isLocalChannel();
            this.connectionType = connectionType;
        }

        public boolean isLocal() {
            return isLocal;
        }

        public String getConnectionType() {
            return connectionType;
        }
    }

    public static class ServerConnectionFromClientEvent extends FMLNetworkEvent<INetHandler> {
        private final boolean isLocal;

        public ServerConnectionFromClientEvent(INetHandler handler, NetworkManager manager) {
            super(handler, INetHandler.class, manager);
            this.isLocal = manager != null && manager.isLocalChannel();
        }

        public boolean isLocal() {
            return isLocal;
        }
    }

    public static class ServerDisconnectionFromClientEvent extends FMLNetworkEvent<INetHandler> {
        public ServerDisconnectionFromClientEvent(INetHandler handler, NetworkManager manager) {
            super(handler, INetHandler.class, manager);
        }
    }

    public static class ClientDisconnectionFromServerEvent extends FMLNetworkEvent<INetHandler> {
        public ClientDisconnectionFromServerEvent(NetworkManager manager) {
            super(legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl.MessageContext.CLIENT_HANDLER, INetHandler.class, manager);
        }
    }

    public static class CustomPacketEvent<S extends INetHandler> extends FMLNetworkEvent<S> {
        private final FMLProxyPacket packet;
        /** Set by a handler to answer the sender. */
        private FMLProxyPacket reply;

        CustomPacketEvent(S handler, Class<S> type, NetworkManager manager, FMLProxyPacket packet) {
            super(handler, type, manager);
            this.packet = packet;
        }

        public FMLProxyPacket getPacket() {
            return packet;
        }

        public FMLProxyPacket getReply() {
            return reply;
        }

        public void setReply(FMLProxyPacket reply) {
            this.reply = reply;
        }

        public Side side() {
            return packet.handler() instanceof legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer ? Side.SERVER : Side.CLIENT;
        }
    }

    public static class ClientCustomPacketEvent extends CustomPacketEvent<INetHandler> {
        public ClientCustomPacketEvent(NetworkManager manager, FMLProxyPacket packet) {
            super(legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl.MessageContext.CLIENT_HANDLER, INetHandler.class, manager, packet);
        }
    }

    public static class ServerCustomPacketEvent extends CustomPacketEvent<INetHandler> {
        public ServerCustomPacketEvent(NetworkManager manager, FMLProxyPacket packet) {
            super(packet.handler(), INetHandler.class, manager, packet);
        }
    }
}
