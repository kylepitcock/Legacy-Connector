package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_6_4.forward.Forward164;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.world.inventory.ClickType;

/**
 * 1.6.4's click-in-window packet, sent by the client: {@code holdingShift} is the click mode (0 pickup, 1 quick move, 2 swap, 3 clone, 4 throw,
 * 5 quick craft, 6 pickup all), {@code mouseClick} the button, {@code action} the transaction counter. Applied on the server by
 * {@code NetHandlerPlayServer.handleWindowClick}; sent as the modern container-click packet.
 */
public class Packet102WindowClick extends Packet {
    public int window_Id;
    public int inventorySlot;
    public int mouseClick;
    public short action;
    public ItemStack itemStack;
    public int holdingShift;

    public Packet102WindowClick() {}

    public Packet102WindowClick(int window, int slot, int button, int mode, ItemStack stack, short action) {
        this.window_Id = window;
        this.inventorySlot = slot;
        this.mouseClick = button;
        this.holdingShift = mode;
        this.itemStack = stack == null ? null : stack.copy();
        this.action = action;
    }

    /** The ordinal of the modern {@link ClickType} for a legacy mode: the modern enum lists the modes in the legacy order (an unknown mode is a pickup). */
    public static int clickOrdinal(int mode) {
        return mode >= 0 && mode <= 6 ? mode : 0;
    }

    /** The modern click type of a legacy mode. */
    public static ClickType clickType(int mode) {
        return ClickType.values()[clickOrdinal(mode)];
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        window_Id = in.readByte();
        inventorySlot = in.readShort();
        mouseClick = in.readByte();
        action = in.readShort();
        holdingShift = in.readByte();
        itemStack = Forward164.readItemStack(in);
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeByte(window_Id);
        out.writeShort(inventorySlot);
        out.writeByte(mouseClick);
        out.writeShort(action);
        out.writeByte(holdingShift);
        Forward164.writeItemStack(itemStack, out);
    }

    @Override
    public int getPacketSize() {
        return 11;
    }

    /** The server side: performs the click on the player's open container (a click on a window the player no longer has open is dropped). */
    public static void apply(Packet102WindowClick packet, net.minecraft.server.level.ServerPlayer player) {
        var menu = player.containerMenu;
        if (menu == null || menu.containerId != packet.window_Id) return;
        menu.clicked(packet.inventorySlot, packet.mouseClick, clickType(packet.holdingShift), player);
        menu.broadcastChanges();
    }

    @Override
    public void mcc$sendToServer() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        var menu = mc.player.containerMenu;
        net.minecraft.world.item.ItemStack carried = menu.getCarried().copy();
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(window_Id, menu.getStateId(),
                inventorySlot, mouseClick, clickType(holdingShift), carried, new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
    }
}
