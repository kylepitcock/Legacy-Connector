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
        // the packet alone does not know its level: the senders that do (a chunk's watchers, a player's connection) use mcc$modern(level)
        return null;
    }

    /**
     * The modern block entity packet of this one, for the block entity at its position in the level: its type and data reach the tile
     * entity's {@code onDataPacket} on the client (see Legacy1122BlockEntity). Mods send it themselves, through
     * {@code PlayerChunkMapEntry.sendPacket} (Techguns' machines, after every change of their operation) or a player's connection; dropped,
     * the client never heard of a machine's operation, its progress or its settings. Null when there is no hosted tile entity there.
     */
    public net.minecraft.network.protocol.Packet<?> mcc$modern(net.minecraft.world.level.Level level) {
        if (level == null || blockPos == null) return null;
        var be = level.getBlockEntity(new net.minecraft.core.BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        if (!(be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity)) return null;
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("mcc$type", tileEntityType);
        if (nbt != null) tag.put("mcc$packet", nbt.mcc$modern().copy());
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(be, (b, registries) -> tag);
    }
}
