package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Tells the client what block stands at a position. */
public class S23PacketBlockChange extends Packet {
    private net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.ZERO;
    private net.minecraft.world.level.block.state.BlockState state = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    private Block legacyBlock;
    private int legacyMeta;

    public S23PacketBlockChange() {
    }

    public S23PacketBlockChange(int x, int y, int z, World world) {
        this.pos = new net.minecraft.core.BlockPos(x, y, z);
        var level = world.mcc$liveLevel();
        this.state = level == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : level.getBlockState(pos);
        this.legacyBlock = world.getBlock(x, y, z);
        this.legacyMeta = world.getBlockMetadata(x, y, z);
    }

    /** The x coordinate of the changed block. */
    public int func_148879_d() {
        return pos.getX();
    }

    /** The y coordinate of the changed block. */
    public int func_148878_e() {
        return pos.getY();
    }

    /** The z coordinate of the changed block. */
    public int func_148877_f() {
        return pos.getZ();
    }

    /** The block the position now holds. */
    public Block func_148880_c() {
        return legacyBlock;
    }

    /** The metadata of that block. */
    public int func_148881_g() {
        return legacyMeta;
    }

    /** 1.7.10 wire format: x (int), y (unsigned byte), z (int), block id (varint), metadata (unsigned byte). */
    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        int x = data.readInt();
        int y = data.readUnsignedByte();
        int z = data.readInt();
        this.pos = new net.minecraft.core.BlockPos(x, y, z);
        this.legacyBlock = Block.getBlockById(data.readVarIntFromBuffer());
        this.legacyMeta = data.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(pos.getX());
        data.writeByte(pos.getY());
        data.writeInt(pos.getZ());
        data.writeVarIntToBuffer(Block.getIdFromBlock(legacyBlock));
        data.writeByte(legacyMeta);
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(pos, state));
    }
}
