package legacyconnector.vb1_7_3.net.minecraft.src;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.BlockItem;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;
import org.kylepitcock.legacyconnector.vb1_7_3.RecipesB173;

/**
 * Risugami's {@code ModLoader} for Beta 1.7.3: the static API mods register their content through. Registrations are recorded here
 * and become modern content after the lifecycle ({@code BridgeB173}). Texture overrides hand out sprite indices exactly as the original
 * did (first free slot of terrain.png / gui/items.png, same occupied slots), so mods that compute texture coordinates themselves agree.
 * Methods that take client or world types (GUIs, renderers, key bindings) are synthesized for now.
 */
public final class ModLoader {
    public static final String VERSION = "ModLoader Beta 1.7.3";
    private static final Logger LOGGER = Logger.getLogger("ModLoader");

    // occupied sprite slots of b1.7.3's gui/items.png and terrain.png, as ModLoader's init() set them ('1' = used)
    private static final String ITEMS_USED = "1111111111111111111111111111111111111101111111011111111111111001111111111111111111111111111011111111100110000011111110000000001111111001100000110000000100000011000000010000001100000000000000110000000000000000000000000000000000000000000000001100000000000000";
    private static final String TERRAIN_USED = "1111111111111111111111111111110111111111111111111111110111111111111111111111000111111011111111111111001111111110111111111111100011111111000010001111011110000000111111000000000011111100000000001111000000000111111000000000001101000000000001111111111111000011";
    private static final boolean[] usedItemSprites = new boolean[256];
    private static final boolean[] usedTerrainSprites = new boolean[256];
    private static int itemSpriteIndex;
    private static int terrainSpriteIndex;
    private static int nextBlockModelID = 1000;
    private static int highestEntityId = 3000;

    static {
        for (int i = 0; i < 256; i++) {
            usedItemSprites[i] = ITEMS_USED.charAt(i) == '1';
            usedTerrainSprites[i] = TERRAIN_USED.charAt(i) == '1';
        }
    }

    private ModLoader() {}

    public static void AddSpawn(Class<?> type,int weight,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group) {AddSpawn(type,weight,group,(legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome[])null);}
    public static void AddSpawn(Class<?> type,int weight,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group,legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome... biomes) {
        org.kylepitcock.legacyconnector.vb1_7_3.SpawnRulesB173.add(type,weight,group,biomes);
    }
    public static void AddSpawn(String name,int weight,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group) {AddSpawn(name,weight,group,(legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome[])null);}
    public static void AddSpawn(String name,int weight,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group,legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome... biomes) {
        Class<?> type=org.kylepitcock.legacyconnector.vb1_7_3.SpawnRulesB173.namedEntity(name);if(type!=null)AddSpawn(type,weight,group,biomes);
    }
    public static void RemoveSpawn(Class<?> type,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group) {RemoveSpawn(type,group,(legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome[])null);}
    public static void RemoveSpawn(Class<?> type,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group,legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome... biomes) {
        org.kylepitcock.legacyconnector.vb1_7_3.SpawnRulesB173.remove(type,group,biomes);
    }
    public static void RemoveSpawn(String name,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group) {RemoveSpawn(name,group,(legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome[])null);}
    public static void RemoveSpawn(String name,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group,legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome... biomes) {
        Class<?> type=org.kylepitcock.legacyconnector.vb1_7_3.SpawnRulesB173.namedEntity(name);if(type!=null)RemoveSpawn(type,group,biomes);
    }

