package legacyconnector.v1_6_4.net.minecraft.network.packet;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's chunk data packet (mods resend a chunk after changing it behind the game's back: {@code new Packet51MapChunk(chunk, true, 65535)}).
 * Delivered as the modern chunk-with-light packet of the same chunk; the legacy compressed wire format is not produced.
 */
public class Packet51MapChunk extends Packet {
    public int xCh;
    public int zCh;
    public int yChMin;
    public int yChMax;
    public boolean includeInitialize;
    private Chunk chunk;

    public Packet51MapChunk() {
        this.isChunkDataPacket = true;
    }

    public Packet51MapChunk(Chunk chunk, boolean includeInitialize, int sectionMask) {
        this();
        this.chunk = chunk;
        this.xCh = chunk.xPosition;
        this.zCh = chunk.zPosition;
        this.includeInitialize = includeInitialize;
        this.yChMin = sectionMask;
        this.yChMax = sectionMask;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        if (chunk == null || !(chunk.mcc$modern() instanceof net.minecraft.world.level.chunk.LevelChunk live)) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(live,
                live.getLevel().getLightEngine(), null, null));
    }
}
