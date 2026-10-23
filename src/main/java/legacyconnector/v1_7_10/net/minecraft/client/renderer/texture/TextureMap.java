package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.Assets1710;

/**
 * The icon register mods see while registering block (type 0) and item (type 1) icons. Names become modern sprite ids
 * (see Assets1710.texture); sprites mods create themselves ({@link #setTextureEntry}) are kept for pack generation.
 */
public class TextureMap implements IIconRegister, ITextureObject {
    public static final ResourceLocation locationBlocksTexture = new ResourceLocation("textures/atlas/blocks.png");
    public static final ResourceLocation locationItemsTexture = new ResourceLocation("textures/atlas/items.png");

    protected final int textureType;
    protected final String basePath;
    protected Map mapRegisteredSprites = new LinkedHashMap();
    protected int mipmapLevels;
    protected int anisotropicFiltering = 1;

    public TextureMap(int textureType, String basePath) {
        this(textureType, basePath, false);
    }

    public TextureMap(int textureType, String basePath, boolean skipFirst) {
        this.textureType = textureType;
        this.basePath = basePath;
    }

    private String id(String name) {
        return Assets1710.texture(name, textureType == 0);
    }

    @Override
    public synchronized IIcon registerIcon(String name) {
        if (name == null) throw new IllegalArgumentException("Name cannot be null!");
        TextureAtlasSprite sprite = (TextureAtlasSprite) mapRegisteredSprites.get(name);
        if (sprite == null) {
            sprite = TextureAtlasSprite.makeAtlasSprite(id(name));
            mapRegisteredSprites.put(name, sprite);
        }
        return sprite;
    }

    public synchronized boolean setTextureEntry(String name, TextureAtlasSprite sprite) {
        if (mapRegisteredSprites.containsKey(name)) return false;
        sprite.iconName = id(name);
        mapRegisteredSprites.put(name, sprite);
        return true;
    }

    public TextureAtlasSprite getAtlasSprite(String name) {
        TextureAtlasSprite sprite = (TextureAtlasSprite) mapRegisteredSprites.get(name);
        if (sprite != null) return sprite;
        // an icon some other code registered (or a vanilla one): name it the way registerIcon would
        try {
            return TextureAtlasSprite.makeAtlasSprite(id(name));
        } catch (RuntimeException e) {
            return TextureAtlasSprite.makeAtlasSprite("minecraft:missingno");
        }
    }

    public TextureAtlasSprite getTextureExtry(String name) {
        return (TextureAtlasSprite) mapRegisteredSprites.get(name);
    }

    @Override
    public void loadTexture(legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager manager) {
    }

    @Override
    public int getGlTextureId() {
        return 0;
    }

    public int getTextureType() {
        return textureType;
    }

    /** The sprites mods registered, by their legacy name. */
    public synchronized Map<String, TextureAtlasSprite> mcc$sprites() {
        return new LinkedHashMap<String, TextureAtlasSprite>(mapRegisteredSprites);
    }

    public String mcc$basePath() {
        return basePath;
    }
}
