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
import org.kylepitcock.legacyconnector.v1_6_4.bridge.ModLoaderHooks125;

/**
 * Forge 1.4.7's ModLoader emulation ({@code net.minecraft.src.ModLoader}, the static API of ModLoader mods). Every call is the equivalent of the
 * registry the 1.7.10 shim already has ({@code GameRegistry}, {@code EntityRegistry}, {@code LanguageRegistry}), the way FML's own ModLoader class
 * forwarded to FML. {@link BaseMod}'s lifecycle handlers call {@link #mcc$load} (the mod's {@code load()}) in the init phase and
 * {@link #mcc$modsLoaded} in post-init. Client-only calls (key bindings, texture animations, GUI tick hooks) are accepted and ignored on a server.
 */
public class ModLoader {
    public static final String fmlMarker = "This is an FML marker";
    /** ModLoader 1.2.5's public constants. */
    public static final boolean DEBUG = false;
    public static final String VERSION = "ModLoader 1.2.5";
    public static final java.util.Properties props = new java.util.Properties();
    public static java.util.logging.Level cfgLoggingLevel = java.util.logging.Level.FINER;
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
                dispatchGenerate(mod, world, random, chunkX, chunkZ);
            }
        }, 0);
    }

    /** {@code generateSurface} for the overworld's generator, {@code generateNether} for the Nether's (ModLoader chose by the chunk generator's kind). */
    private static void dispatchGenerate(BaseMod mod, World world, Random random, int chunkX, int chunkZ) {
        int dim = World.mcc$get$provider(world).dimensionId;
        if (dim == 0) mod.generateSurface(world, random, chunkX << 4, chunkZ << 4);
        else if (dim == -1) mod.generateNether(world, random, chunkX << 4, chunkZ << 4);
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

    /** 1.2.x: the tile entity and the special renderer that draws it (client only). */
    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer renderer) {
        GameRegistry.registerTileEntity(tileEntityClass, id);
        if (renderer != null && FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            legacyconnector.v1_7_10.cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(tileEntityClass, renderer);
        }
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

    /** {@code addSpawn(String entityName, ...)}: the entity is the one registered under that name (ModLoader looked it up in the entity list). */
    @SuppressWarnings("unchecked")
    public static void addSpawn(String entityName, int weightedProb, int min, int max, EnumCreatureType spawnList) {
        Class<? extends Entity> cls = (Class<? extends Entity>) legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.get(entityName);
        if (cls != null) addSpawn(cls, weightedProb, min, max, spawnList);
        else LOG.warning("addSpawn: no entity named " + entityName);
    }

    @SuppressWarnings("unchecked")
    public static void addSpawn(String entityName, int weightedProb, int min, int max, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        Class<? extends Entity> cls = (Class<? extends Entity>) legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.get(entityName);
        if (cls != null) addSpawn(cls, weightedProb, min, max, spawnList, biomes);
        else LOG.warning("addSpawn: no entity named " + entityName);
    }

    @SuppressWarnings("unchecked")
    public static void removeSpawn(String entityName, EnumCreatureType spawnList) {
        Class<? extends Entity> cls = (Class<? extends Entity>) legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.get(entityName);
        if (cls != null) removeSpawn(cls, spawnList);
    }

    @SuppressWarnings("unchecked")
    public static void removeSpawn(String entityName, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        Class<? extends Entity> cls = (Class<? extends Entity>) legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.get(entityName);
        if (cls != null) removeSpawn(cls, spawnList, biomes);
    }

    public static void removeSpawn(Class<? extends Entity> entityClass, EnumCreatureType spawnList) {
        EntityRegistry.removeSpawn(entityClass, spawnList, base12Biomes());
    }

    public static void removeSpawn(Class<? extends Entity> entityClass, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        EntityRegistry.removeSpawn(entityClass, spawnList, biomes);
    }

    /** {@code ModLoader.addBiome(biome)}: the biome joins the default world type's list (same as Forge's {@code GameRegistry.addBiome}). */
    public static void addBiome(BiomeGenBase biome) {
        if (biome != null) legacyconnector.v1_7_10.net.minecraft.world.WorldType.DEFAULT.addNewBiome(biome);
    }

    public static void addTrade(int profession, TradeEntry entry) {}

    public static void registerContainerID(BaseMod mod, int id) {}

    private static final java.util.Map<String, BaseMod> CHANNEL_MODS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Custom payloads on {@code channel} go to {@code mod.receiveCustomPacket} (ModLoader 1.2.5's packet-250 model); the mod sends with {@link #sendPacket}. */
    public static void registerPacketChannel(BaseMod mod, String channel) {
        if (mod == null || channel == null) return;
        CHANNEL_MODS.put(channel, mod);
        org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164.registerChannel(
                (manager, packet, player) -> {
                    if (player instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP mp) mod.receiveCustomPacket(packet, mp);
                    else mod.receiveCustomPacket(packet);
                }, channel, null);
    }

    /** Sends a packet to the server (single player: the integrated one) from the client. */
    public static void sendPacket(legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        if (packet != null) org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164.sendToServer(packet);
    }

    /** The host's channel dispatch hands custom payloads to the registered mod already; kept for mods that call it themselves. */
    public static void receivePacket(legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload packet) {
        if (packet == null || packet.channel == null) return;
        BaseMod m = CHANNEL_MODS.get(packet.channel);
        if (m != null) m.receiveCustomPacket(packet);
    }

    /** Sends {@code text} as a chat message of the local player (ModLoader's chat channel to the server). */
    public static void serverChat(String text) {
        if (text == null || FMLCommonHandler.instance().getSide() != Side.CLIENT) return;
        legacyconnector.v1_7_10.net.minecraft.client.entity.EntityClientPlayerMP p = Minecraft.mcc$get$thePlayer(Minecraft.getMinecraft());
        if (p != null) p.sendChatMessage(text);
    }

    public static void serverConnect(legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient handler,
            legacyconnector.v1_6_4.net.minecraft.network.packet.Packet1Login login) {
        for (BaseMod m : MODS) m.serverConnect(handler);
    }

    public static void serverDisconnect() {
        for (BaseMod m : MODS) m.serverDisconnect();
    }

    /** The server build's {@code serverDisconnect(player)}. */
    public static void serverDisconnect(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player) {
        for (BaseMod m : MODS) m.serverDisconnect(player);
    }

    public static void registerKey(BaseMod mod, Object keyHandler, boolean allowRepeat) {
        if (keyHandler instanceof legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding key) registerKey(mod, key, allowRepeat);
    }

    /** Lists the key in the controls screen and calls {@code mod.keyboardEvent(key)} when it goes down (every tick while held if {@code allowRepeat}). */
    public static void registerKey(BaseMod mod, legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding key, boolean allowRepeat) {
        if (mod == null || key == null) return;
        // ModLoader read the keyboard itself, also with a screen open, where the modern key mappings are released: poll the raw state for that case
        Ticks.ensureClient();
        org.kylepitcock.legacyconnector.v1_6_4.bridge.ModKeys125.add(new org.kylepitcock.legacyconnector.v1_6_4.bridge.ModKeys125.Key(
                () -> mod.keyboardEvent(key), allowRepeat, () -> org.kylepitcock.legacyconnector.v1_6_4.bridge.ModKeys125.rawDown(key)));
        legacyconnector.v1_6_4.cpw.mods.fml.client.registry.KeyBindingRegistry.registerKeyBinding(
                new legacyconnector.v1_6_4.cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler(
                        new legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding[] {key}, new boolean[] {allowRepeat}) {
                    @Override
                    public void keyDown(EnumSet<TickType> types, legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding kb, boolean tickEnd, boolean isRepeat) {
                        if (tickEnd) mod.keyboardEvent(kb);
                    }

                    @Override
                    public void keyUp(EnumSet<TickType> types, legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding kb, boolean tickEnd) {}

                    @Override
                    public EnumSet<TickType> ticks() {
                        return EnumSet.of(TickType.CLIENT);
                    }

                    @Override
                    public String getLabel() {
                        return "ModLoader key " + kb0();
                    }

                    private String kb0() {
                        return key.getKeyDescription();
                    }
                });
    }

    private static final List<legacyconnector.v1_7_10.net.minecraft.command.ICommand> COMMANDS = new CopyOnWriteArrayList<>();
    private static MinecraftServer commandsRegisteredOn;

    /**
     * Adds a command to the server command manager. It is registered on each server's first tick (a new world has a new command tree), so it
     * also works when called while the mods load.
     */
    public static void addCommand(legacyconnector.v1_7_10.net.minecraft.command.ICommand command) {
        if (command == null || COMMANDS.contains(command)) return;
        COMMANDS.add(command);
        if (COMMANDS.size() == 1) {
            TickRegistry.registerTickHandler(new ITickHandler() {
                @Override
                public void tickStart(EnumSet<TickType> type, Object... tickData) {}

                @Override
                public void tickEnd(EnumSet<TickType> type, Object... tickData) {
                    MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                    if (server == null || server == commandsRegisteredOn) return;
                    commandsRegisteredOn = server;
                    for (legacyconnector.v1_7_10.net.minecraft.command.ICommand c : COMMANDS) {
                        try {
                            org.kylepitcock.legacyconnector.v1_7_10.bridge.CommandBridge1710.register(server.mcc$modern(), c);
                        } catch (RuntimeException e) {
                            LOG.warning("Could not register the command " + c.getCommandName() + ": " + e);
                        }
                    }
                }

                @Override
                public EnumSet<TickType> ticks() {
                    return EnumSet.of(TickType.SERVER);
                }

                @Override
                public String getLabel() {
                    return "ModLoader commands";
                }
            }, Side.SERVER);
        }
    }

    /** Sends a packet from the client to the server it is connected to. */
    public static void clientSendPacket(legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient handler = Minecraft.getMinecraft().getNetHandler();
        if (handler != null && packet != null) handler.addToSendQueue(packet);
    }

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
        if (FMLCommonHandler.instance().getSide() != Side.CLIENT) return false;
        GuiScreen current = Minecraft.mcc$get$currentScreen(Minecraft.getMinecraft());
        return gui == null ? current != null : gui.isInstance(current);
    }

    /** Opens {@code gui} for the local player (client only; ModLoader checked that {@code player} is the client's own). */
    public static void openGUI(EntityPlayer player, GuiScreen gui) {
        if (FMLCommonHandler.instance().getSide() != Side.CLIENT) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || Minecraft.mcc$get$thePlayer(mc) != player) return;
        mc.displayGuiScreen(gui);
    }

    /** The server's {@code openGUI}: opens {@code container} for the player as the GUI the mod registered under {@code id} ({@link ModLoaderMp#registerGUI}). */
    public static void openGUI(EntityPlayer player, int id, IInventory inventory, legacyconnector.v1_7_10.net.minecraft.inventory.Container container) {
        if (!ModLoaderMp.mcc$openGui(player, id, container)) LOG.warning("openGUI: no mod registered the GUI id " + id);
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
        if (enable) Ticks.add(mod, false, useClock);
        else Ticks.remove(mod, false);
    }

    public static void setInGUIHook(BaseMod mod, boolean enable, boolean useClock) {
        if (enable) Ticks.add(mod, true, useClock);
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

    /**
     * A render type id whose blocks {@code mod.renderWorldBlock} / {@code mod.renderInvBlock} draw (ModLoader asked the mod for every block of that
     * model id); {@code inventoryRenderer} says the inventory form is the 3D block, not a flat item.
     */
    public static int getUniqueBlockModelID(BaseMod mod, boolean inventoryRenderer) {
        int id = legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.getNextAvailableRenderId();
        BLOCK_MODELS.put(id, mod);
        BLOCK_FULL3D.put(id, inventoryRenderer);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            try {
                ClientHooks.registerBlockModel(id, mod, inventoryRenderer);
            } catch (RuntimeException | LinkageError e) {
                LOG.warning(mod.getName() + ": block model " + id + " not registered: " + e);
            }
        }
        return id;
    }

    private static final java.util.Map<Integer, BaseMod> BLOCK_MODELS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Integer, Boolean> BLOCK_FULL3D = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean renderBlockIsItemFull3D(int modelId) {
        return BLOCK_FULL3D.getOrDefault(modelId, Boolean.FALSE);
    }

    public static boolean renderWorldBlock(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks renderer,
            legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, Block block, int modelId) {
        BaseMod m = BLOCK_MODELS.get(modelId);
        return m != null && m.renderWorldBlock(renderer, world, x, y, z, block, modelId);
    }

    public static void renderInvBlock(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks renderer, Block block, int metadata, int modelId) {
        BaseMod m = BLOCK_MODELS.get(modelId);
        if (m != null) m.renderInvBlock(renderer, block, metadata, modelId);
    }

    public static int addArmor(String armor) {
        return legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.addNewArmourRendererPrefix(armor);
    }

    /** The animations mods added; ModLoader's {@code registerAllTextureOverrides} cleared its list and had every mod register again for each texture pack. */
    private static final List<Object> ANIMATIONS = new CopyOnWriteArrayList<>();
    private static volatile List<String> lastPacks;

    public static void addAnimation(Object textureFx) {
        ANIMATIONS.add(textureFx);
        org.kylepitcock.legacyconnector.v1_6_4.bridge.Animations147.register(textureFx);
    }

    public static void addAnimation(legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx.TextureFX textureFx) {
        addAnimation((Object) textureFx);
    }

    /**
     * {@code registerAllTextureOverrides} again, after the selected resource packs changed: the animations are dropped and every mod's
     * {@code registerAnimation} runs once more (a mod builds its animations from the images of the pack in use). Client tick.
     */
    static void mcc$checkPacks() {
        List<String> ids = List.copyOf(net.minecraft.client.Minecraft.getInstance().getResourcePackRepository().getSelectedIds());
        List<String> before = lastPacks;
        lastPacks = ids;
        if (before == null || before.equals(ids)) return;
        mcc$reloadAnimations();
    }

    static void mcc$reloadAnimations() {
        for (Object fx : ANIMATIONS) org.kylepitcock.legacyconnector.v1_6_4.bridge.Animations147.unregister(fx);
        ANIMATIONS.clear();
        for (BaseMod mod : MODS) {
            try {
                mod.registerAnimation(Minecraft.getMinecraft());
            } catch (RuntimeException | LinkageError e) {
                LOG.warning("registerAnimation of " + mod.getClass().getName() + " failed after a resource pack change: " + e);
            }
        }
    }

    /** The name and description of an achievement (the 1.2.x constructor took no text; ModLoader added the lang entries). */
    public static void addAchievementDesc(legacyconnector.v1_7_10.net.minecraft.stats.Achievement achievement, String name, String description) {
        if (achievement == null) return;
        String descKey = achievement.mcc$descriptionKey();
        String nameKey = descKey.endsWith(".desc") ? descKey.substring(0, descKey.length() - 5) : descKey;
        LanguageRegistry.instance().addStringLocalization(nameKey, "en_US", name);
        LanguageRegistry.instance().addStringLocalization(descKey, "en_US", description);
    }

    /** The burn time the mods give an item: the longest of their {@code addFuel} answers (ModLoader asked every mod for every furnace fuel). */
    public static int addAllFuel(int id, int metadata) {
        int best = 0;
        for (BaseMod m : MODS) best = Math.max(best, m.addFuel(id, metadata));
        return best;
    }

    public static void addAllRenderers(Map<Class<? extends Entity>, legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.Render> renderers) {
        for (BaseMod m : MODS) m.addRenderer(renderers);
    }

    /** Whether a mod claims the dispensed item and spawned its entity. */
    public static boolean dispenseEntity(World world, double x, double y, double z, int xVel, int zVel, ItemStack item) {
        for (BaseMod m : MODS) if (m.dispenseEntity(world, x, y, z, xVel, zVel, item)) return true;
        return false;
    }

    /** The game's key list with the mods' keys: the layer lists them itself (see registerKey), so the list is returned as it is. */
    public static legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding[] registerAllKeys(
            legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding[] keys) {
        return keys;
    }

    public static void registerAllTextureOverrides(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager textures) {}

    /** Reads {@code path} from the mods' jars (or the game's resources) as an image, as ModLoader read it from the texture pack or the mod. */
    public static java.awt.image.BufferedImage loadImage(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager textures, String path) throws Exception {
        String p = path.startsWith("/") ? path.substring(1) : path;
        java.io.InputStream in = null;
        for (BaseMod m : MODS) {
            in = m.getClass().getClassLoader().getResourceAsStream(p);
            if (in != null) break;
        }
        if (in == null) in = ModLoader.class.getClassLoader().getResourceAsStream(p);
        if (in == null) throw new java.io.FileNotFoundException(path);
        try (java.io.InputStream stream = in) {
            return javax.imageio.ImageIO.read(stream);
        }
    }

    public static void removeBiome(BiomeGenBase biome) {
        if (biome != null) legacyconnector.v1_7_10.net.minecraft.world.WorldType.DEFAULT.removeBiome(biome);
    }

    /**
     * The game's per-frame entry point of the patched {@code Minecraft}. The layer runs the hooks from its tick registry instead (the registry has
     * the render tick this call stood for), so a call from a mod only runs the due in-game hooks once, here, with the same rules.
     */
    public static void onTick(float partialTicks, Minecraft game) {
        Ticks.CLIENT.tickEnd(EnumSet.of(TickType.RENDER), partialTicks);
    }

    /**
     * The patched chunk generators' entry point: every mod's {@code generateSurface}/{@code generateNether} for the chunk, with ModLoader's random.
     * The layer itself runs the same dispatch through its native world generator registry (see {@code registerWorldGen}), so this only serves a mod
     * that populates a chunk by hand.
     */
    public static void populateChunk(legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider provider, int chunkX, int chunkZ, World world) {
        Random random = ModLoaderHooks125.chunkRandom(world.getSeed(), chunkX, chunkZ);
        for (BaseMod m : MODS) dispatchGenerate(m, world, random, chunkX, chunkZ);
    }

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
        static void registerBlockModel(int id, BaseMod mod, boolean full3D) {
            legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.registerBlockHandler(id,
                    new legacyconnector.v1_7_10.cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler() {
                        @Override
                        public void renderInventoryBlock(Block block, int metadata, int modelId, legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks renderer) {
                            mod.renderInvBlock(renderer, block, metadata, modelId);
                        }

                        @Override
                        public boolean renderWorldBlock(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, Block block, int modelId,
                                legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks renderer) {
                            return mod.renderWorldBlock(renderer, world, x, y, z, block, modelId);
                        }

                        @Override
                        public boolean shouldRender3DInInventory(int modelId) {
                            return full3D;
                        }

                        @Override
                        public int getRenderId() {
                            return id;
                        }
                    });
        }

        static void registerRenderers(BaseMod mod) {
            Map<Class<? extends Entity>, legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.Render> map = new java.util.LinkedHashMap<>();
            mod.addRenderer(map);
            for (var e : map.entrySet()) {
                legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(e.getKey(), e.getValue());
            }
            mod.registerAnimation(Minecraft.getMinecraft());
        }
    }

    /**
     * The in-game tick hooks of the mods: server ticks to {@code onTickInGame(MinecraftServer)} (the 1.2.5 server declared it {@code void}: the loader adds the
     * boolean method next to a mod's void override, {@code return-bridges.tsv}), render ticks to {@code onTickInGame(float, Minecraft)}. {@code useClock} hooks run once per world
     * tick, not once per frame, and a hook whose callback answers false is unregistered, as ModLoader's {@code onTick} did.
     */
    private static final class Ticks implements ITickHandler {
        private static final Ticks SERVER = new Ticks(Side.SERVER);
        private static final Ticks CLIENT = new Ticks(Side.CLIENT);
        private static boolean serverRegistered;
        private static boolean clientRegistered;

        private final Side side;
        private final ModLoaderHooks125.Hooks<BaseMod> game = new ModLoaderHooks125.Hooks<>();
        private final ModLoaderHooks125.Hooks<BaseMod> gui = new ModLoaderHooks125.Hooks<>();
        private long clock;

        private Ticks(Side side) {
            this.side = side;
        }

        static synchronized void add(BaseMod mod, boolean gui, boolean useClock) {
            if (!gui) {
                SERVER.game.add(mod, useClock);
                if (!serverRegistered) {
                    serverRegistered = true;
                    TickRegistry.registerTickHandler(SERVER, Side.SERVER);
                }
            }
            if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                (gui ? CLIENT.gui : CLIENT.game).add(mod, useClock);
                if (!clientRegistered) {
                    clientRegistered = true;
                    TickRegistry.registerTickHandler(CLIENT, Side.CLIENT);
                }
            }
        }

        /** The client tick also polls the mod keys, so it runs even for a mod that registered no tick hook. */
        static synchronized void ensureClient() {
            if (FMLCommonHandler.instance().getSide() == Side.CLIENT && !clientRegistered) {
                clientRegistered = true;
                TickRegistry.registerTickHandler(CLIENT, Side.CLIENT);
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
                    game.run(true, m -> m.onTickInGame(server));
                } else {
                    Minecraft mc = Minecraft.getMinecraft();
                    float partial = tickData.length > 0 && tickData[0] instanceof Float f ? f : 0F;
                    World world = Minecraft.mcc$get$theWorld(mc);
                    long now = world != null ? world.getTotalWorldTime() : 0L; // 1.2.5 world time advanced every tick; the modern day time stops with doDaylightCycle off
                    boolean changed = now != clock;
                    if (world != null) game.run(changed, m -> m.onTickInGame(partial, mc));
                    GuiScreen screen = Minecraft.mcc$get$currentScreen(mc);
                    // ModLoader skips a useClock GUI hook only when the clock is unchanged AND a world exists (on the title screen it runs every frame)
                    if (screen != null) gui.run(changed || world == null, m -> m.onTickInGUI(partial, mc, screen));
                    mcc$checkPacks();
                    org.kylepitcock.legacyconnector.v1_6_4.bridge.ModKeys125.poll(screen != null);
                    clock = now;
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
