package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The client clicked a slot of a container window. {@code mode}: 0 pickup, 1 quick move, 2 swap, 3 clone, 4 throw, 5 quick craft, 6 pickup all. */
public class C0EPacketClickWindow extends Packet {
    private int windowId;
    private int slotId;
    private int mouseButton;
    private short actionNumber;
    private ItemStack clickedItem;
    private int mode;

    public C0EPacketClickWindow() {
    }

    public C0EPacketClickWindow(int windowId, int slot, int button, int mode, ItemStack clicked, short actionNumber) {
        this.windowId = windowId;
        this.slotId = slot;
        this.mouseButton = button;
        this.mode = mode;
        this.clickedItem = clicked == null ? null : clicked.copy();
        this.actionNumber = actionNumber;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        windowId = data.readByte();
        slotId = data.readShort();
        mouseButton = data.readByte();
        actionNumber = data.readShort();
        mode = data.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(windowId);
        data.writeShort(slotId);
        data.writeByte(mouseButton);
        data.writeShort(actionNumber);
        data.writeByte(mode);
    }

    public int getWindowId() {
        return windowId;
    }

    public int getSlotId() {
        return slotId;
    }

    public int getUsedButton() {
        return mouseButton;
    }

    public short getActionNumber() {
        return actionNumber;
    }

    public ItemStack getClickedItem() {
        return clickedItem;
    }

    public int getMode() {
        return mode;
    }

    @Override
    public void mcc$sendToServer() {
        Client.click(windowId, slotId, mouseButton, mode);
    }

    /** The client-only part, in its own class so a dedicated server never loads the client player type while verifying this one. */
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static final class Client {
        static void click(int windowId, int slotId, int button, int mode) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.gameMode == null || mc.player == null) return;
            var types = net.minecraft.world.inventory.ClickType.values();
            mc.gameMode.handleInventoryMouseClick(windowId, slotId, button, mode >= 0 && mode < types.length ? types[mode] : types[0], mc.player);
        }
    }
}
