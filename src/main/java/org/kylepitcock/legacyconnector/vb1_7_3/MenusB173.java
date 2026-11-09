package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opening b1.7.3 GUIs. {@code ModLoader.OpenGUI(player, gui)} runs where the mod's block callback runs, on the server's and on the client's
 * level:
 * <ul>
 * <li>server, a {@code GuiContainer}: a {@link LegacyB173Menu} over its container. In single player the GUI object itself goes to the
 * client screen (handed over by id: same JVM), as b1.7.3's single player drew the GUI over the very tile entity that ticks;</li>
 * <li>server, another screen: original callbacks remain on the server; drawing and input cross the generic screen-session connection;</li>
 * <li>client: ignored in single player (the server's call shows it); on a server a {@code GuiContainer} waits for its menu (matched by
 * class) and another screen opens right away.</li>
 * </ul>
 * {@code displayGUIChest} opens a modern chest menu over the legacy inventory. Client parts live behind {@link Client}, installed by the
 * client, so a dedicated server never loads client classes.
 */
public final class MenusB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final Map<Integer, Object> HANDOFF = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT = new AtomicInteger();
    private static volatile MenuType<LegacyB173Menu> type;
    private static volatile Client client;
    private static final class Actor {
        final ServerPlayer player;Screen requested;boolean pending;
        Actor(ServerPlayer player){this.player=player;}
    }
    private static final ThreadLocal<Actor> ACTOR=new ThreadLocal<>();
    /** Defer nested screen construction until the initiating callback has committed its player inventory. */
    public static void withActor(ServerPlayer player,Runnable callback){
        Actor previous=ACTOR.get();if(previous!=null&&previous.player==player){callback.run();return;}
        Actor actor=new Actor(player);ACTOR.set(actor);boolean completed=false;
        try(var state=HookActorStateB173.forPlayer(player)){callback.run();completed=true;}
        finally{
            if(previous==null)ACTOR.remove();else ACTOR.set(previous);
            if(completed&&actor.pending)dispatchScreen(player,actor.requested);
        }
    }
    public static ServerPlayer currentActor(){Actor actor=ACTOR.get();return actor==null?null:actor.player;}
    public static Player localPlayer(){return client==null?null:client.localPlayer();}
    static boolean hasPendingScreen(ServerPlayer player){Actor actor=ACTOR.get();return actor!=null&&actor.player==player&&actor.pending;}
    private static void requestScreen(ServerPlayer player,Screen screen){Actor actor=ACTOR.get();if(actor!=null&&actor.player==player){actor.requested=screen;actor.pending=true;}else dispatchScreen(player,screen);}
    private static void dispatchScreen(ServerPlayer player,Screen screen){
        if(screen instanceof HandledScreen handled&&handled.container!=null){GuiSessionsB173.open(player,null);openMenu(player,handled.container,screen);}
        else {GuiSessionsB173.open(player,screen);if(screen==null&&player.containerMenu!=player.inventoryMenu)player.closeContainer();}
    }

    /** The client's part. */
    public interface Client {
        /** A legacy GUI opened by the client's own callback. */
        void openedOnClient(Screen gui);

        /** A screen without container the single player server opened for the local player. */
        void showFromServer(Screen gui);

        /** The {@code GuiContainer} the client's callback built for the menu now opening, if its class matches. */
        Screen takePending(String guiClass);

        /** {@code Minecraft.displayGuiScreen}. */
        void setScreen(Screen screen);

        String clipboard();
        void remoteFrame(net.minecraft.nbt.CompoundTag frame);
        default Player localPlayer(){return null;}
        default boolean hasAchievement(net.minecraft.resources.ResourceLocation id){return false;}
    }

    private MenusB173() {}

    public static void setMenuType(MenuType<LegacyB173Menu> t) {
        type = t;
        GuiSessionsB173.install();
    }

    public static MenuType<LegacyB173Menu> type() {
        return type;
    }

    public static void installClient(Client c) {
        client = c;
    }

    private static Player modern(PlayerEntity player) {
        return player != null && player.mcc$modern instanceof Player p ? p : null;
    }

    /** The single player host: its client runs in this JVM and can draw the server's GUI objects. */
    private static boolean localHost(ServerPlayer sp) {
        return client != null && !sp.server.isDedicatedServer() && sp.server.isSingleplayerOwner(sp.getGameProfile());
    }

    private static final AtomicInteger TRACED = new AtomicInteger();

    /** The first GUI events of a session go to the log (where an open stops when a GUI does not show). */
    public static void trace(String format, Object... args) {
        if (TRACED.incrementAndGet() <= 40) LOG.info("b1.7.3 GUI trace: " + format, args);
    }

    /** {@code ModLoader.OpenGUI(player, gui)}. */
    public static void open(PlayerEntity player, Screen gui) {
        if (gui == null) return;
        Player p = modern(player);
        trace("OpenGUI({}) by {} on the {} side", gui.getClass().getName(), p == null ? "an unknown player" : p.getGameProfile().getName(),
                p == null ? "unknown" : p.level().isClientSide ? "client" : "server");
        if (p instanceof ServerPlayer sp) {requestScreen(sp,gui);return;}
        if (p != null && !p.level().isClientSide) return;
        Client c = client;
        if (c != null) c.openedOnClient(gui);
    }

    /** ModLoaderMp's {@code OpenGUI(player, id, inventory, container)}: a menu over the container. */
    public static void openContainer(PlayerEntity player, ScreenHandler container) {
        if (container != null && modern(player) instanceof ServerPlayer sp) openMenu(sp, container, null);
    }

    private static void openMenu(ServerPlayer sp, ScreenHandler container, Screen gui) {
        if (type == null) {
            LOG.error("b1.7.3 GUI {} not opened: the menu type was never registered", gui == null ? container.getClass().getName() : gui.getClass().getName());
            return;
        }
        int handoff = gui != null && localHost(sp) ? NEXT.incrementAndGet() : 0;
        if (handoff != 0) HANDOFF.put(handoff, gui);
        try {
            var opened = sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new LegacyB173Menu(id, sp, container, handoff), Component.empty()),
                    buf -> LegacyB173Menu.write(buf, container, gui, handoff));
            trace("menu {} for {} ({} slots), GUI handed over: {}", opened.isPresent() ? opened.getAsInt() : "not opened",
                    container.getClass().getName(), container.slots.size(), handoff != 0);
            if (opened.isEmpty()) release(handoff);
        } catch (RuntimeException | LinkageError e) {
            release(handoff);
            LOG.error("b1.7.3 GUI {} could not be opened", gui == null ? container.getClass().getName() : gui.getClass().getName(), e);
        }
    }

    static Object handedOff(int id) {
        return HANDOFF.get(id);
    }

    static void release(int id) {
        if (id != 0) HANDOFF.remove(id);
    }

    static Object pending(String guiClass) {
        Client c = client;
        return c == null ? null : c.takePending(guiClass);
    }

    /** {@code displayGUIChest}: a vanilla chest menu over the legacy inventory (sizes of whole rows, up to six). */
    public static void openChest(PlayerEntity player, Inventory inventory) {
        if (inventory == null || !(modern(player) instanceof ServerPlayer sp)) return;
        int size = inventory.size();
        int rows = size / 9;
        if (size % 9 != 0 || rows < 1 || rows > 6) {
            LOG.warn("b1.7.3 chest screen for {} ({} slots) is not a whole number of rows; not opened", inventory.getClass().getName(), size);
            return;
        }
        MenuType<ChestMenu> chest = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        LegacyInventoryB173 container = LegacyInventoryB173.of(inventory);
        String name;
        try {
            name = inventory.getName();
        } catch (RuntimeException | LinkageError e) {
            name = "Chest";
        }
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ChestMenu(chest, id, inv, container, rows),
                Component.literal(name == null ? "Chest" : ContentB173.translate(name))));
    }

    /** {@code closeScreen}. */
    public static void close(PlayerEntity player) {
        Player p = modern(player);
        if (p instanceof ServerPlayer sp) {
            requestScreen(sp,null);
        } else if (p == null || p.level().isClientSide) {
            setScreen(null);
        }
    }

    /** {@code dropPlayerItem}: the modern player drops the stack (server side; the client's copy of a callback drops nothing). */
    public static void drop(PlayerEntity player, ItemStack stack, boolean scatter) {
        Player p = modern(player);
        if (p == null || p.level().isClientSide || stack == null || stack.count <= 0) return;
        net.minecraft.world.item.ItemStack modern = StacksB173.toModern(stack);
        if (!modern.isEmpty()) p.drop(modern, scatter, false);
    }

    /** {@code Minecraft.displayGuiScreen}. */
    public static void setScreen(Screen screen) {
        Actor actor=ACTOR.get();
        if(actor!=null){actor.requested=screen;actor.pending=true;return;}
        Client c = client;
        if (c != null) c.setScreen(screen);
    }

    public static void remoteFrame(net.minecraft.nbt.CompoundTag frame){Client c=client;if(c!=null)c.remoteFrame(frame);}
    public static String clipboard() {
        Client c = client;
        return c == null ? "" : c.clipboard();
    }
    public static boolean hasAchievement(net.minecraft.resources.ResourceLocation id){Client current=client;return current!=null&&current.hasAchievement(id);}
}
