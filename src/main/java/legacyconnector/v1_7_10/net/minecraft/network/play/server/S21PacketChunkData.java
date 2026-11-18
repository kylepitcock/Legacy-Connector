package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;

/**
 * A chunk's blocks for the client. Mods send it after changing many blocks at once (regenerating a chunk, moving
 * blocks) so clients redraw it; it is sent as the modern chunk-with-light packet of the chunk as it now is.
 */
public class S21PacketChunkData extends Packet {
    public int field_149284_a;
    public int field_149282_b;
    public int field_149283_c;
    public int field_149280_d;
    public boolean field_149279_g;
    private final Chunk mcc$chunk;

    public S21PacketChunkData() {
        this.mcc$chunk = null;
    }

    public S21PacketChunkData(Chunk chunk, boolean fullChunk, int sectionMask) {
        this.mcc$chunk = chunk;
        this.field_149284_a = chunk == null ? 0 : chunk.xPosition;
        this.field_149282_b = chunk == null ? 0 : chunk.zPosition;
        this.field_149279_g = fullChunk;
        this.field_149283_c = sectionMask;
    }

    /** The uncompressed 1.7.10 chunk payload: which sections it has, which of them have an add (id high bits) array, and the data. */
    public static class Extracted {
        public byte[] field_150282_a;
        public int field_150280_b;
        public int field_150281_c;

        public Extracted() {
        }
    }

    /**
     * 1.7.10's {@code func_149269_a}: the wire payload of the sections of {@code mask} (empty sections of a full chunk are left out): all block
     * LSB arrays, then metadata, block light, sky light (not in dimensions without sky), the add arrays of the sections that have one, and for
     * a full chunk the 256 biome ids.
     */
    public static Extracted func_149269_a(Chunk chunk, boolean full, int mask) {
        legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        Extracted result = new Extracted();
        java.util.List<byte[]> parts = new java.util.ArrayList<>();
        java.util.List<legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage> used = new java.util.ArrayList<>();
        int adds = 0;
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] != null && (!full || !sections[i].isEmpty()) && (mask & 1 << i) != 0) {
                result.field_150280_b |= 1 << i;
                used.add(sections[i]);
                if (sections[i].getBlockMSBArray() != null) {
                    result.field_150281_c |= 1 << i;
                    adds++;
                }
            }
        }
        boolean hasSky = !legacyconnector.v1_7_10.net.minecraft.world.World.mcc$get$provider(chunk.worldObj).hasNoSky;
        for (var s : used) parts.add(s.getBlockLSBArray());
        for (var s : used) parts.add(s.getMetadataArray().data);
        for (var s : used) parts.add(s.getBlocklightArray().data);
        if (hasSky) for (var s : used) parts.add(s.getSkylightArray().data);
        if (adds > 0) for (var s : used) if (s.getBlockMSBArray() != null) parts.add(s.getBlockMSBArray().data);
        if (full) parts.add(chunk.getBiomeArray());
        int size = 0;
        for (byte[] p : parts) size += p.length;
        byte[] data = new byte[size];
        int at = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, data, at, p.length);
            at += p.length;
        }
        result.field_150282_a = data;
        return result;
    }

    /** The size of the buffer the legacy packet compressed into. */
    public static int func_149275_c() {
        return 196864;
    }

    public int func_149273_e() {
        return field_149284_a;
    }

    public int func_149271_f() {
        return field_149282_b;
    }

    public int func_149276_g() {
        return field_149283_c;
    }

    public int func_149270_h() {
        return field_149280_d;
    }

    public boolean func_149274_i() {
        return field_149279_g;
    }

    public byte[] func_149272_d() {
        return new byte[0];
    }

    /** The modern packet for a chunk the client should reload, or null for chunks not in a live world. */
    static net.minecraft.network.protocol.Packet<?> mcc$modern(net.minecraft.server.level.ServerPlayer player, Chunk chunk) {
        if (chunk == null) return null;
        var level = player.serverLevel();
        net.minecraft.world.level.chunk.LevelChunk live = chunk.mcc$modern() instanceof net.minecraft.world.level.chunk.LevelChunk lc ? lc
                : level.getChunkSource().getChunkNow(chunk.xPosition, chunk.zPosition);
        if (live == null || live.getLevel() != level) return null;
        return new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(live, level.getLightEngine(), null, null);
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var packet = mcc$modern(player, mcc$chunk);
        if (packet != null) player.connection.send(packet);
    }
}
