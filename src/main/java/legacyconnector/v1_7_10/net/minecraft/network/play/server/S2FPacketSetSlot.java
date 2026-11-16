package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** One slot of a container window changed (window -1, slot -1: the stack the cursor carries). */
public class S2FPacketSetSlot extends Packet {
    private int windowId;
    private int slot;
    private ItemStack stack;

    public S2FPacketSetSlot() {
    }

    public S2FPacketSetSlot(int windowId, int slot, ItemStack stack) {
        this.windowId = windowId;
        this.slot = slot;
        this.stack = stack == null ? null : stack.copy();
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        windowId = data.readByte();
        slot = data.readShort();
        stack = data.readItemStackFromBuffer();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(windowId);
        data.writeShort(slot);
        data.writeItemStackToBuffer(stack);
    }

    public int func_149175_c() {
        return windowId;
    }

    public int func_149173_d() {
        return slot;
    }

    public ItemStack func_149174_e() {
        return stack;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(windowId, 0, slot,
                stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.mcc$modern()));
    }
}
