package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.List;

import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandlerListener;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The modern menu of a b1.7.3 container ({@code ModLoader.OpenGUI} with a {@code GuiContainer}). On the server it holds the legacy
 * container: slots of the player's inventory become slots of the modern inventory, the others {@link LegacySlotB173}s over the legacy slots;
 * shift-click, validity and closing call the container; the progress values it reports to its crafters ({@code updateCraftingInventoryInfo})
 * become menu data. The client builds the same slot layout from the menu's extra data and draws it with the mod's GUI: in single player the
 * one the server built (it reads the tile entity directly, as b1.7.3 did). Remote clients use their own block callback's GUI or reconstruct
 * its public machine constructor from the canonical client tile. Both remote paths receive original container slot and progress updates.
 */
public class LegacyB173Menu extends AbstractContainerMenu {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    static final int DATA = 32;

    /** Server only. */
    private final ScreenHandler legacy;
    private final Player player;
    private final int handoff;
    private final SimpleContainerData data = new SimpleContainerData(DATA);
    private ScreenHandlerListener listener;
    private boolean failed;
    /** Client only: the legacy GUI drawn for this menu, whether menu data goes to its container, and its size. */
    private final Object gui;
    private final boolean forwardData;
    private final int backgroundWidth;
    private final int backgroundHeight;

    LegacyB173Menu(int id, ServerPlayer player, ScreenHandler legacy, int handoff) {
        super(MenusB173.type(), id);
        this.legacy = legacy;
        this.player = player;
        this.handoff = handoff;
        this.gui = null;
        this.forwardData = false;
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        legacy.syncId = id;
        for (Slot s : legacy.slots) {
            addSlot(isPlayerSlot(s) ? new net.minecraft.world.inventory.Slot(player.getInventory(), s.index, s.x, s.y) : new LegacySlotB173(s));
        }
        addDataSlots(data);
        listener = new Listener();
        call("addListener", () -> legacy.addListener(listener));
    }

    private LegacyB173Menu(int id, Inventory inventory, int[][] layout, Object gui, boolean forwardData, int width, int height) {
        super(MenusB173.type(), id);
        this.legacy = null;
        this.player = inventory.player;
        this.handoff = 0;
        this.gui = gui;
        this.forwardData = forwardData;
        this.backgroundWidth = width;
        this.backgroundHeight = height;
        SimpleContainer others = new SimpleContainer(Math.max(1, layout.length));
        for (int i = 0; i < layout.length; i++) {
            int[] s = layout[i];
            addSlot(s[0] == 0 ? new net.minecraft.world.inventory.Slot(inventory, s[1], s[2], s[3]) : new net.minecraft.world.inventory.Slot(others, i, s[2], s[3]));
        }
        addDataSlots(data);
    }

    /** The client menu, from the extra data {@link #write} put in the open-screen packet. */
    public static LegacyB173Menu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        int handoff = buf.readVarInt();
        String guiClass = buf.readUtf();
        int width = buf.readVarInt();
        int height = buf.readVarInt();
        int[][] layout = new int[buf.readVarInt()][];
        for (int i = 0; i < layout.length; i++) layout[i] = new int[] {buf.readByte(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()};
        var context=RemoteMenuContextB173.read(buf);
        var selected=RemoteMenuContextB173.select(handoff!=0?MenusB173.handedOff(handoff):null,
                ()->guiClass.isEmpty()?null:MenusB173.pending(guiClass),
                ()->guiClass.isEmpty()||context==null?null:RemoteMenuContextB173.reconstruct(guiClass,context,inventory,layout));
        Object gui=selected.gui();boolean forward=selected.forward();
        MenusB173.trace("client menu {}: {} slots, drawn by {}", id, layout.length,
                gui != null ? gui.getClass().getName() + (forward ? " (the client's own)" : " (handed over)") : "a plain panel (" + guiClass + " not found)");
        return new LegacyB173Menu(id, inventory, layout, gui, forward, width, height);
    }

    /** Open-screen data: GUI identity, slots and finite registered-tile context for remote reconstruction. */
    static void write(RegistryFriendlyByteBuf buf, ScreenHandler container, Object gui, int handoff) {
        buf.writeVarInt(handoff);
        buf.writeUtf(gui == null ? "" : gui.getClass().getName());
        buf.writeVarInt(gui instanceof HandledScreen hs ? hs.mcc$backgroundWidth() : 176);
        buf.writeVarInt(gui instanceof HandledScreen hs ? hs.mcc$backgroundHeight() : 166);
        List<Slot> slots = container.slots;
        buf.writeVarInt(slots.size());
        for (Slot s : slots) {
            buf.writeByte(isPlayerSlot(s) ? 0 : 1);
            buf.writeVarInt(Math.max(0, s.index));
            buf.writeVarInt(s.x);
            buf.writeVarInt(s.y);
        }
        buf.writeNbt(handoff==0&&gui instanceof HandledScreen?RemoteMenuContextB173.capture(container):new net.minecraft.nbt.CompoundTag());
    }

