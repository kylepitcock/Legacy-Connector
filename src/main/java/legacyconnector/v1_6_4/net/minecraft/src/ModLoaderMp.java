package legacyconnector.v1_6_4.net.minecraft.src;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164;

/**
 * ModLoaderMP's static API (Minecraft 1.2.x). Its packets ({@link Packet230ModLoader}) travel as custom payloads on {@link #CHANNEL}, the layer's own
 * packet transport; the receiving side finds the mod by the id in the packet and calls its {@code handlePacket}.
 */
public final class ModLoaderMp {
    public static final String NAME = "ModLoaderMP";
    public static final String VERSION = "1.2.5";
    /** The payload channel of ModLoaderMP's packets. */
    public static final String CHANNEL = "ModLoaderMP";

    private static final Map<Integer, BaseModMp> MODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, BaseModMp> BY_CLASS = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private ModLoaderMp() {}

    /** Hooks the packet channel; ModLoaderMP did this when the game started (mods call it too, so it is idempotent). */
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        Network164.registerChannel((manager, packet, player) -> receiveOnClient(packet.data), CHANNEL, Side.CLIENT);
        Network164.registerChannel((manager, packet, player) -> receiveOnServer(packet.data, player), CHANNEL, Side.SERVER);
        legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new Object() {
            @legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent
            public void onLogin(legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
                if (e.player instanceof EntityPlayerMP mp) {
                    for (BaseModMp m : MODS.values()) m.handleLogin(mp);
                }
            }
        });
    }

    /** Packet ids ModLoaderMP kept for itself (the "mod id" of the packet is the hash of the word). */
    static final int ID_MODLOADERMP = "ModLoaderMP".hashCode();
    static final int ID_SPAWN = "Spawn".hashCode();

    /**
     * The client end of the channel ({@code ModLoaderMp.handleAllPackets}, read from the real class): the reserved id "ModLoaderMP" carries the mod check (type 0,
     * the layer sends none) and the tile entity packet (type 1); a packet of any other mod goes to that mod's {@code handlePacket(packet)}. The "Spawn" packets of
     * custom entities travel with the entity's native spawn data instead ({@link #mcc$readSpawn}), never on this channel.
     */
    static void receiveOnClient(byte[] data) {
        Packet230ModLoader p = Packet230ModLoader.decode(data);
        if (p.modId == ID_MODLOADERMP) {
            if (p.packetType == 1) handleTileEntityPacket(p);
            return;
        }
        if (p.modId == ID_SPAWN) {
            log("A Spawn packet arrived on the ModLoaderMP channel; custom entity spawns travel with the entity's own spawn data in this layer and are ignored here.");
            return;
        }
        BaseModMp mod = MODS.get(p.modId);
        if (mod != null) mod.handlePacket(p);
    }

    /**
     * The tile entity packet ({@code handleTileEntityPacket} of the real class): {@code dataInt = {mod id, x, y, z, type, ints...}}; the mod with that id gets
     * {@code handleTileEntityPacket(x, y, z, type, ints, floats, strings)}.
     */
    private static void handleTileEntityPacket(Packet230ModLoader p) {
        if (p.dataInt == null || p.dataInt.length < 5) {
            log("Bad TileEntityPacket received.");
            return;
        }
        BaseModMp mod = MODS.get(p.dataInt[0]);
        if (mod == null) return;
        int[] rest = new int[p.dataInt.length - 5];
        System.arraycopy(p.dataInt, 5, rest, 0, rest.length);
        mod.handleTileEntityPacket(p.dataInt[1], p.dataInt[2], p.dataInt[3], p.dataInt[4], rest, p.dataFloat, p.dataString);
    }

    /** The server end: a packet (or key press) the client half sent goes to {@code handlePacket(packet, player)} / {@code handleSendKey}. */
    static void receiveOnServer(byte[] data, Object player) {
        Packet230ModLoader p = Packet230ModLoader.decode(data);
        BaseModMp mod = MODS.get(p.modId);
        if (mod == null || !(player instanceof EntityPlayerMP mp)) return;
        if (p.packetType == KEY_PACKET && p.dataInt.length > 0) mod.handleSendKey(mp, p.dataInt[0]);
        else mod.handlePacket(p, mp);
    }

    /** Test seam: the id table without the channel registration. */
    static void mcc$put(BaseModMp mod) {
        MODS.put(mod.getId(), mod);
        BY_CLASS.put(mod.getClass(), mod);
    }

    /** Called by the host for every {@code BaseModMp} (see {@link BaseMod}'s init handler). */
    static void mcc$register(BaseModMp mod) {
        mcc$put(mod);
        initialize();
    }

    public static BaseModMp getModInstance(Class<? extends BaseModMp> modClass) {
        return BY_CLASS.get(modClass);
    }

    private static Packet250CustomPayload wrap(BaseModMp mod, Packet230ModLoader packet) {
        if (packet == null) throw new IllegalArgumentException("packet cannot be null");
        if (mod != null) packet.modId = mod.getId();
        return new Packet250CustomPayload(CHANNEL, packet.encode());
    }

    /** Client to server. */
    public static void sendPacket(BaseModMp mod, Packet230ModLoader packet) {
        initialize();
        Network164.sendToServer(wrap(mod, packet));
    }

    /** Server to one client. */
    public static void sendPacketTo(BaseModMp mod, EntityPlayerMP player, Packet230ModLoader packet) {
        initialize();
        Network164.sendToPlayer(wrap(mod, packet), player);
    }

    /** Server to every client. */
    public static void sendPacketToAll(BaseModMp mod, Packet230ModLoader packet) {
        initialize();
        Network164.sendToAll(wrap(mod, packet));
    }

    /** The packet type of a key the client sent with {@link #sendKey} (ModLoaderMP used a reserved mod id; the layer a reserved type). */
    private static final int KEY_PACKET = Integer.MIN_VALUE + 1;

    /** Client to server: a key press for the mod's {@code handleSendKey}. */
    public static void sendKey(BaseModMp mod, int key) {
        Packet230ModLoader packet = new Packet230ModLoader();
        packet.packetType = KEY_PACKET;
        packet.dataInt = new int[] {key};
        sendPacket(mod, packet);
    }

    private static final Map<Integer, BaseModMp> GUI_MODS = new ConcurrentHashMap<>();
    private static final java.util.Set<BaseModMp> GUI_HANDLERS = ConcurrentHashMap.newKeySet();
    private static final Map<legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer, legacyconnector.v1_7_10.net.minecraft.inventory.Container> STAGED =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * The mod answers GUI id {@code id}: the server's {@code ModLoader.openGUI(player, id, inventory, container)} opens that container for the player
     * and the client calls the mod's {@code handleGUI(id)} for the screen. Both ride the FML GUI registry ({@code EntityPlayer.openGui}), which
     * the layer turns into a menu on the server and a container screen on the client.
     */
    public static void registerGUI(BaseModMp mod, int id) {
        if (!org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.active()) return; // implemented for Minecraft 1.2.5 only; a no-op for the other versions, as before
        // ModLoaderMP logged "RegisterGUI error: inventoryType already registered." and kept the first mod
        if (GUI_MODS.putIfAbsent(id, mod) != null && GUI_MODS.get(id) != mod) {
            log("RegisterGUI error: inventoryType already registered.");
            return;
        }
        if (!GUI_HANDLERS.add(mod)) return;
        legacyconnector.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(mod,
                new legacyconnector.v1_7_10.cpw.mods.fml.common.network.IGuiHandler() {
                    @Override
                    public Object getServerGuiElement(int guiId, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
                            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
                        return STAGED.remove(player);
                    }

                    @Override
                    public Object getClientGuiElement(int guiId, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
                            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
                        BaseModMp m = GUI_MODS.get(guiId);
                        return m == null ? null : m.handleGUI(guiId);
                    }
                });
    }

    /** The server side of {@code ModLoader.openGUI(player, id, inventory, container)}: false when no mod registered the id. */
    static boolean mcc$openGui(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, int id,
            legacyconnector.v1_7_10.net.minecraft.inventory.Container container) {
        if (!org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.active()) return false; // implemented for Minecraft 1.2.5 only; a no-op for the other versions, as before
        BaseModMp mod = GUI_MODS.get(id);
        if (mod == null || player == null || container == null) return false;
        STAGED.put(player, container);
        player.openGui(mod, id, legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$worldObj(player), 0, 0, 0);
        return true;
    }

    public static void log(String message) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/ModLoaderMP").info(message);
    }

    /** Chat to every player (the sender, when given, is named in front). */
    public static void sendChatToAll(String sender, String message) {
        sendChatToAll(sender == null || sender.isEmpty() ? message : "<" + sender + "> " + message);
    }

    public static void sendChatToAll(String message) {
        var server = legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) server.mcc$modern().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(message), false);
    }

    public static void sendChatToOps(String sender, String message) {
        sendChatToAll(sender, message);
    }

    public static void sendChatToOps(String message) {
        sendChatToAll(message);
    }

    /**
     * Server: the packet that describes a mod tile entity to the clients ({@code getTileEntityPacket} of the real class). A mod returns it from
     * {@code TileEntity.getDescriptionPacket()}; the layer's block entity sync then carries it to every client that tracks the tile (as a custom payload of the update),
     * where {@code dataInt = {mod id, x, y, z, type, ints...}} reaches {@code BaseModMp.handleTileEntityPacket}. The real packet was flagged as chunk data too, which
     * the update packet of a block entity is by nature.
     */
    public static legacyconnector.v1_7_10.net.minecraft.network.Packet getTileEntityPacket(BaseModMp mod, int x, int y, int z, int type, int[] ints, float[] floats,
            String[] strings) {
        Packet230ModLoader packet = new Packet230ModLoader();
        packet.modId = ID_MODLOADERMP;
        packet.packetType = 1;
        int extra = ints == null ? 0 : ints.length;
        int[] all = new int[extra + 5];
        all[0] = mod.getId();
        all[1] = x;
        all[2] = y;
        all[3] = z;
        all[4] = type;
        if (extra != 0) System.arraycopy(ints, 0, all, 5, extra);
        packet.dataInt = all;
        packet.dataFloat = floats;
        packet.dataString = strings;
        return new Packet250CustomPayload(CHANNEL, packet.encode());
    }

    /** Server: sends the tile's description packet to every client ({@code sendPacketToAll(tile.getDescriptionPacket())}). */
    public static void sendTileEntityPacket(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity tile) {
        if (!org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.active()) return; // implemented for Minecraft 1.2.5 only; a no-op for the other versions, as before
        initialize();
        Network164.sendToAll(tile.getDescriptionPacket());
    }

    // ------------------------------------------------------------------ custom entity spawns and trackers

    private static final Map<Class<?>, int[]> TRACKERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, EntityTrackerEntry2> TRACKER_ENTRIES = new ConcurrentHashMap<>();
    private static final Map<Integer, NetClientHandlerEntity> CLIENT_ENTITIES = new ConcurrentHashMap<>();

    static {
        org.kylepitcock.legacyconnector.v1_7_10.entity.LegacySpawnExtras.register(ModLoaderMp::ownerSpawnData);
    }

    /** Server: tracker range and update interval of an entity class (blocks, ticks); the entity's native tracker uses them ({@code LegacyHosts.setTracking}). */
    public static void registerEntityTracker(Class<?> entityClass, int updateRange, int updateInterval) {
        if (!org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.active()) return; // implemented for Minecraft 1.2.5 only; a no-op for the other versions, as before
        if (TRACKERS.putIfAbsent(entityClass, new int[] {updateRange, updateInterval}) != null) {
            log("RegisterEntityTracker error: entityClass already registered.");
            return;
        }
        org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.setTracking(entityClass, updateRange, updateInterval);
    }

    public static void registerEntityTrackerEntry(Class<?> entityClass, int id) {
        registerEntityTrackerEntry(entityClass, false, id);
    }

    /** Server: the spawn type id of an entity class and whether its spawn packet names an owner (the entity's {@code owner} field). */
    public static void registerEntityTrackerEntry(Class<?> entityClass, boolean hasOwner, int id) {
        if (!org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.active()) return; // implemented for Minecraft 1.2.5 only; a no-op for the other versions, as before
        if (id > 255) log("RegisterEntityTrackerEntry error: entityId cannot be greater than 255.");
        if (TRACKER_ENTRIES.putIfAbsent(entityClass, new EntityTrackerEntry2(id, hasOwner)) != null) {
            log("RegisterEntityTrackerEntry error: entityClass already registered.");
        }
    }

    /** The server's row for an entity (by its exact class, as the real class did), or null. */
    public static EntityTrackerEntry2 handleEntityTrackerEntries(Object entity) {
        return TRACKER_ENTRIES.get(entity.getClass());
    }

    public static void registerNetClientHandlerEntity(Class<?> entityClass, int id) {
        registerNetClientHandlerEntity(entityClass, false, id);
    }

    /** Client: the entity class of a spawn type (ids above 127 are stored as the signed byte the packet carries, as in the real class). */
    public static void registerNetClientHandlerEntity(Class<?> entityClass, boolean hasOwner, int id) {
        if (!org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.active()) return; // implemented for Minecraft 1.2.5 only; a no-op for the other versions, as before
        if (id > 255) {
            log("RegisterNetClientHandlerEntity error: entityId cannot be greater than 255.");
            return;
        }
        if (CLIENT_ENTITIES.containsKey(id)) {
            log("RegisterNetClientHandlerEntity error: entityId already registered.");
            return;
        }
        CLIENT_ENTITIES.put(id > 127 ? id - 256 : id, new NetClientHandlerEntity(entityClass, hasOwner));
    }

    public static NetClientHandlerEntity handleNetClientHandlerEntities(int type) {
        return CLIENT_ENTITIES.get(type);
    }

    private static int wireType(EntityTrackerEntry2 entry) {
        return entry.entityId > 127 ? entry.entityId - 256 : entry.entityId;
    }

    /**
     * Server half of {@code EntityTrackerEntry.getSpawnPacket} for an {@link ISpawnable} entity of a registered class: its {@code getSpawnPacket()} with the
     * "Spawn" id and the type id, written into the entity's spawn data. Nothing is written for an unregistered class, a build without {@code getSpawnPacket}
     * or a null packet.
     */
    static void mcc$writeSpawn(ISpawnable entity, io.netty.buffer.ByteBuf out) {
        EntityTrackerEntry2 entry = handleEntityTrackerEntries(entity);
        if (entry == null) return;
        Packet230ModLoader packet;
        try {
            packet = entity.getSpawnPacket();
        } catch (RuntimeException | LinkageError e) {
            ModLoader.throwException(String.format("Error sending spawn packet for entity of type %s.", entity.getClass()), e);
            return;
        }
        if (packet == null) return;
        packet.modId = ID_SPAWN;
        packet.packetType = wireType(entry);
        out.writeBytes(packet.encode());
    }

    /**
     * Client half ({@code handleAllPackets}, "Spawn"): the packet is decoded, its type must be registered with {@code registerNetClientHandlerEntity} for a class
     * the entity is an instance of, and the entity's {@code spawn(packet)} runs. The entity itself and its id come from the native spawn.
     */
    static void mcc$readSpawn(ISpawnable entity, io.netty.buffer.ByteBuf in) {
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        if (bytes.length == 0) return;
        Packet230ModLoader packet = Packet230ModLoader.decode(bytes);
        NetClientHandlerEntity row = handleNetClientHandlerEntities(packet.packetType);
        if (row == null || !row.entityClass.isInstance(entity)) {
            log("A spawn packet of type " + packet.packetType + " arrived for " + entity.getClass().getName() + ", which is not registered with registerNetClientHandlerEntity.");
            return;
        }
        try {
            entity.spawn(packet);
        } catch (RuntimeException | LinkageError e) {
            ModLoader.throwException(String.format("Error initializing entity of type %s.", packet.packetType), e);
        }
    }

    /**
     * Spawn data for an entity that is not {@link ISpawnable} but whose tracker entry says it has an owner: the real spawn packet carried the owner's entity id
     * (the entity's own id when the {@code owner} field was null) and the client stored the entity with that id in the public {@code owner} field.
     */
    private static legacyconnector.v1_7_10.cpw.mods.fml.common.registry.IEntityAdditionalSpawnData ownerSpawnData(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        EntityTrackerEntry2 entry = TRACKER_ENTRIES.get(entity.getClass());
        if (entry == null || !entry.entityHasOwner || entity instanceof ISpawnable) return null;
        final int type = wireType(entry);
        return new legacyconnector.v1_7_10.cpw.mods.fml.common.registry.IEntityAdditionalSpawnData() {
            @Override
            public void writeSpawnData(io.netty.buffer.ByteBuf out) {
                try {
                    java.lang.reflect.Field field = entity.getClass().getField("owner");
                    if (!legacyconnector.v1_7_10.net.minecraft.entity.Entity.class.isAssignableFrom(field.getType())) {
                        throw new IllegalStateException(String.format("Entity's owner field must be of type Entity, but it is of type %s.", field.getType()));
                    }
                    legacyconnector.v1_7_10.net.minecraft.entity.Entity owner = (legacyconnector.v1_7_10.net.minecraft.entity.Entity) field.get(entity);
                    out.writeByte(type);
                    out.writeInt((owner != null ? owner : entity).getEntityId());
                } catch (ReflectiveOperationException | RuntimeException e) {
                    ModLoader.throwException(String.format("Error sending spawn packet for entity of type %s.", entity.getClass()), e);
                }
            }

            @Override
            public void readSpawnData(io.netty.buffer.ByteBuf in) {
                if (in.readableBytes() < 5) return;
                in.readByte();
                int ownerId = in.readInt();
                try {
                    java.lang.reflect.Field field = entity.getClass().getField("owner");
                    legacyconnector.v1_7_10.net.minecraft.entity.Entity owner = entity.mcc$world() == null ? null : entity.mcc$world().getEntityByID(ownerId);
                    if (owner == null) {
                        log("Received spawn packet for entity with owner, but owner was not found.");
                    } else if (field.getType().isAssignableFrom(owner.getClass())) {
                        field.set(entity, owner);
                    } else {
                        throw new IllegalStateException(String.format("Tried to assign an entity of type %s to entity owner, which is of type %s.", owner.getClass(), field.getType()));
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    ModLoader.throwException(String.format("Error initializing entity of type %s.", type), e);
                }
            }
        };
    }

    public static void handleAllLogins(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        for (BaseModMp m : MODS.values()) m.onClientLogin(player);
    }
}
