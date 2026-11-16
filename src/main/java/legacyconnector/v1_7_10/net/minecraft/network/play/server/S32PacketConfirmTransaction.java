package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * Answer to a window click transaction. 1.21.1 has no transactions (the container state id replaces them), so nothing goes over the wire: the
 * accept/reject round trip of 1.7.10 only mattered to the legacy client, which the modern one never needs to ask.
 */
public class S32PacketConfirmTransaction extends Packet {
    private int windowId;
    private short actionNumber;
    private boolean accepted;

    public S32PacketConfirmTransaction() {
    }

    public S32PacketConfirmTransaction(int windowId, short actionNumber, boolean accepted) {
        this.windowId = windowId;
        this.actionNumber = actionNumber;
        this.accepted = accepted;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        windowId = data.readUnsignedByte();
        actionNumber = data.readShort();
        accepted = data.readBoolean();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(windowId);
        data.writeShort(actionNumber);
        data.writeBoolean(accepted);
    }

    public int func_148889_c() {
        return windowId;
    }

    public short func_148890_d() {
        return actionNumber;
    }

    public boolean func_148888_e() {
        return accepted;
    }
}
