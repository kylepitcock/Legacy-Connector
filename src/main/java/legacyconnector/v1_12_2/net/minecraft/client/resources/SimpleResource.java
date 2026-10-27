package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.io.IOException;
import java.io.InputStream;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A resource of a pack with its stream and (optional) {@code .mcmeta} stream, read once on the first metadata request. */
public class SimpleResource implements IResource {
    private final String resourcePackName;
    private final ResourceLocation srResourceLocation;
    private final InputStream resourceInputStream;
    protected final InputStream mcmetaInputStream;
    private final MetadataSerializer srMetadataSerializer;
    private byte[] mcmeta;
    private boolean mcmetaRead;

    public SimpleResource(String packName, ResourceLocation location, InputStream resource, InputStream mcmeta, MetadataSerializer serializer) {
        this.resourcePackName = packName;
        this.srResourceLocation = location;
        this.resourceInputStream = resource;
        this.mcmetaInputStream = mcmeta;
        this.srMetadataSerializer = serializer;
    }

    @Override
    public ResourceLocation getResourceLocation() {
        return srResourceLocation;
    }

    @Override
    public InputStream getInputStream() {
        return resourceInputStream;
    }

    @Override
    public boolean hasMetadata() {
        return mcmetaInputStream != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T extends IMetadataSection> T getMetadata(String sectionName) {
        if (!hasMetadata()) return null;
        if (!mcmetaRead) {
            mcmetaRead = true;
            try {
                mcmeta = mcmetaInputStream.readAllBytes();
            } catch (IOException e) {
                mcmeta = null;
            } finally {
                try {
                    mcmetaInputStream.close();
                } catch (IOException ignored) {
                    // already read
                }
            }
        }
        return mcmeta == null ? null : (T) MetadataSerializer.mcc$parse(srMetadataSerializer, mcmeta, sectionName);
    }

    @Override
    public String getResourcePackName() {
        return resourcePackName;
    }

    @Override
    public void close() throws IOException {
        resourceInputStream.close();
        if (mcmetaInputStream != null) mcmetaInputStream.close();
    }
}
