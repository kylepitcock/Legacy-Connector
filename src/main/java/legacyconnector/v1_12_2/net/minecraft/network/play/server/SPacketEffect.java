package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A world event (sounds and particles by id: 1000+ sounds, 2000+ particles) at a block. */
public class SPacketEffect implements Packet<INetHandlerPlayClient> {
    private int soundType;
    private legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos soundPos;
    private int soundData;
    private boolean serverWide;

    public SPacketEffect() {
    }

    public SPacketEffect(int soundTypeIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos soundPosIn, int soundDataIn, boolean serverWideIn) {
        this.soundType = soundTypeIn;
        this.soundPos = soundPosIn;
        this.soundData = soundDataIn;
        this.serverWide = serverWideIn;
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

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getSoundPos() {
        return soundPos;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        soundType = buf.readInt();
        soundPos = buf.readBlockPos();
        soundData = buf.readInt();
        serverWide = buf.readBoolean();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeInt(soundType);
        buf.writeBlockPos(soundPos);
        buf.writeInt(soundData);
        buf.writeBoolean(serverWide);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (soundPos == null) return null;
        int data = soundData;
        if (soundType == 2001) {
            // block break particles: the data is 1.12's state id, the modern client wants its own
            var legacy = legacyconnector.v1_12_2.net.minecraft.block.Block.getStateById(soundData);
            var modern = legacy == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(legacy);
            data = modern == null ? 1 : net.minecraft.world.level.block.Block.getId(modern);
        }
        return new net.minecraft.network.protocol.game.ClientboundLevelEventPacket(soundType, soundPos.mcc$modern(), data, serverWide);
    }
}
