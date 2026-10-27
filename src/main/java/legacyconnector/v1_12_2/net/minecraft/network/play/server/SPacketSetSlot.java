package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** One slot of an open container (window -1 is the cursor, -2 the player's inventory). */
public class SPacketSetSlot implements Packet<INetHandlerPlayClient> {
    private int windowId;
    private int slot;
    private legacyconnector.v1_12_2.net.minecraft.item.ItemStack item;

    public SPacketSetSlot() {
    }

    public SPacketSetSlot(int windowIdIn, int slotIn, legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemIn) {
        this.windowId = windowIdIn;
        this.slot = slotIn;
        this.item = itemIn == null ? legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY : itemIn.copy();
    }

    public int getWindowId() {
        return windowId;
    }

    public int getSlot() {
        return slot;
    }

    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getStack() {
        return item;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        windowId = buf.readByte();
        slot = buf.readShort();
        item = buf.readItemStack();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeByte(windowId);
        buf.writeShort(slot);
        buf.writeItemStack(item);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(windowId, 0, slot,
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(item));
    }
}
