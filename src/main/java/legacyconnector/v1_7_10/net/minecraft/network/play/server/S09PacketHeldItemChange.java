package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The server selects a hotbar slot (0-8) for the player. */
public class S09PacketHeldItemChange extends Packet {
    private int slot;

    public S09PacketHeldItemChange() {
    }

    public S09PacketHeldItemChange(int slot) {
        this.slot = slot;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        slot = data.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(slot);
    }

    public int func_149385_c() {
        return slot;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        if (slot >= 0 && slot < 9) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(slot));
    }
}
