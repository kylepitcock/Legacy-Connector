package legacyconnector.v1_12_2.net.minecraftforge.registries;

/** Forge's {@code @ObjectHolder} injector; mods call {@link #applyObjectHolders} after registering outside the registry events. */
public enum ObjectHolderRegistry {
    INSTANCE;

    public void applyObjectHolders() {
        org.kylepitcock.legacyconnector.v1_12_2.Platform1122.applyObjectHolders();
    }
}
