package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's set-slot packet: the stack of one slot of an open window (window 0 is the player's inventory, -1 the cursor). */
public class Packet103SetSlot extends Packet {
    public int windowId;
    public int itemSlot;
    public ItemStack myItemStack;

    public Packet103SetSlot() {}

    public Packet103SetSlot(int windowId, int slot, ItemStack stack) {
        this.windowId = windowId;
        this.itemSlot = slot;
        this.myItemStack = stack == null ? null : stack.copy();
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        windowId = in.readByte();
        itemSlot = in.readShort();
        myItemStack = legacyconnector.v1_6_4.forward.Forward164.readItemStack(in);
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeByte(windowId);
        out.writeShort(itemSlot);
        legacyconnector.v1_6_4.forward.Forward164.writeItemStack(myItemStack, out);
    }

    @Override
    public int getPacketSize() {
        return 8;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        net.minecraft.world.item.ItemStack stack = myItemStack == null ? net.minecraft.world.item.ItemStack.EMPTY : myItemStack.mcc$modern();
        int state = windowId == player.containerMenu.containerId ? player.containerMenu.getStateId() : 0;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(windowId, state, itemSlot, stack));
    }
}
