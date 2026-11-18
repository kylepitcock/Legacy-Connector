package legacyconnector.v1_6_4.facade.net.minecraft.client.resources;

/** Facade of the 1.7.10 shim's {@code ResourcePackRepository} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class ResourcePackRepository {
    private ResourcePackRepository() {}

    /** 1.6.4 {@code getResourcePackName()}: the name of the pack the server sent, or the empty string when none is active. */
    public static String getResourcePackName(legacyconnector.v1_7_10.net.minecraft.client.resources.ResourcePackRepository self) {
        legacyconnector.v1_7_10.net.minecraft.client.resources.IResourcePack pack = self.getResourcePackInstance();
        return pack == null ? "" : pack.getPackName();
    }
}