    static boolean isPlayerSlot(Slot s) {
        return s.inventory instanceof PlayerInventory && s.index >= 0 && s.index < 40;
    }

    public Object gui() {
        return gui;
    }

    public int backgroundWidth() {
        return backgroundWidth;
    }

    public int backgroundHeight() {
        return backgroundHeight;
    }

    public ScreenHandler legacy() {
        return legacy;
    }

    int handoff() {
        return handoff;
    }

    /** True once a legacy call of the container threw (it then stops updating). */
    public boolean failed() {
        return failed;
    }

    private void use() {
        Minecraft.mcc$use(World.mcc$wrap(player.level()));
    }

    private void call(String what, Runnable r) {
        if (failed) return;
        try {
            use();
            r.run();
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            LOG.error("b1.7.3 container {} failed in {}; it stops updating", legacy.getClass().getName(), what, e);
        }
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (legacy != null) use();
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (legacy == null || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = slots.get(index);
        ItemStack before = slot.getItem().copy();
        if (before.isEmpty()) return ItemStack.EMPTY;
        flushSlots();
        PlayerViewB173 view = PlayerViewB173.of(player);
        try {
            use();
            legacy.quickMove(index);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("b1.7.3 container {} failed to shift-click slot {}: {}", legacy.getClass().getName(), index, e.toString());
            return ItemStack.EMPTY;
        } finally {
            view.writeBack();
        }
        // vanilla repeats while this returns the slot's item; the legacy default answered the unchanged stack, which never ends
        return ItemStack.matches(before, slot.getItem()) ? ItemStack.EMPTY : before;
    }

    private void flushSlots() {
        for (net.minecraft.world.inventory.Slot s : slots) if (s instanceof LegacySlotB173 ls) ls.flush();
    }

    @Override
    public boolean stillValid(Player player) {
        if (legacy == null) return true;
        try {
            use();
            return legacy.canUse(PlayerViewB173.of(player));
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    @Override
    public void broadcastChanges() {
        if (legacy != null) {
            flushSlots();
            call("updateCraftingResults", legacy::sendContentUpdates);
        }
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (legacy == null) return;
        PlayerViewB173 view = PlayerViewB173.of(player);
        try {
            use();
            legacy.onClosed(view);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("b1.7.3 container {} failed while closing: {}", legacy.getClass().getName(), e.toString());
        } finally {
            view.writeBack();
            if (listener != null) legacy.mcc$removeListener(listener);
            MenusB173.release(handoff);
        }
    }

    /** A progress value from the server: also the client container's {@code updateProgressBar} when it is the client's own. */
    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        if (forwardData && gui instanceof HandledScreen hs && hs.container != null) {
            try {
                hs.container.setProperty(id, value);
            } catch (RuntimeException | LinkageError e) {
                // the display only
            }
        }
    }

    /** The client's slots come from the server's layout; extra or missing entries are ignored instead of disconnecting. */
    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        for (int i = 0; i < items.size() && i < slots.size(); i++) {getSlot(i).set(items.get(i));forwardStack(i,items.get(i));}
        setCarried(carried);
    }

    @Override
    public void setItem(int slot, int stateId, ItemStack stack) {
        if (slot >= 0 && slot < slots.size()) {super.setItem(slot, stateId, stack);forwardStack(slot,stack);}
    }

    private void forwardStack(int slot,ItemStack stack){
        if(!failed&&forwardData&&gui instanceof HandledScreen handled)try{RemoteMenuContextB173.forward(handled,slot,stack);}
        catch(RuntimeException|LinkageError error){failed=true;LOG.warn("Original remote menu {} failed to receive slot {}",gui.getClass().getName(),slot,error);}
    }

    /** {@code ICrafting} of the modern menu: progress values become menu data. */
    private final class Listener implements ScreenHandlerListener {
        @Override
        public void onContentsUpdate(ScreenHandler handler, List<?> stacks) {}

        @Override
        public void onSlotUpdate(ScreenHandler handler, int slot, legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack) {}

        @Override
        public void onPropertyUpdate(ScreenHandler handler, int id, int value) {
            if (id >= 0 && id < DATA) data.set(id, value);
        }
    }
}
