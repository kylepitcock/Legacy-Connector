package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

import org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164;

/** FML 1.6's network handler, as far as mods use it: the singleton and the lookup of a mod's network record. */
public final class FMLNetworkHandler {
    private static final FMLNetworkHandler INSTANCE = new FMLNetworkHandler();

    private FMLNetworkHandler() {}

    public static FMLNetworkHandler instance() {
        return INSTANCE;
    }

    /** The network record of a mod (given its instance or its id), or null when it is not a legacy mod. */
    public NetworkModHandler findNetworkModHandler(Object mod) {
        int id = Network164.networkId(mod);
        return id < 0 ? null : new NetworkModHandler(id);
    }
}
