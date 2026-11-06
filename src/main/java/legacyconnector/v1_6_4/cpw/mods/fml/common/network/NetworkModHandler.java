package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

/** FML 1.6's record of a networked mod; mods read its numeric network id ({@code FMLNetworkHandler.findNetworkModHandler(mod).getNetworkId()}). */
public class NetworkModHandler {
    private final int networkId;

    public NetworkModHandler(int networkId) {
        this.networkId = networkId;
    }

    public int getNetworkId() {
        return networkId;
    }
}
