package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal;

/** FML's per-mod network record. Mods reach it through {@code NetworkRegistry.registry}; every mod is accepted by vanilla clients. */
public class NetworkModHolder {
    public boolean testVanillaAcceptance() {
        return true;
    }

    public boolean acceptVersion(String version) {
        return true;
    }

    public String getRemoteVersion() {
        return "";
    }
}
