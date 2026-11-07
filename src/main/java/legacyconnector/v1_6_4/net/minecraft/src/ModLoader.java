package legacyconnector.v1_6_4.net.minecraft.src;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.TickType;
import legacyconnector.v1_6_4.cpw.mods.fml.common.registry.TickRegistry;
import legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.IFuelHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.IWorldGenerator;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ObfuscationReflectionHelper;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.EntityRegistry;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.LanguageRegistry;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/**
 * Forge 1.4.7's ModLoader emulation ({@code net.minecraft.src.ModLoader}, the static API of ModLoader mods). Every call is the equivalent of the
 * registry the 1.7.10 shim already has ({@code GameRegistry}, {@code EntityRegistry}, {@code LanguageRegistry}), the way FML's own ModLoader class
 * forwarded to FML. {@link BaseMod}'s lifecycle handlers call {@link #mcc$load} (the mod's {@code load()}) in the init phase and
 * {@link #mcc$modsLoaded} in post-init. Client-only calls (key bindings, texture animations, GUI tick hooks) are accepted and ignored on a server.
 */
public class ModLoader {
    public static final String fmlMarker = "This is an FML marker";
    private static final Logger LOG = Logger.getLogger("ModLoader");

    private static final List<BaseMod> MODS = new CopyOnWriteArrayList<>();

    private ModLoader() {}

    // lifecycle (called by BaseMod's handlers)

