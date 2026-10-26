package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResource;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * A texture read from one image of the resource manager and uploaded to its own GL texture (blur and clamp from the
 * image's "texture" metadata). Registered with {@link TextureManager#loadTexture}, it becomes a modern texture under its location.
 */
public class SimpleTexture extends AbstractTexture {
    public static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("SimpleTexture");
    protected final ResourceLocation textureLocation;

    public SimpleTexture(ResourceLocation location) {
        this.textureLocation = location;
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
        deleteGlTexture();
        IResource resource = manager.getResource(textureLocation);
        try (InputStream in = resource.getInputStream()) {
            BufferedImage image = javax.imageio.ImageIO.read(in);
            if (image == null) throw new IOException("Not an image: " + textureLocation);
            boolean blur = false;
            boolean clamp = false;
            if (resource.hasMetadata()) {
                try {
                    var section = (legacyconnector.v1_7_10.net.minecraft.client.resources.data.TextureMetadataSection) resource.getMetadata("texture");
                    if (section != null) {
                        blur = section.getTextureBlur();
                        clamp = section.getTextureClamp();
                    }
                } catch (RuntimeException e) {
                    logger.warn("Failed reading metadata of: " + textureLocation, e);
                }
            }
            TextureUtil.uploadTextureImageAllocate(getGlTextureId(), image, blur, clamp);
        }
    }
}
