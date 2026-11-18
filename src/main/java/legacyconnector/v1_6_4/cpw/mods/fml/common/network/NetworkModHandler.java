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

    private legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer container;
    private NetworkMod annotation;

    /** FML's record of a mod and its {@code @NetworkMod} (the layer assigns the numeric network id when it registers the mod's channels). */
    public NetworkModHandler(legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer container, NetworkMod annotation) {
        this(-1, null);
        this.container = container;
        this.annotation = annotation;
    }

    /** The container of the mod this record is for. */
    public legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer getContainer() {
        return container;
    }

    /** Binds the record to its mod's container (FML did this when it had built the record before the container existed). */
    public void configureNetworkMod(legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer container) {
        this.container = container;
    }

    /** Whether a client without the mod cannot join ({@code clientSideRequired}). */
    public boolean requiresClientSide() {
        return annotation != null && annotation.clientSideRequired();
    }

    /** Whether a server without the mod cannot be joined ({@code serverSideRequired}). */
    public boolean requiresServerSide() {
        return annotation != null && annotation.serverSideRequired();
    }

    /** Whether the remote mod version fits the annotation's {@code versionBounds} (an empty bound accepts any; a bound that does not parse accepts too). */
    public boolean acceptVersion(String version) {
        String bounds = annotation == null ? "" : annotation.versionBounds();
        if (bounds == null || bounds.isEmpty() || version == null) return true;
        try {
            return legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.VersionRange.createFromVersionSpec(bounds)
                    .containsVersion(new legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.DefaultArtifactVersion(version));
        } catch (RuntimeException | legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.InvalidVersionSpecificationException e) {
            return true;
        }
    }

    /** The {@code tinyPacketHandler} of the mod's {@code @NetworkMod}, or null (CodeChickenCore's PacketCustom checks it when a mod assigns its handler). */
    public ITinyPacketHandler getTinyPacketHandler() {
        return tinyHandler;
    }

    public int getNetworkId() {
        return networkId;
    }
}
