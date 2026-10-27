package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A pack read from a file or folder: resources are named {@code assets/<domain>/<path>}. */
public abstract class AbstractResourcePack implements IResourcePack {
    protected static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("LegacyConnector/ResourcePacks");
    protected final File resourcePackFile;

    public AbstractResourcePack(File resourcePackFile) {
        this.resourcePackFile = resourcePackFile;
    }

    private static String locationToName(ResourceLocation location) {
        return String.format("%s/%s/%s", "assets", location.getNamespace(), location.getPath());
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
        LOGGER.warn("ResourcePack: ignored non-lowercase namespace: {} in {}", name, resourcePackFile);
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) throws IOException {
        try (InputStream in = getInputStreamByName("pack.mcmeta")) {
            @SuppressWarnings("unchecked")
            T out = (T) MetadataSerializer.mcc$parse(serializer, in.readAllBytes(), section);
            return out;
        }
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        try (InputStream in = getInputStreamByName("pack.png")) {
            return javax.imageio.ImageIO.read(in);
        }
    }

    @Override
    public String getPackName() {
        return resourcePackFile.getName();
    }

    @Override
    public abstract Set<String> getResourceDomains();
}
