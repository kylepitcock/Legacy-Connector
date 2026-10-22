package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.io.InputStream;

public interface IResource {
    InputStream getInputStream();

    boolean hasMetadata();

    /** A .mcmeta section ("animation", "texture", ...); mods' resources are served without any. */
    default legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSection getMetadata(String name) {
        return null;
    }

    String getResourcePackName();
}
