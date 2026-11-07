package legacyconnector.v1_6_4.facade.net.minecraft.client.network;

import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Manager164;

/** Facade of the 1.7.10 shim's {@code NetHandlerPlayClient} for 1.6.4 mods ({@code NetClientHandler}): the connection as an {@link INetworkManager}. Never instantiated. */
public final class NetHandlerPlayClient {
    private NetHandlerPlayClient() {}

    /** NetClientHandler.getNetManager() */
    public static INetworkManager getNetManager(legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient self) {
        return Manager164.of(self);
    }

    /** NetClientHandler.netManager */
    public static INetworkManager mcc$get$netManager(legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient self) {
        return Manager164.of(self);
    }
}
