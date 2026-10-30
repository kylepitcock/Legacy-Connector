package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/** A chunk's blocks and light, sent as the modern chunk packet (mods send it to make clients reload a chunk they changed). */
public class SPacketChunkData implements Packet<INetHandlerPlayClient> {
    private int chunkX;
    private int chunkZ;
    private int availableSections;
    private boolean fullChunk;
    private Chunk chunk;

    public SPacketChunkData() {
    }

    /** {@code changedSectionFilter} 65535 sends the whole chunk (the modern packet always does). */
    public SPacketChunkData(Chunk chunkIn, int changedSectionFilter) {
        this.chunk = chunkIn;
        this.chunkX = chunkIn.x;
        this.chunkZ = chunkIn.z;
        this.fullChunk = changedSectionFilter == 65535;
        this.availableSections = changedSectionFilter;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getExtractedSize() {
        return availableSections;
    }

    public boolean isFullChunk() {
        return fullChunk;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        var access = chunk == null ? null : chunk.mcc$access();
        if (!(access instanceof net.minecraft.world.level.chunk.LevelChunk level)) return null;
        return new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(level, level.getLevel().getLightEngine(), null, null);
    }
}