    public static void AddAchievementDesc(legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement achievement,String title,String description) {
        org.kylepitcock.legacyconnector.vb1_7_3.AdvancementsB173.describe(achievement,title,description);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static List<BaseMod> getLoadedMods() {
        return new ArrayList<>(ContentB173.loadedMods());
    }

    /** The one legacy client instance; its {@code world} is the level legacy code currently runs on. */
    public static legacyconnector.vb1_7_3.net.minecraft.client.Minecraft getMinecraftInstance() {
        return legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();
    }

    public static boolean isModLoaded(String name) {
        return ContentB173.isModLoaded(name);
    }

    public static void RegisterBlock(Block block) {
        RegisterBlock(block, null);
    }

    /** Creates the block's item (the given {@code ItemBlock} subclass, or a plain one) as ModLoader did. */
    public static void RegisterBlock(Block block, Class<?> itemClass) {
        if (block == null) return;
        int id = block.id;
        try {
            if (Item.ITEMS[id] == null) {
                if (itemClass != null) {
                    var ctor = itemClass.getDeclaredConstructor(int.class);
                    ctor.setAccessible(true);
                    ctor.newInstance(id - 256);
                } else {
                    new BlockItem(id - 256);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot create the item of block " + block + " (" + itemClass + ")", e);
        }
        ContentB173.registeredBlock(block);
    }

    public static void AddName(Object object, String name) {
        ContentB173.addName(object, name);
    }

    public static void AddLocalization(String key, String value) {
        ContentB173.addLocalization(key, value);
    }

    public static int addOverride(String sheet, String overlayPath) {
        int index = getUniqueSpriteIndex(sheet);
        addOverride(sheet, overlayPath, index);
        return index;
    }

    public static void addOverride(String sheet, String overlayPath, int index) {
        ContentB173.addTextureOverride(sheet, overlayPath, index);
    }

    public static int getUniqueSpriteIndex(String sheet) {
        if ("/gui/items.png".equals(sheet)) return getUniqueItemSpriteIndex();
        if ("/terrain.png".equals(sheet)) return getUniqueTerrainSpriteIndex();
        throw new RuntimeException("No registry for this texture: " + sheet);
    }

    /**
     * The original threw "No more empty item sprite indices left!" once the 256-slot sheet was full, which a few mods together reach
     * (InfSprites existed for that). The layer draws the sprites itself, so the indices go on past 255.
     */
    private static synchronized int getUniqueItemSpriteIndex() {
        for (; itemSpriteIndex < usedItemSprites.length; itemSpriteIndex++) {
            if (!usedItemSprites[itemSpriteIndex]) {
                usedItemSprites[itemSpriteIndex] = true;
                return itemSpriteIndex++;
            }
        }
        return itemSpriteIndex++;
    }

    private static synchronized int getUniqueTerrainSpriteIndex() {
        for (; terrainSpriteIndex < usedTerrainSprites.length; terrainSpriteIndex++) {
            if (!usedTerrainSprites[terrainSpriteIndex]) {
                usedTerrainSprites[terrainSpriteIndex] = true;
                return terrainSpriteIndex++;
            }
        }
        return terrainSpriteIndex++;
    }

    public static void AddRecipe(ItemStack output, Object... inputs) {
        RecipesB173.shaped(output, inputs);
    }

    public static void AddShapelessRecipe(ItemStack output, Object... inputs) {
        RecipesB173.shapeless(output, inputs);
    }

    public static void AddSmelting(int input, ItemStack output) {
        legacyconnector.vb1_7_3.net.minecraft.recipe.SmeltingRecipeManager.getInstance().addRecipe(input, output);
    }

    /** A mod's GUI: a {@code GuiContainer} opens on a modern menu hosting its container, any other screen on the client. */
    public static void OpenGUI(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen gui) {
        org.kylepitcock.legacyconnector.vb1_7_3.MenusB173.open(player, gui);
    }

    /** ModLoaderMp's server form: the container on a modern menu, drawn by the GUI the client opened for it (or a plain one). */
    public static void OpenGUI(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player, int guiId,
            legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory inventory, legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler container) {
        org.kylepitcock.legacyconnector.vb1_7_3.MenusB173.openContainer(player, container);
    }

    public static int AddAllFuel(int id) {
        return ContentB173.fuel(id);
    }

    public static int AddArmor(String name) {
        return ContentB173.armorTexture(name);
    }

    public static void RegisterTileEntity(Class<?> type, String name) {
        ContentB173.registerTileEntity(type, name);
    }

    /** With a special renderer: the save id is registered the same way; the renderer is kept for the client. */
    public static void RegisterTileEntity(Class<?> type, String name,
            legacyconnector.vb1_7_3.net.minecraft.client.render.block.entity.BlockEntityRenderer renderer) {
        ContentB173.registerTileEntity(type, name);
        ContentB173.registerTileEntityRenderer(type, renderer);
    }

    public static void RegisterEntityID(Class<?> type, String name, int id) {
        ContentB173.registerEntity(type, name, id);
    }

    public static synchronized int getUniqueEntityId() {
        return highestEntityId++;
    }

    public static synchronized int getUniqueBlockModelID(BaseMod mod, boolean full3DItem) {
        int id=nextBlockModelID++;
        org.kylepitcock.legacyconnector.vb1_7_3.BlockRenderB173.register(id,mod);
        return id;
    }
    public static void addAnimation(legacyconnector.vb1_7_3.net.minecraft.client.render.texture.DynamicTexture effect){org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.add(effect);}
    public static java.awt.image.BufferedImage loadImage(legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager manager,String path) throws java.io.IOException {
        try(var stream=manager.texturePacks.selected.getResource(path)){
            if(stream==null)throw new java.io.IOException("Missing Beta texture "+path);
            var image=javax.imageio.ImageIO.read(stream);if(image==null)throw new java.io.IOException("Invalid Beta texture "+path);return image;
        }
    }

    public static void SetInGameHook(BaseMod mod, boolean enable, boolean useClock) {
        ContentB173.setTickHook(mod, enable, useClock, false);
    }

    public static void SetInGUIHook(BaseMod mod, boolean enable, boolean useClock) {
        ContentB173.setTickHook(mod, enable, useClock, true);
    }

    public static void loadConfig() {}

    public static void saveConfig() {}

    public static Object getPrivateValue(Class<?> owner, Object instance, int index) {
        try {
            Field f = org.kylepitcock.legacyconnector.vb1_7_3.ReflectionB173.field(owner, index);
            f.setAccessible(true);
            return f.get(instance);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.throwing("ModLoader", "getPrivateValue", e);
            ThrowException("An impossible error has occured!", e);
            return null;
        }
    }

    public static Object getPrivateValue(Class<?> owner, Object instance, String name) throws NoSuchFieldException {
        try {
            Field f = org.kylepitcock.legacyconnector.vb1_7_3.ReflectionB173.field(owner, name);
            f.setAccessible(true);
            return f.get(instance);
        } catch (IllegalAccessException | RuntimeException e) {
            LOGGER.throwing("ModLoader", "getPrivateValue", e);
            ThrowException("An impossible error has occured!", e);
            return null;
        }
    }

    public static void setPrivateValue(Class<?> owner, Object instance, int index, Object value) {
        try {
            Field f = org.kylepitcock.legacyconnector.vb1_7_3.ReflectionB173.field(owner, index);
            f.setAccessible(true);
            f.set(instance, value);
            org.kylepitcock.legacyconnector.vb1_7_3.SpawnCapsB173.afterWrite(f, instance);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.throwing("ModLoader", "setPrivateValue", e);
            ThrowException("An impossible error has occured!", e);
        }
    }

    public static void setPrivateValue(Class<?> owner, Object instance, String name, Object value) throws NoSuchFieldException {
        try {
            Field f = org.kylepitcock.legacyconnector.vb1_7_3.ReflectionB173.field(owner, name);
            f.setAccessible(true);
            f.set(instance, value);
            org.kylepitcock.legacyconnector.vb1_7_3.SpawnCapsB173.afterWrite(f, instance);
        } catch (IllegalAccessException | RuntimeException e) {
            LOGGER.throwing("ModLoader", "setPrivateValue", e);
            ThrowException("An impossible error has occured!", e);
        }
    }

    public static void ThrowException(String message, Throwable e) {
        throw new RuntimeException(message, e);
    }
}
