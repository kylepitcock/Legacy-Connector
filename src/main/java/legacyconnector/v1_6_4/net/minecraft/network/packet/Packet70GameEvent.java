package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's game state packet: 0 bed unusable, 1 rain starts, 2 rain stops, 3 game mode, 4 credits. */
public class Packet70GameEvent extends Packet {
    public static final String[] notValidBedMessage = {"tile.bed.notValid", null, null};
    public int eventType;
    public int gameMode;

    public Packet70GameEvent() {}

    public Packet70GameEvent(int eventType, int gameMode) {
        this.eventType = eventType;
        this.gameMode = gameMode;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        eventType = in.readByte();
        gameMode = in.readByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeByte(eventType);
        out.writeByte(gameMode);
    }

    @Override
    public int getPacketSize() {
        return 2;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        new S2BPacketChangeGameState(eventType, gameMode).mcc$send(player);
    }
}
