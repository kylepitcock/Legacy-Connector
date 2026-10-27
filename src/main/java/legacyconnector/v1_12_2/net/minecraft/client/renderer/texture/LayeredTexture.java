package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Images drawn over each other into one texture (the first one sets the size), as horse armour layers were. */
public class LayeredTexture extends AbstractTexture {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("LayeredTexture");
    public final List<String> layeredTextureNames;

    public LayeredTexture(String... textureNames) {
        layeredTextureNames = com.google.common.collect.Lists.newArrayList(textureNames);
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        deleteGlTexture();
        BufferedImage combined = null;
        for (String name : layeredTextureNames) {
            if (name == null) continue;
            try (InputStream in = TextureUtil.open(new ResourceLocation(name))) {
                BufferedImage layer = TextureUtil.readBufferedImage(in);
                if (layer == null) continue;
                if (combined == null) combined = new BufferedImage(layer.getWidth(), layer.getHeight(), BufferedImage.TYPE_INT_ARGB);
                combined.getGraphics().drawImage(layer, 0, 0, null);
            }
        }
        if (combined == null) {
            LOGGER.error("Couldn't load layered image {}", layeredTextureNames);
            return;
        }
        TextureUtil.uploadTextureImage(getGlTextureId(), combined);
    }
}
