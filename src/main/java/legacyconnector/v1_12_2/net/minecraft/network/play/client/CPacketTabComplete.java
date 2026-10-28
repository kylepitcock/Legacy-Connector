package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** A chat line to complete (commands are completed by the game's command suggestion packet; plain chat has none). */
public class CPacketTabComplete implements Packet<INetHandlerPlayServer> {
    private String message;
    private boolean hasTargetBlock;
    private BlockPos targetBlock;

    public CPacketTabComplete() {
    }

    public CPacketTabComplete(String message, BlockPos targetBlock, boolean hasTargetBlock) {
        this.message = message;
        this.targetBlock = targetBlock;
        this.hasTargetBlock = hasTargetBlock;
    }

    public String getMessage() {
        return message;
    }

    public BlockPos getTargetBlock() {
        return targetBlock;
    }

    public boolean hasTargetBlock() {
        return hasTargetBlock;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        message = buf.readString(32767);
        hasTargetBlock = buf.readBoolean();
        if (buf.readBoolean()) targetBlock = buf.readBlockPos();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(message.length() > 32767 ? message.substring(0, 32767) : message);
        buf.writeBoolean(hasTargetBlock);
        buf.writeBoolean(targetBlock != null);
        if (targetBlock != null) buf.writeBlockPos(targetBlock);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return message != null && message.startsWith("/") ? new net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket(0, message) : null;
    }
}
