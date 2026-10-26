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
public class TextureMap implements IIconRegister, ITickableTextureObject {
    public static final ResourceLocation locationBlocksTexture = new ResourceLocation("textures/atlas/blocks.png");
    public static final ResourceLocation locationItemsTexture = new ResourceLocation("textures/atlas/items.png");

    /** Mods attach log filters to it (INpureCore silences the stitching messages). */
    public static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("TextureMap");

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
        if (textureType != 0 && textureType != 1) MOD_MAPS.put(textureType, this);
    }

    /**
     * Atlases mods made themselves, by texture type (Fisk's Heroes' suit icons, in textures/items/heroes): the items whose
     * sprite number names one register their icons in it. Their textures are stitched into the modern atlas with the
     * mod's other item and block textures, so only their names differ.
     */
    private static final Map<Integer, TextureMap> MOD_MAPS = new java.util.concurrent.ConcurrentHashMap<>();

    /** The atlas of a sprite number: a mod's own, or the item atlas as 1.7.10 used for any type but blocks. */
    public static TextureMap mcc$forSpriteNumber(int spriteNumber, TextureMap blocks, TextureMap items) {
        if (spriteNumber == 0) return blocks;
        TextureMap own = MOD_MAPS.get(spriteNumber);
        return own != null ? own : items;
    }

    private String id(String name) {
        String sub = basePath == null ? "" : basePath.startsWith("textures/") ? basePath.substring("textures/".length()) : basePath;
        if (textureType == 0 || textureType == 1 || name == null || !(sub.startsWith("items/") || sub.startsWith("blocks/"))) {
            return Assets1710.texture(name, textureType == 0);
        }
        // a mod's atlas below textures/items or textures/blocks: its sprites are in the modern atlas under that folder
        String folder = sub.substring(sub.indexOf('/') + 1);
        int colon = name.indexOf(':');
        String domain = colon < 0 ? "minecraft" : name.substring(0, colon);
        return Assets1710.texture(domain + ":" + folder + "/" + name.substring(colon + 1), sub.startsWith("blocks/"));
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

    /**
     * 1.7.10 stitched an atlas here, registering the icons of the items whose sprite number names it first. The block and
     * item atlases are built with the resource pack instead; a mod's own atlas registers its items' icons whenever the mod
     * loads it (Fisk's Heroes does after reading its hero renderers, which name the icons).
     */
    @Override
    public void loadTexture(legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager manager) {
        if (textureType == 0 || textureType == 1) return;
        var bus = legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.post(new legacyconnector.v1_7_10.net.minecraftforge.client.event.TextureStitchEvent.Pre(this));
        for (var item : org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.modItems()) {
            try {
                if (item.getSpriteNumber() == textureType) item.registerIcons(this);
            } catch (RuntimeException | LinkageError e) {
                logger.warn("registerIcons failed for {}: {}", item, e.toString());
            }
        }
        bus.post(new legacyconnector.v1_7_10.net.minecraftforge.client.event.TextureStitchEvent.Post(this));
    }

    @Override
    public void tick() {
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
