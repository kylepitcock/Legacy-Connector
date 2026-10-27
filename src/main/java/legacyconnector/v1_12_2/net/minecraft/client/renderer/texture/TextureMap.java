package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.Sprites1122;

/**
 * The 1.12.2 block atlas as legacy code sees it: a registry of sprites by name. The modern block atlas does the stitching
 * (registered sprites become atlas sources, see {@link Sprites1122}); after it is built every sprite held here is synchronized with
 * its modern counterpart. Binding {@link #LOCATION_BLOCKS_TEXTURE} binds the modern block atlas.
 */
public class TextureMap extends AbstractTexture implements ITickableTextureObject {
    public static final ResourceLocation LOCATION_MISSING_TEXTURE = new ResourceLocation("missingno");
    public static final ResourceLocation LOCATION_BLOCKS_TEXTURE = new ResourceLocation("textures/atlas/blocks.png");
    private static TextureMap blocks;

    protected final List<TextureAtlasSprite> listAnimatedSprites = Lists.newArrayList();
    protected final Map<String, TextureAtlasSprite> mapRegisteredSprites = Maps.newHashMap();
    protected final Map<String, TextureAtlasSprite> mapUploadedSprites = Maps.newHashMap();
    protected final String basePath;
    protected final ITextureMapPopulator iTextureMapPopulator;
    protected int mipmapLevels;
    protected final TextureAtlasSprite missingImage = new TextureAtlasSprite("missingno");

    public TextureMap(String basePathIn) {
        this(basePathIn, null);
    }

    public TextureMap(String basePathIn, boolean skipFirst) {
        this(basePathIn, null, skipFirst);
    }

    public TextureMap(String basePathIn, ITextureMapPopulator iconCreatorIn) {
        this(basePathIn, iconCreatorIn, false);
    }

    public TextureMap(String basePathIn, ITextureMapPopulator iconCreatorIn, boolean skipFirst) {
        basePath = basePathIn;
        iTextureMapPopulator = iconCreatorIn;
    }

    /** The block atlas (Minecraft.getTextureMapBlocks()). */
    public static synchronized TextureMap mcc$blocks() {
        if (blocks == null) blocks = new TextureMap("textures");
        return blocks;
    }

    protected void initMissingImage() {
        missingImage.mcc$sync(Sprites1122.sprite("missingno"));
    }

    /** The modern atlas is loaded by the game; legacy code that asks to load this map only gets its populator run. */
    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        if (iTextureMapPopulator != null) loadSprites(resourceManager, iTextureMapPopulator);
    }

    public void loadSprites(IResourceManager resourceManager, ITextureMapPopulator iconCreatorIn) {
        mapRegisteredSprites.clear();
        iconCreatorIn.registerSprites(this);
        initMissingImage();
    }

    public void loadTextureAtlas(IResourceManager resourceManager) {
        mcc$synchronize();
    }

    public ResourceLocation getResourceLocation(ResourceLocation location, int level) {
        return level == 0 ? new ResourceLocation(location.getNamespace(), String.format("%s/%s%s", basePath, location.getPath(), ".png"))
                : new ResourceLocation(location.getNamespace(), String.format("%s/mipmaps/%s.%d%s", basePath, location.getPath(), level, ".png"));
    }

    private ResourceLocation getResourceLocation(TextureAtlasSprite sprite) {
        return getResourceLocation(new ResourceLocation(sprite.getIconName()), 0);
    }

    /**
     * The sprite of a name: the registered one, or one made on demand showing the modern sprite of that name (the missing texture's if
     * the atlas has none).
     */
    public TextureAtlasSprite getAtlasSprite(String iconName) {
        TextureAtlasSprite sprite;
        synchronized (this) {
            sprite = mapUploadedSprites.get(iconName);
            if (sprite == null) sprite = mapRegisteredSprites.get(iconName);
            if (sprite == null) {
                if (iconName == null || iconName.equals("missingno")) return missingImage;
                sprite = new TextureAtlasSprite(iconName);
                mapUploadedSprites.put(iconName, sprite);
            }
        }
        sprite.mcc$modern();
        return sprite;
    }

    public void updateAnimations() {
    }

    public TextureAtlasSprite registerSprite(ResourceLocation location) {
        if (location == null) throw new IllegalArgumentException("Location cannot be null!");
        String name = location.toString();
        synchronized (this) {
            TextureAtlasSprite sprite = mapRegisteredSprites.get(name);
            if (sprite == null) {
                sprite = mapUploadedSprites.get(name);
                if (sprite == null) sprite = TextureAtlasSprite.makeAtlasSprite(location);
                mapRegisteredSprites.put(name, sprite);
            }
            Sprites1122.register(name);
            return sprite;
        }
    }

    @Override
    public void tick() {
    }

    public void setMipmapLevels(int mipmapLevelsIn) {
        mipmapLevels = mipmapLevelsIn;
    }

    public TextureAtlasSprite getMissingSprite() {
        if (missingImage.getMaxU() == 0.0F) initMissingImage();
        return missingImage;
    }

    public TextureAtlasSprite getTextureExtry(String name) {
        synchronized (this) {
            TextureAtlasSprite sprite = mapRegisteredSprites.get(name);
            return sprite != null ? sprite : mapUploadedSprites.get(name);
        }
    }

    /** Adds a sprite object of the mod's own class (custom loaders): it is synchronized with the modern sprite of its name. */
    public boolean setTextureEntry(TextureAtlasSprite entry) {
        String name = entry.getIconName();
        synchronized (this) {
            if (mapRegisteredSprites.containsKey(name)) return false;
            mapRegisteredSprites.put(name, entry);
        }
        Sprites1122.register(name);
        return true;
    }

    public String getBasePath() {
        return basePath;
    }

    public int getMipmapLevels() {
        return mipmapLevels;
    }

    @Override
    public int getGlTextureId() {
        var atlas = Sprites1122.blockAtlas();
        return atlas == null ? 0 : atlas.getId();
    }

    @Override
    public void deleteGlTexture() {
    }

    /** After a modern atlas build: every sprite legacy code holds takes the position of its modern sprite. */
    public void mcc$synchronize() {
        var atlas = Sprites1122.blockAtlas();
        if (atlas == null) return;
        List<TextureAtlasSprite> sprites;
        synchronized (this) {
            sprites = new java.util.ArrayList<>(mapRegisteredSprites.values());
            sprites.addAll(mapUploadedSprites.values());
        }
        for (TextureAtlasSprite sprite : sprites) {
            try {
                sprite.mcc$sync(atlas.getSprite(Sprites1122.modernName(sprite.getIconName())));
            } catch (RuntimeException e) {
                org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("placing a legacy sprite", e);
            }
        }
        initMissingImage();
    }
}
