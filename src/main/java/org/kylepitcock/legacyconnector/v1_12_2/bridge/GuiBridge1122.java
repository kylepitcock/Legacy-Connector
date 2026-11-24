package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.IGuiHandler;
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
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FML 1.12 GUI opening ({@code player.openGui(mod, id, world, x, y, z)} + {@code IGuiHandler}) on modern menus.
 *
 * <p>Server: the handler's server element (a legacy Container) is hosted by a {@link LegacyMenu1122}; the client receives mod id, gui id
 * and position as menu extra data, asks the same handler for its client element (a legacy GuiContainer) and shows it in a modern screen
 * wrapping it. GUIs without a server container are opened client-side through a small payload, as FML did.</p>
 */
public final class GuiBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Gui");
    static final String FAMILY = "1.12.2-gui";
    private static final Map<Object, IGuiHandler> HANDLERS = new ConcurrentHashMap<>();
    private static MenuType<LegacyMenu1122> menuType;

    static {
        LegacyNetwork.registerReceiver(FAMILY, GuiBridge1122::receive);
    }

    private GuiBridge1122() {}

    public static void setMenuType(MenuType<LegacyMenu1122> type) {
        menuType = type;
    }

    public static MenuType<LegacyMenu1122> menuType() {
        return menuType;
    }

    public static void register(Object mod, IGuiHandler handler) {
        if (mod != null && handler != null) HANDLERS.put(mod, handler);
    }

    /** The handler registered for a mod; mods pass openGui another object of their mod class than the one they registered with. */
    public static IGuiHandler handler(Object mod) {
        if (mod == null) return null;
        IGuiHandler h = HANDLERS.get(mod);
        if (h != null) return h;
        // another object of the mod's class (not another mod's id: every id is a String)
        if (!(mod instanceof String)) for (var e : HANDLERS.entrySet()) if (e.getKey().getClass() == mod.getClass()) return e.getValue();
        if (mod instanceof String id) {
            LegacyModRecord r = LegacyMods.byLegacyId(id);
            if (r != null && r.instance != null && r.instance != mod) {
                h = HANDLERS.get(r.instance);
                if (h != null) return h;
                for (var e : HANDLERS.entrySet()) if (e.getKey().getClass() == r.instance.getClass()) return e.getValue();
            }
            return null;
        }
        // registered under the mod id (FML took either: Techguns registers with its id and opens with its instance, and no machine opened)
        LegacyModRecord r = recordOf(mod);
        if (r != null && r.legacyId != null) return HANDLERS.get(r.legacyId);
        return null;
    }

    private static LegacyModRecord recordOf(Object mod) {
        if (mod instanceof String id) return LegacyMods.byLegacyId(id);
        for (LegacyModRecord r : LegacyMods.all()) if (r.instance == mod) return r;
        for (LegacyModRecord r : LegacyMods.all()) if (r.instance != null && mod != null && r.instance.getClass() == mod.getClass()) return r;
        return null;
    }

    public static void openGui(EntityPlayer player, Object mod, int id, World world, int x, int y, int z) {
        IGuiHandler handler = handler(mod);
        LegacyModRecord record = recordOf(mod);
        if (handler == null || record == null || world == null) {
            LOG.warn("openGui({}) for {} without a registered IGuiHandler (handler {}, mod record {})", id, mod, handler, record);
            return;
        }
        if (world.isRemote) {
            // FML: a client-side openGui shows a client-only GUI immediately; container GUIs come from the server.
            if (FMLEnvironment.dist.isClient()) Client.showClientOnly(handler, player, id, world, x, y, z);
            return;
        }
        if (!(player.mcc$entity instanceof ServerPlayer sp)) return;
        Object element;
        try {
            element = handler.getServerGuiElement(id, player, world, x, y, z);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("Server GUI {} of {} failed to construct", id, record.legacyId, e);
            return;
        }
        if (element instanceof Container container) {
            sp.openMenu(new SimpleMenuProvider((windowId, inv, p) -> new LegacyMenu1122(menuType, windowId, inv, container, null), Component.empty()),
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

    /**
     * {@code player.openContainer = container}: how 1.12 mods opened a container with their own network code (Galacticraft's buggy and
     * parachest inventories send their own packet that makes the client build the GUI, then assign the server container and add the player
     * as its listener). The container is hosted in a menu with the window id the mod counted ({@code getNextWindowId}), and the client is
     * told the id so that the screen it built becomes the player's open menu; without a menu the server would not see the clicks.
     */
    public static void openManual(ServerPlayer sp, int counted, Container container) {
        if (container == null || menuType == null || sp instanceof net.neoforged.neoforge.common.util.FakePlayer) return;
        if (container.mcc$menu instanceof LegacyMenu1122 m && sp.containerMenu == m) return;
        int id = container.windowId != 0 ? container.windowId : counted > 0 && counted <= 100 ? counted : sp.containerMenu.containerId % 100 + 1;
        if (sp.containerMenu != sp.inventoryMenu) sp.closeContainer();
        LegacyMenu1122 menu = new LegacyMenu1122(menuType, id, sp.getInventory(), container, null);
        menu.mcc$listenerManaged(); // initMenu broadcasts changes, which would add the player before the mod does ("Listener already listening")
        ByteBuf buf = org.kylepitcock.legacyconnector.core.network.LegacyBuffers.buffer().writeInt(id);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        PacketDistributor.sendToPlayer(sp, new LegacyPayload(FAMILY, "manual", 0, data));
        sp.containerMenu = menu;
        sp.initMenu(menu);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open(sp, menu));
    }

    /** Client side of {@link MenuType} creation: rebuild the legacy GUI from the menu extra data. */
    public static LegacyMenu1122 clientMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf buf) {
        String modId = buf.readUtf();
        int id = buf.readVarInt();
        int x = buf.readInt(), y = buf.readInt(), z = buf.readInt();
        return Client.createMenu(windowId, inv, modId, id, x, y, z);
    }

    static IGuiHandler handlerFor(String legacyId) {
        LegacyModRecord r = LegacyMods.byLegacyId(legacyId);
        return r == null ? null : handler(r.instance);
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        if (!FMLEnvironment.dist.isClient()) return;
        ByteBuf buf = org.kylepitcock.legacyconnector.core.network.LegacyBuffers.wrap(payload.data());
        context.enqueueWork(() -> {
            if ("manual".equals(payload.channel())) {
                Client.manualOpen(buf.readInt());
            } else if ("open".equals(payload.channel())) {
                byte[] modId = new byte[buf.readInt()];
                buf.readBytes(modId);
                IGuiHandler handler = handlerFor(new String(modId, StandardCharsets.UTF_8));
                if (handler != null) Client.openFromServer(handler, payload.discriminator(), buf.readInt(), buf.readInt(), buf.readInt());
            }
        });
    }

    // ------------------------------------------------------------------ EntityPlayerMP as an IContainerListener

    /** {@code sendAllContents}: the modern menu resends everything. */
    public static void sendAllContents(ServerPlayer player, Container container) {
        if (container != null && container.mcc$menu instanceof LegacyMenu1122 menu && player.containerMenu == menu) menu.sendAllDataToRemote();
    }

    public static void sendSlotContents(ServerPlayer player, Container container, int slot, ItemStack stack) {
        if (player == null || player.connection == null || container == null) return;
        int stateId = container.mcc$menu instanceof LegacyMenu1122 menu ? menu.incrementStateId() : 0;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(container.windowId, stateId, slot, Stacks1122.toModern(stack)));
    }

    /** A progress bar value: a vanilla container data packet (the client menu passes it to {@code updateProgressBar}). */
    public static void sendWindowProperty(ServerPlayer player, Container container, int id, int value) {
        if (player == null || player.connection == null || container == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket(container.windowId, id, value));
    }

    /** Client-only half, in its own class so dedicated servers never load client classes. */
    static final class Client {
        static LegacyMenu1122 createMenu(int windowId, Inventory inv, String modId, int id, int x, int y, int z) {
            return org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.createMenu(menuType, handlerFor(modId), windowId, inv, id, x, y, z);
        }

        static void showClientOnly(IGuiHandler handler, EntityPlayer player, int id, World world, int x, int y, int z) {
            org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.showClientOnly(handler, player, id, world, x, y, z);
        }

        static void manualOpen(int windowId) {
            org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.manualOpen(windowId);
        }

        static void openFromServer(IGuiHandler handler, int id, int x, int y, int z) {
            org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.openFromServer(handler, id, x, y, z);
        }
    }
}
