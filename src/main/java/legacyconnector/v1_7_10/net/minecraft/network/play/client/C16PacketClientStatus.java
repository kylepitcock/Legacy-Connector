package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/** Client status: respawn, request the statistics, open the achievement screen. */
public class C16PacketClientStatus extends Packet {
    public enum EnumState {
        PERFORM_RESPAWN,
        REQUEST_STATS,
        OPEN_INVENTORY_ACHIEVEMENT
    }

    private EnumState status = EnumState.PERFORM_RESPAWN;

    public C16PacketClientStatus() {
    }

    public C16PacketClientStatus(EnumState status) {
        this.status = status;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        status = EnumState.values()[data.readByte() % EnumState.values().length];
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(status.ordinal());
    }

    public EnumState func_149435_c() {
        return status;
    }

    @Override
    public void mcc$sendToServer() {
        switch (status) {
            case PERFORM_RESPAWN -> org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(
                    new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            case REQUEST_STATS -> org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(
                    new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
            default -> {
            }
        }
    }
}
