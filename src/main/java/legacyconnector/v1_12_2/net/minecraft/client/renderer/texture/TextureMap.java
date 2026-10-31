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
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Textures");
    private static TextureMap blocks;
    /** Set once the modern block atlas is built: mod-created atlases are stitched from then on (their sprites are registered by then). */
    private static volatile boolean atlasesReady;

    private boolean isBlocks;
    private ResourceLocation mcc$location;

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
        if (blocks == null) {
            TextureMap map = new TextureMap("textures");
            map.isBlocks = true;
            blocks = map;
        }
        return blocks;
    }

    protected void initMissingImage() {
        if (isBlocks) {
            missingImage.mcc$sync(Sprites1122.sprite("missingno"));
            return;
        }
        // an atlas of a mod's own: the missing texture is a stitched sprite like the others
        missingImage.mcc$own();
        missingImage.setIconWidth(16);
        missingImage.setIconHeight(16);
        missingImage.setFramesTextureData(java.util.Collections.singletonList(new int[][] {TextureUtil.MISSING_TEXTURE_DATA.clone()}));
    }

    /** True for the shim's block atlas (backed by the modern one), false for atlases mods create themselves. */
    public boolean mcc$isBlocks() {
        return isBlocks;
    }

    /** Called when the texture manager registers this atlas under a location. */
    public void mcc$setLocation(ResourceLocation location) {
        mcc$location = location;
    }

    public static boolean mcc$atlasesReady() {
        return atlasesReady;
    }

    public static void mcc$setAtlasesReady() {
        atlasesReady = true;
    }

    /** The modern atlas is loaded by the game; legacy code that asks to load this map only gets its populator run. */
    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        if (!isBlocks) {
            initMissingImage();
            deleteGlTexture();
            loadTextureAtlas(resourceManager);
            return;
        }
        if (iTextureMapPopulator != null) loadSprites(resourceManager, iTextureMapPopulator);
    }

    public void loadSprites(IResourceManager resourceManager, ITextureMapPopulator iconCreatorIn) {
        mapRegisteredSprites.clear();
        iconCreatorIn.registerSprites(this);
        initMissingImage();
    }

    public void loadTextureAtlas(IResourceManager resourceManager) {
        if (isBlocks) {
            mcc$synchronize();
            return;
        }
        stitchOwn(resourceManager);
    }

    /** 1.12's TextureMap.loadTextureAtlas for an atlas a mod created: stitches the registered sprite files into a GL texture of its own. */
    private void stitchOwn(IResourceManager resourceManager) {
        int max = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getGLMaximumTextureSize();
        Stitcher stitcher = new Stitcher(max, max, 0, mipmapLevels);
        mapUploadedSprites.clear();
        listAnimatedSprites.clear();
        try {
            legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.TextureStitchEvent.Pre(this));
        } catch (RuntimeException | LinkageError e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("posting TextureStitchEvent.Pre", e);
        }
        for (Map.Entry<String, TextureAtlasSprite> entry : Lists.newArrayList(mapRegisteredSprites.entrySet())) {
            TextureAtlasSprite sprite = entry.getValue();
            sprite.mcc$own();
            ResourceLocation location = getResourceLocation(sprite);
            try {
                if (sprite.hasCustomLoader(resourceManager, location)) {
                    if (!sprite.load(resourceManager, location, l -> mapRegisteredSprites.get(l.toString()))) stitcher.addSprite(sprite);
                    continue;
                }
                legacyconnector.v1_12_2.net.minecraft.client.resources.IResource resource = resourceManager.getResource(location);
                PngSizeInfo size = PngSizeInfo.makeFromResource(resource);
                sprite.loadSprite(size, false);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Atlas {}: unable to load {}: {}", mcc$location, location, e.toString());
                continue;
            }
            stitcher.addSprite(sprite);
        }
        missingImage.generateMipmaps(mipmapLevels);
        stitcher.addSprite(missingImage);
        try {
            stitcher.doStitch();
        } catch (RuntimeException e) {
            LOGGER.warn("Atlas {} could not be stitched", mcc$location, e);
            return;
        }
        int width = stitcher.getCurrentWidth(), height = stitcher.getCurrentHeight();
        TextureUtil.allocateTextureImpl(getGlTextureId(), mipmapLevels, width, height);
        for (TextureAtlasSprite sprite : stitcher.getStichSlots()) {
            if (sprite != missingImage) {
                try {
                    sprite.loadSpriteFrames(resourceManager.getResource(getResourceLocation(sprite)), mipmapLevels + 1);
                    sprite.generateMipmaps(mipmapLevels);
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("Atlas {}: unable to read frames of {}: {}", mcc$location, sprite.getIconName(), e.toString());
                    continue;
                }
            }
            mapRegisteredSprites.remove(sprite.getIconName());
            mapUploadedSprites.put(sprite.getIconName(), sprite);
            try {
                TextureUtil.uploadTextureMipmap(sprite.getFrameTextureData(0), sprite.getIconWidth(), sprite.getIconHeight(), sprite.getOriginX(), sprite.getOriginY(), false, false);
            } catch (RuntimeException e) {
                LOGGER.warn("Atlas {}: unable to upload {}: {}", mcc$location, sprite.getIconName(), e.toString());
            }
        }
        for (TextureAtlasSprite sprite : mapRegisteredSprites.values()) sprite.copyFrom(missingImage);
        try {
            legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.TextureStitchEvent.Post(this));
        } catch (RuntimeException | LinkageError e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("posting TextureStitchEvent.Post", e);
        }
    }

    /** One log line per built custom atlas; the size is read back from the GL texture when the mod stitched it itself. */
    public void mcc$logBuilt(int width, int height) {
        if (isBlocks) return;
        if (width <= 0 || height <= 0) {
            try {
                int id = getGlTextureId();
                if (id > 0) {
                    legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.bindTexture(id);
                    width = org.lwjgl.opengl.GL11C.glGetTexLevelParameteri(3553, 0, 4096);
                    height = org.lwjgl.opengl.GL11C.glGetTexLevelParameteri(3553, 0, 4097);
                }
            } catch (RuntimeException | LinkageError e) {
                // size unknown
            }
        }
        int sprites;
        synchronized (this) {
            sprites = mapUploadedSprites.size() + mapRegisteredSprites.size();
        }
        LOGGER.info("1.12.2 custom atlas {}: {} sprites, {}x{}", mcc$location, sprites, width, height);
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
                if (!isBlocks) return getMissingSprite();
                sprite = new TextureAtlasSprite(iconName);
                mapUploadedSprites.put(iconName, sprite);
            }
        }
        if (isBlocks) sprite.mcc$modern();
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
            if (isBlocks) Sprites1122.register(name);
            else sprite.mcc$own();
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
        if (isBlocks) Sprites1122.register(name);
        else entry.mcc$own();
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
        if (!isBlocks) return super.getGlTextureId();
        var atlas = Sprites1122.blockAtlas();
        return atlas == null ? 0 : atlas.getId();
    }

    @Override
    public void deleteGlTexture() {
        if (!isBlocks) super.deleteGlTexture();
    }

    /** The legacy sprite of a modern block-atlas sprite (named by the modern id), synchronized with it. */
    public TextureAtlasSprite mcc$spriteOf(net.minecraft.client.renderer.texture.TextureAtlasSprite modern) {
        String name = modern.contents().name().toString();
        TextureAtlasSprite sprite;
        synchronized (this) {
            sprite = mapUploadedSprites.get(name);
            if (sprite == null) sprite = mapRegisteredSprites.get(name);
            if (sprite == null) {
                sprite = new TextureAtlasSprite(name);
                mapUploadedSprites.put(name, sprite);
            }
        }
        if (sprite.mcc$modernIfSynced() != modern) sprite.mcc$sync(modern);
        return sprite;
    }

    /** After a modern atlas build: every sprite legacy code holds takes the position of its modern sprite. */
    public void mcc$synchronize() {
        if (!isBlocks) return;
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
