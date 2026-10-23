package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations;

/** Binding a texture just records it in the render context; modern draw calls name their texture explicitly. */
public class TextureManager {
    public static final TextureManager INSTANCE = new TextureManager();

    public void bindTexture(ResourceLocation location) {
        if (location == null) return; // legacy renderers with an unset texture keep the previous one
        RenderContext.bindTexture(TextureLocations.modern(location.getResourceDomain(), location.getResourcePath()));
    }

    /** The block and item atlases (mods look sprites up by name through them); other textures are not tracked. */
    public ITextureObject getTexture(ResourceLocation location) {
        if (location == null) return null;
        String path = location.getResourcePath();
        if (path.equals("textures/atlas/blocks.png")) return org.kylepitcock.legacyconnector.v1_7_10.Assets1710.blockAtlas();
        if (path.equals("textures/atlas/items.png")) return org.kylepitcock.legacyconnector.v1_7_10.Assets1710.itemAtlas();
        return null;
    }

    public ResourceLocation getResourceLocation(int spriteIndex) {
        return new ResourceLocation(spriteIndex == 0 ? "textures/atlas/blocks.png" : "textures/atlas/items.png");
    }
}
