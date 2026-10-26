package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSerializer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** A pack read from a file or folder: assets live under {@code assets/<domain>/<path>}. */
public abstract class AbstractResourcePack implements IResourcePack {
    protected final File resourcePackFile;

    public AbstractResourcePack(File file) {
        this.resourcePackFile = file;
    }

    private static String locationToName(ResourceLocation location) {
        return String.format("%s/%s/%s", "assets", location.getResourceDomain(), location.getResourcePath());
    }

    protected static String getRelativeName(File base, File file) {
        return base.toURI().relativize(file.toURI()).getPath();
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        return getInputStreamByName(locationToName(location));
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return hasResourceName(locationToName(location));
    }

    protected abstract InputStream getInputStreamByName(String name) throws IOException;

    protected abstract boolean hasResourceName(String name);

    protected void logNameNotLowercase(String name) {
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section) throws IOException {
        return null;
    }

    protected static IMetadataSection readMetadata(IMetadataSerializer serializer, InputStream in, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return null;
    }

    @Override
    public String getPackName() {
        return resourcePackFile == null ? "" : resourcePackFile.getName();
    }
}
