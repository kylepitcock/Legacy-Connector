package legacyconnector.v1_12_2.net.minecraft.client.resources;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** One resource file from a pack: its contents, which pack it came from and whether it has an {@code .mcmeta} file. */
public interface IResource extends java.io.Closeable {
    ResourceLocation getResourceLocation();

    java.io.InputStream getInputStream();

    boolean hasMetadata();

    String getResourcePackName();

    /** A section of the resource's {@code .mcmeta} file, or null (resources of the game's own packs answer null). */
    default <T extends legacyconnector.v1_12_2.net.minecraft.client.resources.data.IMetadataSection> T getMetadata(String sectionName) {
        return null;
    }
}
