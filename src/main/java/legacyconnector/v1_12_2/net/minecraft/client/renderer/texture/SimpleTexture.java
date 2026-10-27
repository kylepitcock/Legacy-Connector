package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * A texture read from one image of the client's resources into its own GL texture. Registered with {@link TextureManager#loadTexture}
 * (or bound by location, which the modern texture manager loads by itself).
 */
public class SimpleTexture extends AbstractTexture {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("SimpleTexture");
    protected final ResourceLocation textureLocation;

    public SimpleTexture(ResourceLocation textureResourceLocation) {
        textureLocation = textureResourceLocation;
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        deleteGlTexture();
        try (InputStream in = TextureUtil.open(textureLocation)) {
            BufferedImage image = TextureUtil.readBufferedImage(in);
            if (image == null) throw new IOException("Not an image: " + textureLocation);
            boolean blur = false, clamp = false;
            try {
                var rl = org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations.modern(textureLocation.getNamespace(), textureLocation.getPath());
                var resource = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(rl);
                if (resource.isPresent()) {
                    var section = resource.get().metadata().getSection(net.minecraft.client.resources.metadata.texture.TextureMetadataSection.SERIALIZER);
                    if (section.isPresent()) {
                        blur = section.get().isBlur();
                        clamp = section.get().isClamp();
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Failed reading metadata of: {}", textureLocation, e);
            }
            TextureUtil.uploadTextureImageAllocate(getGlTextureId(), image, blur, clamp);
        }
    }
}
