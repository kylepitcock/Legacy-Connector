package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The server ends the connection with a reason. */
public class S40PacketDisconnect extends Packet {
    public legacyconnector.v1_7_10.net.minecraft.util.IChatComponent reason;

    public S40PacketDisconnect() {
    }

    public S40PacketDisconnect(legacyconnector.v1_7_10.net.minecraft.util.IChatComponent reason) {
        this.reason = reason;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        reason = legacyconnector.v1_7_10.net.minecraft.util.IChatComponent.Serializer.jsonToComponent(data.readStringFromBuffer(32767));
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(legacyconnector.v1_7_10.net.minecraft.util.IChatComponent.Serializer.componentToJson(reason));
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IChatComponent func_149165_c() {
        return reason;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.disconnect(reason == null ? net.minecraft.network.chat.Component.empty() : reason.mcc$toModern());
    }
}
