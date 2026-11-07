package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_6_4.forward.Packets164;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.WorldSettings;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's respawn packet (dimension travel and the death screen's respawn). Delivered as the modern respawn packet built from the dimension the
 * player is in now, so the client reloads into it; the legacy dimension id, height and terrain type only travel on the legacy wire.
 */
public class Packet9Respawn extends Packet {
    public int respawnDimension;
    public int difficulty;
    public int worldHeight;
    public WorldSettings.GameType gameType;
    public WorldType terrainType;

    public Packet9Respawn() {}

    public Packet9Respawn(int dimension, byte difficulty, WorldType terrain, int height, WorldSettings.GameType gameType) {
        this.respawnDimension = dimension;
        this.difficulty = difficulty;
        this.terrainType = terrain;
        this.worldHeight = height;
        this.gameType = gameType;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        respawnDimension = in.readInt();
        difficulty = in.readByte();
        gameType = WorldSettings.GameType.getByID(in.readByte());
        worldHeight = in.readShort();
        terrainType = WorldType.parseWorldType(Packets164.readString(in, 16));
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(respawnDimension);
        out.writeByte(difficulty);
        out.writeByte(gameType == null ? 0 : gameType.getID());
        out.writeShort(worldHeight);
        Packets164.writeString(terrainType == null ? "default" : terrainType.getWorldTypeName(), out);
    }

    @Override
    public int getPacketSize() {
        return 8 + (terrainType == null ? 7 : terrainType.getWorldTypeName().length());
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundRespawnPacket(
                player.createCommonSpawnInfo(player.serverLevel()), net.minecraft.network.protocol.game.ClientboundRespawnPacket.KEEP_ALL_DATA));
    }
}
