package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

/** FML 1.6's record of a networked mod; mods read its numeric network id ({@code FMLNetworkHandler.findNetworkModHandler(mod).getNetworkId()}). */
public class NetworkModHandler {
    private final int networkId;

    private final ITinyPacketHandler tinyHandler;

    public NetworkModHandler(int networkId) {
        this(networkId, null);
    }

    public NetworkModHandler(int networkId, ITinyPacketHandler tinyHandler) {
        this.networkId = networkId;
        this.tinyHandler = tinyHandler;
    }

    /** The {@code tinyPacketHandler} of the mod's {@code @NetworkMod}, or null (CodeChickenCore's PacketCustom checks it when a mod assigns its handler). */
    public ITinyPacketHandler getTinyPacketHandler() {
        return tinyHandler;
    }

    public int getNetworkId() {
        return networkId;
    }
}
