package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** A world sound/particle effect by legacy effect id (playAuxSFX), sent as the matching modern level event. */
public class S28PacketEffect extends Packet {
    public int soundType;
    public int soundData;
    public int xPos;
    public int yPos;
    public int zPos;
    public boolean serverWide;

    public S28PacketEffect() {
    }

    public S28PacketEffect(int type, int x, int y, int z, int data, boolean serverWide) {
        this.soundType = type;
        this.xPos = x;
        this.yPos = y;
        this.zPos = z;
        this.soundData = data;
        this.serverWide = serverWide;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        soundType = data.readInt();
        xPos = data.readInt();
        yPos = data.readByte() & 255;
        zPos = data.readInt();
        soundData = data.readInt();
        serverWide = data.readBoolean();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(soundType);
        data.writeInt(xPos);
        data.writeByte(yPos & 255);
        data.writeInt(zPos);
        data.writeInt(soundData);
        data.writeBoolean(serverWide);
    }

    public boolean isSoundServerwide() {
        return serverWide;
    }

    public int getSoundType() {
        return soundType;
    }

    public int getSoundData() {
        return soundData;
    }

    public int getPosX() {
        return xPos;
    }

    public int getPosY() {
        return yPos;
    }

    public int getPosZ() {
        return zPos;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        int[] modern = org.kylepitcock.legacyconnector.v1_7_10.bridge.LevelEvents1710.toModern(soundType, soundData);
        if (modern == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelEventPacket(modern[0],
                new net.minecraft.core.BlockPos(xPos, yPos, zPos), modern[1], serverWide));
    }
}
