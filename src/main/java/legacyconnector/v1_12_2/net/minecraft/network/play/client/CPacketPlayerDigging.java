package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** Breaking a block, dropping items, releasing a used item or swapping hands (sent as the game's player action packet). */
public class CPacketPlayerDigging implements Packet<INetHandlerPlayServer> {
    private BlockPos position;
    private EnumFacing facing;
    private Action action;

    public CPacketPlayerDigging() {
    }

    public CPacketPlayerDigging(Action action, BlockPos pos, EnumFacing facing) {
        this.action = action;
        this.position = pos;
        this.facing = facing;
    }

    public BlockPos getPosition() {
        return position;
    }

    public EnumFacing getFacing() {
        return facing;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        action = buf.readEnumValue(Action.class);
        position = buf.readBlockPos();
        facing = EnumFacing.byIndex(buf.readUnsignedByte());
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeEnumValue(action);
        buf.writeBlockPos(position);
        buf.writeByte(facing.getIndex());
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        if (handler instanceof legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer h) h.processPlayerDigging(this);
    }

    @Override
    public net.minecraft.network.protocol.game.ServerboundPlayerActionPacket mcc$modern() {
        return new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.values()[action.ordinal()], position.mcc$modern(),
                facing == null ? net.minecraft.core.Direction.DOWN : facing.mcc$modern());
    }

    public enum Action {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM,
        SWAP_HELD_ITEMS
    }
}