    public static void mcc$load(BaseMod mod) {
        if (!MODS.contains(mod)) MODS.add(mod);
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/ModLoader").info("Loading ModLoader mod {} {}", mod.getName(), mod.getVersion());
        mod.load();
        registerWorldGen(mod);
        registerFuel(mod);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            try {
                ClientHooks.registerRenderers(mod);
            } catch (RuntimeException | LinkageError e) {
                LOG.warning(mod.getName() + ": addRenderer failed: " + e);
            }
        }
    }

    public static void mcc$modsLoaded(BaseMod mod) {
        mod.modsLoaded();
    }

    private static void registerWorldGen(BaseMod mod) {
        GameRegistry.registerWorldGenerator(new IWorldGenerator() {
            @Override
            public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
                int dim = World.mcc$get$provider(world).dimensionId;
                if (dim == 0) mod.generateSurface(world, random, chunkX << 4, chunkZ << 4);
                else if (dim == -1) mod.generateNether(world, random, chunkX << 4, chunkZ << 4);
            }
        }, 0);
    }

    private static void registerFuel(BaseMod mod) {
        GameRegistry.registerFuelHandler(new IFuelHandler() {
            @Override
            public int getBurnTime(ItemStack fuel) {
                if (fuel == null || fuel.getItem() == null) return 0;
                return mod.addFuel(legacyconnector.v1_6_4.net.minecraft.util.Ids.itemId(fuel.getItem()), fuel.getItemDamage());
            }
        });
    }

    // registration

    public static void registerBlock(Block block) {
        legacyconnector.v1_6_4.facade.cpw.mods.fml.common.registry.GameRegistry.registerBlock(block);
    }

    public static void registerBlock(Block block, Class<? extends ItemBlock> itemclass) {
        String n = block.getUnlocalizedName();
        if (n == null) n = "block" + legacyconnector.v1_6_4.net.minecraft.util.Ids.blockId(block);
        if (n.startsWith("tile.")) n = n.substring(5);
        GameRegistry.registerBlock(block, itemclass, n);
    }

    public static void registerEntityID(Class<? extends Entity> entityClass, String entityName, int id) {
        EntityRegistry.registerGlobalEntityID(entityClass, entityName, id);
    }

    public static void registerEntityID(Class<? extends Entity> entityClass, String entityName, int id, int background, int foreground) {
        EntityRegistry.registerGlobalEntityID(entityClass, entityName, id, background, foreground);
    }

    /** {@code addEntityTracker}: the tracker settings of a mod entity (the mod object is the BaseMod). */
    public static void addEntityTracker(BaseMod mod, Class<? extends Entity> entityClass, int entityTypeId, int updateRange, int updateInterval, boolean sendVelocityInfo) {
        EntityRegistry.registerModEntity(entityClass, entityClass.getSimpleName(), entityTypeId, mod, updateRange, updateInterval, sendVelocityInfo);
    }

    public static int getUniqueEntityId() {
        return EntityRegistry.findGlobalUniqueEntityId();
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id) {
        GameRegistry.registerTileEntity(tileEntityClass, id);
    }

    public static void addRecipe(ItemStack output, Object... params) {
        GameRegistry.addRecipe(output, params);
    }

    public static void addShapelessRecipe(ItemStack output, Object... params) {
        GameRegistry.addShapelessRecipe(output, params);
    }

    public static void addSmelting(int input, ItemStack output) {
        legacyconnector.v1_6_4.facade.cpw.mods.fml.common.registry.GameRegistry.addSmelting(input, output, 1.0F);
    }

    public static void addSmelting(int input, ItemStack output, float experience) {
        legacyconnector.v1_6_4.facade.cpw.mods.fml.common.registry.GameRegistry.addSmelting(input, output, experience);
    }

    public static void addName(Object instance, String name) {
        LanguageRegistry.addName(instance, name);
    }

    public static void addName(Object instance, String lang, String name) {
        LanguageRegistry.instance().addNameForObject(instance, lang, name);
    }

    public static void addLocalization(String key, String value) {
        LanguageRegistry.instance().addStringLocalization(key, "en_US", value);
    }

    public static void addLocalization(String key, String lang, String value) {
        LanguageRegistry.instance().addStringLocalization(key, lang, value);
    }

    private static BiomeGenBase[] base12Biomes() {
        return new BiomeGenBase[] {BiomeGenBase.desert, BiomeGenBase.forest, BiomeGenBase.extremeHills, BiomeGenBase.swampland, BiomeGenBase.plains, BiomeGenBase.taiga, BiomeGenBase.jungle};
    }

    public static void addSpawn(Class<? extends Entity> entityClass, int weightedProb, int min, int max, EnumCreatureType spawnList) {
        EntityRegistry.addSpawn(entityClass, weightedProb, min, max, spawnList, base12Biomes());
    }

    public static void addSpawn(Class<? extends Entity> entityClass, int weightedProb, int min, int max, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        EntityRegistry.addSpawn(entityClass, weightedProb, min, max, spawnList, biomes);
    }

    public static void removeSpawn(Class<? extends Entity> entityClass, EnumCreatureType spawnList) {
        EntityRegistry.removeSpawn(entityClass, spawnList, base12Biomes());
    }

    public static void removeSpawn(Class<? extends Entity> entityClass, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        EntityRegistry.removeSpawn(entityClass, spawnList, biomes);
    }

    public static void addTrade(int profession, TradeEntry entry) {}

    public static void registerContainerID(BaseMod mod, int id) {}

    public static void registerPacketChannel(BaseMod mod, String channel) {}

    public static void registerKey(BaseMod mod, Object keyHandler, boolean allowRepeat) {}

    // information

    public static List<BaseMod> getLoadedMods() {
        return new ArrayList<>(MODS);
    }

    public static Logger getLogger() {
        return LOG;
    }

    public static boolean isModLoaded(String modname) {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.isModLoaded(modname);
    }

    public static Minecraft getMinecraftInstance() {
        return FMLCommonHandler.instance().getSide() == Side.CLIENT ? Minecraft.getMinecraft() : null;
    }

    public static MinecraftServer getMinecraftServerInstance() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    public static boolean isGUIOpen(Class<? extends GuiScreen> gui) {
        return false;
    }

    public static <T, E> T getPrivateValue(Class<? super E> instanceclass, E instance, int fieldindex) {
        return ObfuscationReflectionHelper.getPrivateValue(instanceclass, instance, fieldindex);
    }

    public static <T, E> T getPrivateValue(Class<? super E> instanceclass, E instance, String field) {
        return ObfuscationReflectionHelper.getPrivateValue(instanceclass, instance, field);
    }

    public static <T, E> void setPrivateValue(Class<? super T> instanceclass, T instance, int fieldindex, E value) {
        ObfuscationReflectionHelper.setPrivateValue(instanceclass, instance, value, fieldindex);
    }

    public static <T, E> void setPrivateValue(Class<? super T> instanceclass, T instance, String field, E value) {
        ObfuscationReflectionHelper.setPrivateValue(instanceclass, instance, value, field);
    }

    public static void throwException(String message, Throwable e) {
        LOG.severe(message + ": " + e);
        e.printStackTrace();
    }

    public static void throwException(Throwable e) {
        throwException("Exception in ModLoader", e);
    }

    /** Spills the contents of a container tile into the world when its block is broken. */
    public static void genericContainerRemoval(World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IInventory inv)) return;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack == null) continue;
            inv.setInventorySlotContents(i, null);
            world.spawnEntityInWorld(new legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack));
        }
    }

    // ticks

    /** {@code setInGameHook}: {@code onTickInGame} of the mod runs every server tick, and every client render tick in a world. */
    public static void setInGameHook(BaseMod mod, boolean enable, boolean useClock) {
        if (enable) Ticks.add(mod, false);
        else Ticks.remove(mod, false);
    }

    public static void setInGUIHook(BaseMod mod, boolean enable, boolean useClock) {
        if (enable) Ticks.add(mod, true);
        else Ticks.remove(mod, true);
    }

    // hooks that need a client (no-ops here: accepted so the mod links and loads)

    public static int getUniqueSpriteIndex(String path) {
        return org.kylepitcock.legacyconnector.v1_6_4.bridge.SpriteSheets147.uniqueIndex(path);
    }

    public static int addOverride(String fileToOverride, String fileToAdd) {
        return org.kylepitcock.legacyconnector.v1_6_4.bridge.SpriteSheets147.override(fileToOverride, fileToAdd);
    }

    public static void addOverride(String path, String overlayPath, int index) {
        org.kylepitcock.legacyconnector.v1_6_4.bridge.SpriteSheets147.override(path, overlayPath, index);
    }

    public static int getUniqueBlockModelID(BaseMod mod, boolean inventoryRenderer) {
        return legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.getNextAvailableRenderId();
    }

    public static int addArmor(String armor) {
        return legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.addNewArmourRendererPrefix(armor);
    }

    public static void addAnimation(Object textureFx) {}

    public static void onItemPickup(EntityPlayer player, ItemStack item) {
        for (BaseMod m : MODS) m.onItemPickup(player, item);
    }

    public static void takenFromCrafting(EntityPlayer player, ItemStack item, IInventory matrix) {
        for (BaseMod m : MODS) m.takenFromCrafting(player, item, matrix);
    }

    public static void takenFromFurnace(EntityPlayer player, ItemStack item) {
        for (BaseMod m : MODS) m.takenFromFurnace(player, item);
    }

    public static void saveConfig() {}

    public static void loadConfig() {}

    /** Client-only part, kept in its own class so a server never loads the client types. */
    private static final class ClientHooks {
        static void registerRenderers(BaseMod mod) {
            Map<Class<? extends Entity>, legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.Render> map = new java.util.LinkedHashMap<>();
            mod.addRenderer(map);
            for (var e : map.entrySet()) {
                legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(e.getKey(), e.getValue());
            }
            mod.registerAnimation(Minecraft.getMinecraft());
        }
    }

    /** The in-game tick hooks of the mods: server ticks to {@code onTickInGame(MinecraftServer)}, render ticks to {@code onTickInGame(float, Minecraft)}. */
    private static final class Ticks implements ITickHandler {
        private static final Ticks SERVER = new Ticks(Side.SERVER);
        private static final Ticks CLIENT = new Ticks(Side.CLIENT);
        private static boolean serverRegistered;
        private static boolean clientRegistered;

        private final Side side;
        private final List<BaseMod> game = new CopyOnWriteArrayList<>();
        private final List<BaseMod> gui = new CopyOnWriteArrayList<>();

        private Ticks(Side side) {
            this.side = side;
        }

        static synchronized void add(BaseMod mod, boolean gui) {
            if (!gui) {
                if (!SERVER.game.contains(mod)) SERVER.game.add(mod);
                if (!serverRegistered) {
                    serverRegistered = true;
                    TickRegistry.registerTickHandler(SERVER, Side.SERVER);
                }
            }
            if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                List<BaseMod> list = gui ? CLIENT.gui : CLIENT.game;
                if (!list.contains(mod)) list.add(mod);
                if (!clientRegistered) {
                    clientRegistered = true;
                    TickRegistry.registerTickHandler(CLIENT, Side.CLIENT);
                }
            }
        }

        static void remove(BaseMod mod, boolean gui) {
            if (!gui) SERVER.game.remove(mod);
            (gui ? CLIENT.gui : CLIENT.game).remove(mod);
        }

        @Override
        public void tickStart(EnumSet<TickType> type, Object... tickData) {}

        @Override
        public void tickEnd(EnumSet<TickType> type, Object... tickData) {
            try {
                if (side == Side.SERVER) {
                    MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                    for (BaseMod m : game) m.onTickInGame(server);
                } else {
                    Minecraft mc = Minecraft.getMinecraft();
                    float partial = tickData.length > 0 && tickData[0] instanceof Float f ? f : 0F;
                    if (Minecraft.mcc$get$theWorld(mc) != null) for (BaseMod m : game) m.onTickInGame(partial, mc);
                    GuiScreen screen = Minecraft.mcc$get$currentScreen(mc);
                    if (screen != null) for (BaseMod m : gui) m.onTickInGUI(partial, mc, screen);
                }
            } catch (RuntimeException e) {
                LOG.warning("A ModLoader tick hook failed: " + e);
            }
        }

        @Override
        public EnumSet<TickType> ticks() {
            return side == Side.SERVER ? EnumSet.of(TickType.SERVER) : EnumSet.of(TickType.RENDER);
        }

        @Override
        public String getLabel() {
            return "ModLoader";
        }
    }
}
