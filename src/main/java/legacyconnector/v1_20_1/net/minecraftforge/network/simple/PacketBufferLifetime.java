package legacyconnector.v1_20_1.net.minecraftforge.network.simple;

import io.netty.buffer.ByteBuf;

/**
 * Forge 1.20.1 kept a packet's buffer alive until its handler had run, so a decoder or handler could keep the buffer and {@code retain()} it. NeoForge
 * releases the buffer when the decode returns: the channel retains it once more and releases that reference after the handler. A buffer that is already
 * released cannot be held (nothing can be read from it either).
 */
final class PacketBufferLifetime {
    private PacketBufferLifetime() {}

    /** @return true when a reference was taken (to be given back with {@link #release}) */
    static boolean hold(ByteBuf buf) {
        if (buf == null || buf.refCnt() <= 0) return false;
        try {
            buf.retain();
            return true;
        } catch (io.netty.util.IllegalReferenceCountException e) {
            return false;
        }
    }

    static void release(ByteBuf buf, boolean held) {
        if (held && buf != null && buf.refCnt() > 0) buf.release();
    }
}
