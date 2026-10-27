package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A resource pack: files under {@code assets/<domain>/}, its pack.mcmeta and icon. */
public interface IResourcePack {
    InputStream getInputStream(ResourceLocation location) throws IOException;

    boolean resourceExists(ResourceLocation location);

    Set<String> getResourceDomains();

    <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) throws IOException;

    BufferedImage getPackImage() throws IOException;

    String getPackName();
}
