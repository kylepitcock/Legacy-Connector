package legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher;

public enum Side {
    CLIENT, SERVER;

    public boolean isServer() {
        return this == SERVER;
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
