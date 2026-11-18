package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

/** Reads one section of a .mcmeta file ({@code "pack"}, {@code "animation"}, a mod's own): a Gson deserializer with the section's name. */
public interface IMetadataSectionSerializer extends com.google.gson.JsonDeserializer {
    String getSectionName();
}
