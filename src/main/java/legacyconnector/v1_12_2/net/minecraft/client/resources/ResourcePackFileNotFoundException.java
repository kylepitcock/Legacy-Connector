package legacyconnector.v1_12_2.net.minecraft.client.resources;

/** A file a resource pack was asked for is not in it. */
public class ResourcePackFileNotFoundException extends java.io.FileNotFoundException {
    public ResourcePackFileNotFoundException(java.io.File resourcePack, String file) {
        super(String.format("'%s' in ResourcePack '%s'", file, resourcePack));
    }
}
