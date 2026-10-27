package legacyconnector.v1_12_2.net.minecraft.client.resources;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** One resource file from a pack: its contents, which pack it came from and whether it has an {@code .mcmeta} file. */
public interface IResource extends java.io.Closeable {
    ResourceLocation getResourceLocation();

    java.io.InputStream getInputStream();

    boolean hasMetadata();

    String getResourcePackName();
}
