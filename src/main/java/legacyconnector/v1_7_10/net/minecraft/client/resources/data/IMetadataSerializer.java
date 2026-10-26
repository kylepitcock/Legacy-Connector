package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

/** Reads metadata sections (pack.mcmeta, .png.mcmeta); the modern game reads its own, so legacy packs get none from here. */
public class IMetadataSerializer {
    public void registerMetadataSectionType(Object serializer, Class<?> type) {
    }

    public IMetadataSection parseMetadataSection(String section, com.google.gson.JsonObject json) {
        return null;
    }
}
