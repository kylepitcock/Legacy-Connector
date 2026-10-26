package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSerializer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** A source of assets by domain and path (mods' own packs are served to the game by LegacyModPacks). */
public interface IResourcePack {
    InputStream getInputStream(ResourceLocation location) throws IOException;

    boolean resourceExists(ResourceLocation location);

    Set getResourceDomains();

    IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section) throws IOException;

    BufferedImage getPackImage() throws IOException;

    String getPackName();
}
