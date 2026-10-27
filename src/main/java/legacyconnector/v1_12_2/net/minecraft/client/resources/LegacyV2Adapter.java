package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12's wrapper for packs of the older (format 2) layout; resources are passed through unchanged. */
public class LegacyV2Adapter implements IResourcePack {
    private final IResourcePack pack;

    public LegacyV2Adapter(IResourcePack pack) {
        this.pack = pack;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        return pack.getInputStream(location);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return pack.resourceExists(location);
    }

    @Override
    public Set<String> getResourceDomains() {
        return pack.getResourceDomains();
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) throws IOException {
        return pack.getPackMetadata(serializer, section);
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return pack.getPackImage();
    }

    @Override
    public String getPackName() {
        return pack.getPackName();
    }
}
