package legacyconnector.v1_7_10.cpw.mods.fml.relauncher;

public enum Side {
    CLIENT, SERVER;

    public boolean isServer() {
        return this == SERVER;
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
