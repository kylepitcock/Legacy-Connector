package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_6_4.forward.Packets164;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's kick packet: the connection is closed and the client shows {@code reason}. Delivered as a disconnect of the player's connection. */
public class Packet255KickDisconnect extends Packet {
    public String reason;

    public Packet255KickDisconnect() {}

    public Packet255KickDisconnect(String reason) {
        this.reason = reason;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        reason = Packets164.readString(in, 256);
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        Packets164.writeString(reason == null ? "" : reason, out);
    }

    @Override
    public int getPacketSize() {
        return reason == null ? 2 : reason.length();
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        player.connection.disconnect(net.minecraft.network.chat.Component.literal(reason == null ? "" : reason));
    }
}
