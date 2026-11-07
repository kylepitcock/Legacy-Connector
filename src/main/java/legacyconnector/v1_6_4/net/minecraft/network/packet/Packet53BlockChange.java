package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's single block update. Built from a world it records the block id and metadata at the position (the legacy wire); delivered as the
 * modern block update of the position as it is in the player's level when sent.
 */
public class Packet53BlockChange extends Packet {
    public int xPosition;
    public int yPosition;
    public int zPosition;
    public int type;
    public int metadata;

    public Packet53BlockChange() {
        this.isChunkDataPacket = true;
    }

    public Packet53BlockChange(int x, int y, int z, World world) {
        this();
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
        var block = world.getBlock(x, y, z);
        this.type = legacyconnector.v1_6_4.net.minecraft.util.Ids.blockId(block);
        this.metadata = world.getBlockMetadata(x, y, z);
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        xPosition = in.readInt();
        yPosition = in.readUnsignedByte();
        zPosition = in.readInt();
        type = in.readShort();
        metadata = in.readUnsignedByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(xPosition);
        out.write(yPosition);
        out.writeInt(zPosition);
        out.writeShort(type);
        out.write(metadata);
    }

    @Override
    public int getPacketSize() {
        return 11;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        var pos = new net.minecraft.core.BlockPos(xPosition, yPosition, zPosition);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.serverLevel(), pos));
    }
}
