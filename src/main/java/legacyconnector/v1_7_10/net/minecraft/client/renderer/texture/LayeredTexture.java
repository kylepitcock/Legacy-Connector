package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** Images drawn over each other into one texture (the first one sets the size), as horse armour layers are. */
public class LayeredTexture extends AbstractTexture {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("LayeredTexture");
    public final List layeredTextureNames;

    public LayeredTexture(String... textureNames) {
        layeredTextureNames = com.google.common.collect.Lists.newArrayList(textureNames);
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
        deleteGlTexture();
        BufferedImage combined = null;
        for (Object o : layeredTextureNames) {
            String name = (String) o;
            if (name == null) continue;
            try (InputStream in = manager.getResource(new ResourceLocation(name)).getInputStream()) {
                BufferedImage layer = javax.imageio.ImageIO.read(in);
                if (layer == null) continue;
                if (combined == null) combined = new BufferedImage(layer.getWidth(), layer.getHeight(), BufferedImage.TYPE_INT_ARGB);
                combined.getGraphics().drawImage(layer, 0, 0, null);
            } catch (IOException e) {
                LOGGER.error("Couldn't load layered image " + name, e);
                return;
            }
        }
        if (combined == null) {
            LOGGER.error("Couldn't load layered image " + layeredTextureNames);
            return;
        }
        TextureUtil.uploadTextureImageAllocate(getGlTextureId(), combined, false, false);
    }
}
