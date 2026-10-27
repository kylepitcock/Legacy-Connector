package legacyconnector.v1_12_2.net.minecraft.client.resources.data;

/** Reads one kind of {@code .mcmeta} section from its JSON. */
public interface IMetadataSectionSerializer<T extends IMetadataSection> extends com.google.gson.JsonDeserializer<T> {
    String getSectionName();
}
