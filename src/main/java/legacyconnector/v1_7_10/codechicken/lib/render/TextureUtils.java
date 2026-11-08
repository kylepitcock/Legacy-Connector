package legacyconnector.v1_7_10.codechicken.lib.render;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** CodeChickenLib's texture helpers. */
public class TextureUtils {
    /** Binds the block (0) or item (1) texture atlas. */
    public static void bindAtlas(int atlasIndex) {
        Minecraft.getMinecraft().renderEngine.bindTexture(atlasIndex == 0 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture);
    }

    public static void changeTexture(ResourceLocation texture) {
        Minecraft.getMinecraft().renderEngine.bindTexture(texture);
    }

    public static void changeTexture(String texture) {
        changeTexture(new ResourceLocation(texture));
    }

    /** A sprite of the block atlas by name (the sprite the layer registered for it), or null. */
    public static IIcon getBlockTexture(String name) {
        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
    }
}
