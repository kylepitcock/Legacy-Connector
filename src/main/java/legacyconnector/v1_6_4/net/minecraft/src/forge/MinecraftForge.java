package legacyconnector.v1_6_4.net.minecraft.src.forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload;
import legacyconnector.v1_6_4.net.minecraft.src.BaseMod;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.EntityRegistry;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Crafting164;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164;

/**
 * Forge 3.x's registration API for ModLoader mods (Minecraft 1.2.x): ore dictionary, handlers, GUI handlers, network mods and tracked entities.
 * Each registration is translated onto what the layer has: ore registrations are the shim's ore dictionary (so recipes and tags see them), the
 * crafting, fuel, pickup, interact and save handlers are events and registries of the shim ({@link ForgeEvents}), GUI handlers the FML GUI
 * registry, and custom payload channels the layer's payload dispatch.
 */
public class MinecraftForge {
    public MinecraftForge() {}

    private static final List<IBucketHandler> BUCKET = new CopyOnWriteArrayList<>();
    private static final Map<Integer, NetworkMod> NETWORK_MODS = new HashMap<>();
    private static final Map<Class<?>, EntityTrackerInfo> TRACKERS = new HashMap<>();
    private static final Map<Integer, Class<?>> TRACKED_BY_ID = new HashMap<>();
    private static final Map<BaseMod, IGuiHandler> GUI_HANDLERS = new HashMap<>();
    private static final Set<AchievementPage> PAGES = new LinkedHashSet<>();
    private static final List<DungeonLoot> DUNGEON_LOOT = new ArrayList<>();

    // ---- handlers

    public static void registerCustomBucketHandler(IBucketHandler handler) {
        BUCKET.add(handler);
    }

    public static ItemStack fillCustomBucket(World world, int x, int y, int z) {
        for (IBucketHandler h : BUCKET) {
            ItemStack stack = h.fillCustomBucket(world, x, y, z);
            if (stack != null) return stack;
        }
        return null;
    }

    public static void registerCraftingHandler(ICraftingHandler handler) {
        CRAFTING.add(handler);
        Crafting164.registerCraftingHandler(new legacyconnector.v1_6_4.cpw.mods.fml.common.ICraftingHandler() {
            @Override
            public void onCrafting(EntityPlayer player, ItemStack item, legacyconnector.v1_7_10.net.minecraft.inventory.IInventory craftMatrix) {
                handler.onTakenFromCrafting(player, item, craftMatrix);
            }

            @Override
            public void onSmelting(EntityPlayer player, ItemStack item) {}
        });
    }

