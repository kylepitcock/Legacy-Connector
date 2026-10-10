package mccompat.v1_7_10.net.minecraft.network;

/** Opaque connection handle passed to legacy callbacks; carries no API surface. */
public class NetworkManager {
    public static final NetworkManager MCC_DUMMY = new NetworkManager();

    public boolean isLocalChannel() {
        return false;
    }
}
