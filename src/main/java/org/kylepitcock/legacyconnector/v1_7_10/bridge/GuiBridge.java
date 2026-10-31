package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.IGuiHandler;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;
import org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FML 7.10 GUI opening ({@code player.openGui(mod, id, world, x, y, z)} + {@code IGuiHandler}) on modern menus.
 *
 * <p>Server: the handler's server element (a legacy Container) is hosted by a {@link LegacyMenu}; the client
 * receives mod id, gui id and position as menu extra data, asks the same handler for its client element (a
 * legacy GuiContainer) and shows it in a modern screen wrapping it. GUIs without a server container are opened
 * client-side through a small payload, like FML did.</p>
 */
public final class GuiBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Gui");
    static final String FAMILY = "1.7.10-gui";
    private static final Map<Object, IGuiHandler> HANDLERS = new HashMap<>();
    private static MenuType<LegacyMenu> menuType;

    static {
        LegacyNetwork.registerReceiver(FAMILY, GuiBridge::receive);
    }

    private GuiBridge() {}

    public static void setMenuType(MenuType<LegacyMenu> type) {
        menuType = type;
    }

    public static MenuType<LegacyMenu> menuType() {
        return menuType;
    }

    public static synchronized void register(Object mod, IGuiHandler handler) {
        HANDLERS.put(mod, handler);
    }

    private static synchronized IGuiHandler handler(Object mod) {
        IGuiHandler h = HANDLERS.get(mod);
        if (h != null) return h;
        // Mods hand openGui a different object of the mod's class than the one they registered with (AE2's static INSTANCE
        // against the instance FML built): FML resolved both to the same container.
        for (var e : HANDLERS.entrySet()) if (e.getKey() != null && mod != null && e.getKey().getClass() == mod.getClass()) return e.getValue();
        return null;
    }

    private static LegacyModRecord recordOf(Object mod) {
        for (LegacyModRecord r : LegacyMods.all()) if (r.instance == mod) return r;
        for (LegacyModRecord r : LegacyMods.all()) if (r.instance != null && mod != null && r.instance.getClass() == mod.getClass()) return r;
        return null;
    }

    public static void openGui(EntityPlayer player, Object mod, int id, World world, int x, int y, int z) {
        IGuiHandler handler = handler(mod);
        LegacyModRecord record = recordOf(mod);
        // FML decided by the player, not the world: ChromatiCraft's Lexicon opens its pages with a null world
        boolean remote = world != null ? world.isRemote
                : !(player.mcc$entity instanceof ServerPlayer) && (player.mcc$entity == null || player.mcc$entity.level().isClientSide());
        LOG.debug("openGui id {} of {} at {} {} {} (remote: {})", id, mod, x, y, z, remote);
        if (handler == null || record == null) {
            LOG.warn("openGui({}) for {} without a registered IGuiHandler (handler {}, mod record {}, handlers registered for {})", id, mod, handler, record, HANDLERS.keySet().stream().map(k -> k + "@" + k.getClass().getClassLoader()).toList() + " asked by loader " + mod.getClass().getClassLoader());
            return;
        }
        if (remote) {
            // FML: a client-side openGui shows a client-only GUI immediately; container GUIs come from the server.
            if (FMLEnvironment.dist.isClient()) Client.showClientOnly(handler, player, id, world, x, y, z);
            return;
        }
        if (!(player.mcc$entity instanceof ServerPlayer sp)) return;
        Object element = handler.getServerGuiElement(id, player, world, x, y, z);
        if (element instanceof Container container) {
            sp.openMenu(new SimpleMenuProvider((windowId, inv, p) -> new LegacyMenu(menuType, windowId, inv, container, null), Component.empty()),
                    buf -> {
                        buf.writeUtf(record.legacyId);
                        buf.writeVarInt(id);
                        buf.writeInt(x);
                        buf.writeInt(y);
                        buf.writeInt(z);
                    });
        } else {
            ByteBuf buf = org.kylepitcock.legacyconnector.core.network.LegacyBuffers.buffer();
            byte[] modId = record.legacyId.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(modId.length).writeBytes(modId).writeInt(x).writeInt(y).writeInt(z);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            PacketDistributor.sendToPlayer(sp, new LegacyPayload(FAMILY, "open", id, data));
        }
    }

    /** Client side of {@link MenuType} creation: rebuild the legacy GUI from the menu extra data. */
    public static LegacyMenu clientMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf buf) {
        String modId = buf.readUtf();
        int id = buf.readVarInt();
        int x = buf.readInt(), y = buf.readInt(), z = buf.readInt();
        return Client.createMenu(windowId, inv, modId, id, x, y, z);
    }

    public static void sendProgressBar(ServerPlayer player, int windowId, int id, int value) {
        ByteBuf buf = org.kylepitcock.legacyconnector.core.network.LegacyBuffers.buffer().writeInt(windowId).writeInt(id).writeInt(value);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        PacketDistributor.sendToPlayer(player, new LegacyPayload(FAMILY, "bar", 0, data));
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        if (!FMLEnvironment.dist.isClient()) return;
        ByteBuf buf = org.kylepitcock.legacyconnector.core.network.LegacyBuffers.wrap(payload.data());
        context.enqueueWork(() -> {
            switch (payload.channel()) {
                case "bar" -> Client.progressBar(buf.readInt(), buf.readInt(), buf.readInt());
                case "open" -> {
                    byte[] modId = new byte[buf.readInt()];
                    buf.readBytes(modId);
                    Client.openFromServer(new String(modId, StandardCharsets.UTF_8), payload.discriminator(), buf.readInt(), buf.readInt(), buf.readInt());
                }
                default -> { }
            }
        });
    }

    static IGuiHandler handlerFor(String legacyId) {
        LegacyModRecord r = LegacyMods.byLegacyId(legacyId);
        return r == null ? null : handler(r.instance);
    }

    /** Client-only half, in its own class so dedicated servers never load client classes. */
    static final class Client {
        static LegacyMenu createMenu(int windowId, Inventory inv, String modId, int id, int x, int y, int z) {
            return org.kylepitcock.legacyconnector.v1_7_10.client.ClientGui.createMenu(menuType, handlerFor(modId), windowId, inv, id, x, y, z);
        }

        static void showClientOnly(IGuiHandler handler, EntityPlayer player, int id, World world, int x, int y, int z) {
            org.kylepitcock.legacyconnector.v1_7_10.client.ClientGui.showClientOnly(handler, player, id, world, x, y, z);
        }

        static void openFromServer(String modId, int id, int x, int y, int z) {
            IGuiHandler handler = handlerFor(modId);
            if (handler != null) org.kylepitcock.legacyconnector.v1_7_10.client.ClientGui.openFromServer(handler, id, x, y, z);
        }

        static void progressBar(int windowId, int id, int value) {
            org.kylepitcock.legacyconnector.v1_7_10.client.ClientGui.progressBar(windowId, id, value);
        }
    }
}
