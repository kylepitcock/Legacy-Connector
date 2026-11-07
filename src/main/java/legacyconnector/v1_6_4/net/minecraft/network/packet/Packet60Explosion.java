package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.play.server.S27PacketExplosion;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's explosion packet: centre, size, the destroyed block positions (stored as bytes relative to the centre) and the knockback of the receiving player. */
public class Packet60Explosion extends Packet {
    public double explosionX;
    public double explosionY;
    public double explosionZ;
    public float explosionSize;
    public List chunkPositionRecords = new ArrayList();
    private float playerVelocityX;
    private float playerVelocityY;
    private float playerVelocityZ;

    public Packet60Explosion() {}

    public Packet60Explosion(double x, double y, double z, float size, List blocks, Vec3 knockback) {
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;
        this.explosionSize = size;
        this.chunkPositionRecords = blocks == null ? new ArrayList() : new ArrayList(blocks);
        if (knockback != null) {
            this.playerVelocityX = (float) knockback.xCoord;
            this.playerVelocityY = (float) knockback.yCoord;
            this.playerVelocityZ = (float) knockback.zCoord;
        }
    }

    public float getPlayerVelocityX() {
        return playerVelocityX;
    }

    public float getPlayerVelocityY() {
        return playerVelocityY;
    }

    public float getPlayerVelocityZ() {
        return playerVelocityZ;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        explosionX = in.readDouble();
        explosionY = in.readDouble();
        explosionZ = in.readDouble();
        explosionSize = in.readFloat();
        int n = in.readInt();
        chunkPositionRecords = new ArrayList(n);
        int bx = (int) explosionX, by = (int) explosionY, bz = (int) explosionZ;
        for (int i = 0; i < n; i++) {
            chunkPositionRecords.add(new ChunkPosition(in.readByte() + bx, in.readByte() + by, in.readByte() + bz));
        }
        playerVelocityX = in.readFloat();
        playerVelocityY = in.readFloat();
        playerVelocityZ = in.readFloat();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeDouble(explosionX);
        out.writeDouble(explosionY);
        out.writeDouble(explosionZ);
        out.writeFloat(explosionSize);
        out.writeInt(chunkPositionRecords.size());
        int bx = (int) explosionX, by = (int) explosionY, bz = (int) explosionZ;
        for (Object o : chunkPositionRecords) {
            ChunkPosition p = (ChunkPosition) o;
            out.writeByte(p.chunkPosX - bx);
            out.writeByte(p.chunkPosY - by);
            out.writeByte(p.chunkPosZ - bz);
        }
        out.writeFloat(playerVelocityX);
        out.writeFloat(playerVelocityY);
        out.writeFloat(playerVelocityZ);
    }

    @Override
    public int getPacketSize() {
        return 32 + chunkPositionRecords.size() * 3 + 12;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        new S27PacketExplosion(explosionX, explosionY, explosionZ, explosionSize, chunkPositionRecords,
                Vec3.createVectorHelper(playerVelocityX, playerVelocityY, playerVelocityZ)).mcc$send(player);
    }
}
