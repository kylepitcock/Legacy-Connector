package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

import legacyconnector.v1_7_10.cpw.mods.fml.common.network.IGuiHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164;

/**
 * FML 1.6's network registry: custom payload channels with {@link IPacketHandler}s, connection handlers and GUI handlers. Channels and
 * connection handlers are kept by {@code bridge.Network164}; GUI handlers are the 1.7.10 shim's (same interface, same ids).
 */
public final class NetworkRegistry {
    private static final NetworkRegistry INSTANCE = new NetworkRegistry();

    private NetworkRegistry() {}

    public static NetworkRegistry instance() {
        return INSTANCE;
    }

    /** Registers {@code handler} for {@code channelName} on both sides. */
    public void registerChannel(IPacketHandler handler, String channelName) {
        Network164.registerChannel(handler, channelName, null);
    }

    /** Registers {@code handler} for {@code channelName}, for packets arriving on {@code side} only. */
    public void registerChannel(IPacketHandler handler, String channelName, Side side) {
        Network164.registerChannel(handler, channelName, side);
    }

    public void registerConnectionHandler(IConnectionHandler handler) {
        Network164.registerConnectionHandler(handler);
    }

    public void registerGuiHandler(Object mod, IGuiHandler handler) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(mod, handler);
    }
}
