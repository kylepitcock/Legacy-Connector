package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/**
 * A base image with up to 17 masks drawn over it, each tinted with a dye colour (banner and shield patterns). A mask pixel's
 * coverage is its red channel (1.12's masks) limited by its alpha (1.21's masks are white with an alpha mask).
 */
public class LayeredColorMaskTexture extends AbstractTexture {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("LayeredColorMaskTexture");
    private final ResourceLocation textureLocation;
    private final List<String> listTextures;
    private final List<EnumDyeColor> listDyeColors;

    public LayeredColorMaskTexture(ResourceLocation textureLocationIn, List<String> p_i46101_2_, List<EnumDyeColor> p_i46101_3_) {
        textureLocation = textureLocationIn;
        listTextures = p_i46101_2_;
        listDyeColors = p_i46101_3_;
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        deleteGlTexture();
        BufferedImage base;
        BufferedImage combined;
        try (InputStream in = TextureUtil.open(textureLocation)) {
            base = TextureUtil.readBufferedImage(in);
        }
        if (base == null) {
            LOGGER.error("Couldn't load layered color mask image {}", textureLocation);
            return;
        }
        combined = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        combined.getGraphics().drawImage(base, 0, 0, null);
        for (int j = 0; j < 17 && j < listTextures.size() && j < listDyeColors.size(); j++) {
            String name = listTextures.get(j);
            EnumDyeColor dye = listDyeColors.get(j);
            if (name == null || dye == null) continue;
            int color = dye.getColorValue();
            BufferedImage mask;
            try (InputStream in = TextureUtil.open(new ResourceLocation(name))) {
                mask = TextureUtil.readBufferedImage(in);
            } catch (IOException e) {
                continue; // a pattern the resource packs lack: skipped, the rest still draws
            }
            if (mask == null) continue;
            int w = Math.min(mask.getWidth(), combined.getWidth()), h = Math.min(mask.getHeight(), combined.getHeight());
            BufferedImage tinted = new BufferedImage(combined.getWidth(), combined.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int m = mask.getRGB(x, y);
                    int alpha = Math.min(m >>> 24, (m >> 16) & 255);
                    if (alpha == 0) continue;
                    int rgb = MathHelper.multiplyColor(base.getRGB(x, y), color) & 0xFFFFFF;
                    tinted.setRGB(x, y, alpha << 24 | rgb);
                }
            }
            combined.getGraphics().drawImage(tinted, 0, 0, null);
        }
        TextureUtil.uploadTextureImage(getGlTextureId(), combined);
    }
}
