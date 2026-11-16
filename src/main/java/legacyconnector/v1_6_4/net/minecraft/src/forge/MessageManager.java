package legacyconnector.v1_6_4.net.minecraft.src.forge;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164;

/**
 * Forge 3.x's channel registry over custom payload packets: a handler registered for a channel receives every payload that arrives on it. The
 * layer's own payload dispatch ({@code Network164}) carries them, so one registration serves every connection.
 */
public class MessageManager {
    private static final MessageManager INSTANCE = new MessageManager();
    private final Map<String, List<IPacketHandler>> handlers = new ConcurrentHashMap<>();

    public static MessageManager getInstance() {
        return INSTANCE;
    }

    public boolean registerChannel(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, IPacketHandler handler, String channel) {
        if (handler == null || channel == null) return false;
        List<IPacketHandler> list = handlers.computeIfAbsent(channel, c -> {
            Network164.registerChannel((manager, packet, player) -> dispatch(manager, packet.channel, packet.data), c, (Side) null);
            return new CopyOnWriteArrayList<>();
        });
        if (!list.contains(handler)) list.add(handler);
        return true;
    }

    private void dispatch(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager manager, String channel, byte[] data) {
        List<IPacketHandler> list = handlers.get(channel);
        if (list == null) return;
        for (IPacketHandler h : list) h.onPacketData(manager, channel, data);
    }

    public boolean unregisterChannel(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, IPacketHandler handler, String channel) {
        List<IPacketHandler> list = handlers.get(channel);
        return list != null && list.remove(handler);
    }

    public String[] unregisterHandler(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, IPacketHandler handler) {
        java.util.List<String> removed = new java.util.ArrayList<>();
        for (Map.Entry<String, List<IPacketHandler>> e : handlers.entrySet()) if (e.getValue().remove(handler)) removed.add(e.getKey());
        return removed.toArray(new String[0]);
    }

    public String[] getRegisteredChannels(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network) {
        return handlers.keySet().toArray(new String[0]);
    }

    public IPacketHandler[] getChannelHandlers(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, String channel) {
        List<IPacketHandler> list = handlers.get(channel);
        return list == null ? new IPacketHandler[0] : list.toArray(new IPacketHandler[0]);
    }

    public String[] removeConnection(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network) {
        return new String[0];
    }

    public void addActiveChannel(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, String channel) {}

    public void removeActiveChannel(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, String channel) {}

    public boolean isActiveChannel(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, String channel) {
        return handlers.containsKey(channel);
    }

    public void dispatchIncomingMessage(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, String channel, byte[] data) {
        dispatch(network, channel, data);
    }
}
