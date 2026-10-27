package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A tile entity's data for the client: delivered to its {@code onDataPacket} (see {@code Legacy1122BlockEntity}). */
public class SPacketUpdateTileEntity implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos blockPos;
    private int tileEntityType;
    private legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound nbt;

    public SPacketUpdateTileEntity() {
    }

    public SPacketUpdateTileEntity(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos blockPosIn, int tileEntityTypeIn, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound compoundIn) {
        this.blockPos = blockPosIn;
        this.tileEntityType = tileEntityTypeIn;
        this.nbt = compoundIn;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getPos() {
        return blockPos;
    }

    public int getTileEntityType() {
        return tileEntityType;
    }

    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound getNbtCompound() {
        return nbt;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        blockPos = buf.readBlockPos();
        tileEntityType = buf.readUnsignedByte();
        nbt = buf.readCompoundTag();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeBlockPos(blockPos);
        buf.writeByte((byte) tileEntityType);
        buf.writeCompoundTag(nbt);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        // sent through the hosting block entity's own update packet, which knows the level
        return null;
    }
}