    public static void registerFuelHandler(IFuelHandler handler) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerFuelHandler(fuel -> handler.getItemBurnTime(fuel));
    }

    public static void registerPickupHandler(IPickupHandler handler) {
        ForgeEvents.pickup(handler);
    }

    public static void registerEntityInteractHandler(IEntityInteractHandler handler) {
        ForgeEvents.interact(handler);
    }

    static final List<IBonemealHandler> BONEMEAL = new CopyOnWriteArrayList<>();
    static final List<ICraftingHandler> CRAFTING = new CopyOnWriteArrayList<>();
    static final List<IMinecartHandler> MINECART = new CopyOnWriteArrayList<>();

    public static void registerBonemealHandler(IBonemealHandler handler) {
        BONEMEAL.add(handler);
        ForgeEvents.bonemeal(handler);
    }

    public static void registerSaveHandler(ISaveEventHandler handler) {
        ForgeEvents.save(handler);
    }

    public static void registerConnectionHandler(IConnectionHandler handler) {
        Network164.registerConnectionHandler(new legacyconnector.v1_6_4.cpw.mods.fml.common.network.IConnectionHandler() {
            @Override
            public void playerLoggedIn(legacyconnector.v1_7_10.cpw.mods.fml.common.network.Player player, legacyconnector.v1_7_10.net.minecraft.network.INetHandler netHandler,
                    INetworkManager manager) {
                handler.onConnect(manager);
                handler.onLogin(manager, null);
            }

            @Override
            public String connectionReceived(legacyconnector.v1_6_4.net.minecraft.network.NetLoginHandler netHandler, INetworkManager manager) {
                return null;
            }

            @Override
            public void connectionOpened(legacyconnector.v1_7_10.net.minecraft.network.INetHandler netClientHandler, String server, int port, INetworkManager manager) {
                handler.onConnect(manager);
            }

            @Override
            public void connectionOpened(legacyconnector.v1_7_10.net.minecraft.network.INetHandler netClientHandler,
                    legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer server, INetworkManager manager) {
                handler.onConnect(manager);
            }

            @Override
            public void connectionClosed(INetworkManager manager) {
                handler.onDisconnect(manager, "", null);
            }

            @Override
            public void clientLoggedIn(legacyconnector.v1_7_10.net.minecraft.network.INetHandler clientHandler, INetworkManager manager,
                    legacyconnector.v1_6_4.net.minecraft.network.packet.Packet1Login login) {
                handler.onLogin(manager, login);
            }
        });
    }

    // stored for mods that read them back; the layer has no equivalent event for these

    private static final List<Object> OTHER_HANDLERS = new CopyOnWriteArrayList<>();

    public static void registerSleepHandler(ISleepHandler handler) {
        OTHER_HANDLERS.add(handler);
    }

    public static void registerHoeHandler(IHoeHandler handler) {
        OTHER_HANDLERS.add(handler);
    }

    public static void registerDestroyToolHandler(IDestroyToolHandler handler) {
        OTHER_HANDLERS.add(handler);
    }

    public static void registerMinecartHandler(IMinecartHandler handler) {
        MINECART.add(handler);
    }

    public static void registerChunkLoadHandler(IChunkLoadHandler handler) {
        OTHER_HANDLERS.add(handler);
    }

    public static void registerChatHandler(IChatHandler handler) {
        OTHER_HANDLERS.add(handler);
    }

    public static void registerSpecialMobSpawnHandler(ISpecialMobSpawnHandler handler) {
        OTHER_HANDLERS.add(handler);
    }

    static final List<IArrowNockHandler> ARROW_NOCK = new CopyOnWriteArrayList<>();
    static final List<IArrowLooseHandler> ARROW_LOOSE = new CopyOnWriteArrayList<>();

    public static void registerArrowNockHandler(IArrowNockHandler handler) {
        ARROW_NOCK.add(handler);
        ForgeEvents.arrowNock(handler);
    }

    public static void registerArrowLooseHandler(IArrowLooseHandler handler) {
        ARROW_LOOSE.add(handler);
        ForgeEvents.arrowLoose(handler);
    }

    // ---- ore dictionary

    public static void registerOreHandler(IOreHandler handler) {
        for (Map.Entry<String, List<ItemStack>> e : new ArrayList<>(OreDictionary.mcc$registrations().entrySet())) {
            for (ItemStack ore : new ArrayList<>(e.getValue())) handler.registerOre(e.getKey(), ore);
        }
        ForgeEvents.ore(handler);
    }

    public static void registerOre(String oreClass, ItemStack ore) {
        OreDictionary.registerOre(oreClass, ore);
    }

    public static List<ItemStack> getOreClass(String oreClass) {
        return OreDictionary.getOres(oreClass);
    }

    // ---- tools and blocks

    public static void setToolClass(Item tool, String toolClass, int harvestLevel) {
        tool.setHarvestLevel(toolClass, harvestLevel);
    }

    public static void setBlockHarvestLevel(Block block, int metadata, String toolClass, int harvestLevel) {
        block.setHarvestLevel(toolClass, harvestLevel, metadata);
    }

    public static void setBlockHarvestLevel(Block block, String toolClass, int harvestLevel) {
        block.setHarvestLevel(toolClass, harvestLevel);
    }

    public static void removeBlockEffectiveness(Block block, int metadata, String toolClass) {
        legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.removeBlockEffectiveness(block, toolClass);
    }

    public static void removeBlockEffectiveness(Block block, String toolClass) {
        legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.removeBlockEffectiveness(block, toolClass);
    }

    public static int getBlockHarvestLevel(Block block, int metadata, String toolClass) {
        return block.getHarvestLevel(metadata);
    }

    public static void addGrassPlant(int blockId, int metadata, int weight) {
        Block b = legacyconnector.v1_6_4.net.minecraft.util.Ids.block(blockId);
        if (b != null) legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.addGrassPlant(b, metadata, weight);
    }

    public static void addGrassSeed(int itemId, int metadata, int count, int weight) {
        Item i = legacyconnector.v1_6_4.net.minecraft.util.Ids.item(itemId);
        if (i != null) legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.addGrassSeed(new ItemStack(i, count, metadata), weight);
    }

    public static OreQuery generateRecipes(Object... params) {
        throw new UnsupportedOperationException("generateRecipes is not available");
    }

    /** Forge 3.x's recipe helper result (the layer has no use for it; the type exists so mods that name it link). */
    public static class OreQuery {}

    // ---- versions

    public static void killMinecraft(String modName, String reason) {
        throw new RuntimeException(modName + ": " + reason);
    }

    public static void versionDetect(String modName, int major, int minor, int revision) {}

    public static void versionDetectStrict(String modName, int major, int minor, int revision) {}

    public static String getVersionString() {
        return "3.3.7.135";
    }

    // ---- dungeon

    public static void setDungeonLootTries(int number) {
        legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks.getInfo(legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST).setMax(number);
    }

    public static int getDungeonLootTries() {
        return legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks.getInfo(legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST).getMax();
    }

    public static float addDungeonMob(String name, float rarity) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.DungeonHooks.addDungeonMob(name, (int) rarity);
    }

    public static float removeDungeonMob(String name) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.DungeonHooks.removeDungeonMob(name);
    }

    public static String getRandomDungeonMob(java.util.Random random) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.DungeonHooks.getRandomDungeonMob(random);
    }

    public static void addDungeonLoot(ItemStack item, float chance) {
        int size = ItemStack.mcc$get$stackSize(item);
        addDungeonLoot(item, chance, size, size);
    }

    public static float addDungeonLoot(ItemStack item, float chance, int min, int max) {
        DUNGEON_LOOT.add(new DungeonLoot(item, min, max));
        legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks.addItem(legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST,
                new legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent(item, min, max, Math.max(1, (int) chance)));
        return chance;
    }

    public static float removeDungeonLoot(ItemStack item) {
        DUNGEON_LOOT.removeIf(l -> l.equals(item));
        return 0.0F;
    }

    public static float removeDungeonLoot(ItemStack item, int min, int max) {
        DUNGEON_LOOT.removeIf(l -> l.equals(item, min, max));
        return 0.0F;
    }

    public static ItemStack getRandomDungeonLoot(java.util.Random random) {
        if (DUNGEON_LOOT.isEmpty()) return null;
        return DUNGEON_LOOT.get(random.nextInt(DUNGEON_LOOT.size())).generateStack(random);
    }

    // ---- achievements, minecarts

    public static void registerAchievementPage(AchievementPage page) {
        PAGES.add(page);
    }

    public static AchievementPage getAchievementPage(int index) {
        return new ArrayList<>(PAGES).get(index);
    }

    public static AchievementPage getAchievementPage(String name) {
        for (AchievementPage p : PAGES) if (p.getName().equals(name)) return p;
        return null;
    }

    public static Set<AchievementPage> getAchievementPages() {
        return Collections.unmodifiableSet(PAGES);
    }

    public static boolean isAchievementInPages(legacyconnector.v1_7_10.net.minecraft.stats.Achievement achievement) {
        for (AchievementPage p : PAGES) if (p.getAchievements().contains(achievement)) return true;
        return false;
    }

    // ---- network mods and tracked entities

    public static boolean registerEntity(Class<?> entityClass, NetworkMod mod, int id, int range, int updateFrequency, boolean sendVelocityInfo) {
        if (TRACKED_BY_ID.containsKey(id) && TRACKED_BY_ID.get(id) != entityClass) {
            // Forge refused a tracking id twice
            return false;
        }
        TRACKERS.put(entityClass, new EntityTrackerInfo(mod, id, range, updateFrequency, sendVelocityInfo));
        TRACKED_BY_ID.put(id, entityClass);
        NETWORK_MODS.putIfAbsent(getModID(mod), mod);
        EntityRegistry.registerModEntity(entityClass, entityClass.getSimpleName(), id, mod, range, updateFrequency, sendVelocityInfo);
        return true;
    }

    public static EntityTrackerInfo getEntityTrackerInfo(Entity entity, boolean forSpawning) {
        return TRACKERS.get(entity.getClass());
    }

    public static Class<?> getEntityClass(int modId, int entityId) {
        return TRACKED_BY_ID.get(entityId);
    }

    public static NetworkMod getModByID(int id) {
        return NETWORK_MODS.get(id);
    }

    /** Forge numbered network mods by the hash of their class name (stable between runs). */
    public static int getModID(NetworkMod mod) {
        return mod == null ? 0 : mod.getClass().getName().hashCode();
    }

    public static NetworkMod[] getNetworkMods() {
        return NETWORK_MODS.values().toArray(new NetworkMod[0]);
    }

    // ---- GUI

    /** {@code EntityPlayer.openGui(mod, id, world, x, y, z)} calls the handler; its element is the Container on the server and the GuiScreen on the client. */
    public static void setGuiHandler(BaseMod mod, IGuiHandler handler) {
        GUI_HANDLERS.put(mod, handler);
        legacyconnector.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(mod,
                new legacyconnector.v1_7_10.cpw.mods.fml.common.network.IGuiHandler() {
                    @Override
                    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
                        return handler.getGuiElement(id, player, world, x, y, z);
                    }

                    @Override
                    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
                        return handler.getGuiElement(id, player, world, x, y, z);
                    }
                });
    }

    public static IGuiHandler getGuiHandler(BaseMod mod) {
        return GUI_HANDLERS.get(mod);
    }

    // ---- packets

    public static void sendPacket(INetworkManager network, legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        if (network != null && packet != null) network.addToSendQueue(packet);
    }

    /** A payload for {@code mod}'s {@code onPacketData}: Forge 3.x multiplexed mods over one channel; here each mod has its own. */
    public static void sendPacket(INetworkManager network, NetworkMod mod, short packetId, byte[] data) {
        if (network == null || mod == null) return;
        String channel = channelOf(mod);
        ensureChannel(mod, channel);
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(bytes)) {
            out.writeShort(packetId);
            out.write(data);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        network.addToSendQueue(new Packet250CustomPayload(channel, bytes.toByteArray()));
    }

    public static void sendTileEntityPacket(INetworkManager network, int x, short y, int z, byte type, int a, int b, int c) {}

    private static final Set<String> CHANNELS = new LinkedHashSet<>();

    private static String channelOf(NetworkMod mod) {
        String n = mod.getClass().getSimpleName();
        return "FML|" + (n.length() > 14 ? n.substring(0, 14) : n);
    }

    private static synchronized void ensureChannel(NetworkMod mod, String channel) {
        if (!CHANNELS.add(channel)) return;
        Network164.registerChannel((manager, packet, player) -> {
            try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(packet.data))) {
                short id = in.readShort();
                byte[] rest = in.readAllBytes();
                mod.onPacketData(manager, id, rest);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }, channel, null);
    }

    /** True on the client side of the process (the integrated server has both). */
    public static boolean isClient() {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().getSide() == legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.CLIENT;
    }

    // ---- minecart registry (data only)

    private static final Map<Class<?>, ItemStack> CARTS = new HashMap<>();

    public static void registerMinecart(Class<?> cart, ItemStack item) {
        CARTS.put(cart, item);
    }

    public static void registerMinecart(Class<?> cart, int type, ItemStack item) {
        CARTS.put(cart, item);
    }

    public static void removeMinecart(Class<?> cart, int type) {
        CARTS.remove(cart);
    }

    public static Set<ItemStack> getAllCartItems() {
        return new LinkedHashSet<>(CARTS.values());
    }

    public static Class<?> getCartClassForItem(ItemStack item) {
        for (Map.Entry<Class<?>, ItemStack> e : CARTS.entrySet()) if (e.getValue().isItemEqual(item)) return e.getKey();
        return null;
    }

    public static int getCartTypeForItem(ItemStack item) {
        return 0;
    }

    public static ItemStack getItemForCart(Class<?> cart) {
        return CARTS.get(cart);
    }

    public static ItemStack getItemForCart(Class<?> cart, int type) {
        return CARTS.get(cart);
    }

    public static ItemStack getItemForCart(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart cart) {
        return CARTS.get(cart.getClass());
    }
}
