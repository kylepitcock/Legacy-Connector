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
        Network164.registerChannel((manager, packet, player) -> {
            Packet230ModLoader p = Packet230ModLoader.decode(packet.data);
            BaseModMp mod = MODS.get(p.modId);
            if (mod != null) mod.handlePacket(p);
        }, CHANNEL, Side.CLIENT);
        Network164.registerChannel((manager, packet, player) -> {
            Packet230ModLoader p = Packet230ModLoader.decode(packet.data);
            BaseModMp mod = MODS.get(p.modId);
            if (mod == null || !(player instanceof EntityPlayerMP mp)) return;
            if (p.packetType == KEY_PACKET && p.dataInt.length > 0) mod.handleSendKey(mp, p.dataInt[0]);
            else mod.handlePacket(p, mp);
        }, CHANNEL, Side.SERVER);
        legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new Object() {
            @legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent
            public void onLogin(legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
                if (e.player instanceof EntityPlayerMP mp) {
                    for (BaseModMp m : MODS.values()) m.handleLogin(mp);
                }
            }
        });
    }

    /** Called by the host for every {@code BaseModMp} (see {@link BaseMod}'s init handler). */
    static void mcc$register(BaseModMp mod) {
        MODS.put(mod.getId(), mod);
        BY_CLASS.put(mod.getClass(), mod);
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

    public static void registerGUI(BaseModMp mod, int id) {}

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

    public static void sendTileEntityPacket(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity tile) {}

    public static void registerEntityTrackerEntry(Class<?> entityClass, int id) {}

    public static void registerEntityTrackerEntry(Class<?> entityClass, boolean isLiving, int id) {}

    public static void registerEntityTracker(Class<?> entityClass, int updateRange, int updateInterval) {}

    public static void registerNetClientHandlerEntity(Class<?> entityClass, int id) {}

    public static void registerNetClientHandlerEntity(Class<?> entityClass, boolean isLiving, int id) {}

    public static void handleAllLogins(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        for (BaseModMp m : MODS.values()) m.onClientLogin(player);
    }
}
