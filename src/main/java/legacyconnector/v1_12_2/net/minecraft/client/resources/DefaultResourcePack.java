package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The game's own assets ({@code Minecraft.defaultResourcePack}): a view of the vanilla pack (1.12 texture paths mapped to 1.21's). */
public class DefaultResourcePack implements IResourcePack {
    public static final Set<String> DEFAULT_RESOURCE_DOMAINS = Set.of("minecraft", "realms");

    public DefaultResourcePack() {
    }

    private static net.minecraft.server.packs.PackResources vanilla() {
        return net.minecraft.client.Minecraft.getInstance().getVanillaPackResources();
    }

    private static net.minecraft.resources.ResourceLocation modern(ResourceLocation location) {
        return org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations.modern(location.getNamespace(), location.getPath());
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        var supplier = vanilla().getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, modern(location));
        if (supplier == null) throw new FileNotFoundException(location.toString());
        return supplier.get();
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        try {
            return vanilla().getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, modern(location)) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public Set<String> getResourceDomains() {
        return DEFAULT_RESOURCE_DOMAINS;
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) throws IOException {
        var root = vanilla().getRootResource("pack.mcmeta");
        if (root == null) return null;
        try (InputStream in = root.get()) {
            @SuppressWarnings("unchecked")
            T out = (T) MetadataSerializer.mcc$parse(serializer, in.readAllBytes(), section);
            return out;
        }
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        var icon = vanilla().getRootResource("pack.png");
        if (icon == null) return null;
        try (InputStream in = icon.get()) {
            return javax.imageio.ImageIO.read(in);
        }
    }

    @Override
    public String getPackName() {
        return "Default";
    }
}
