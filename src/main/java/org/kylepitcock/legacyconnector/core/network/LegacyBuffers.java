package org.kylepitcock.legacyconnector.core.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * Buffers handed to legacy mods. Their packet code calls {@code buf.array()} (also on the buffers {@code readBytes(n)}
 * derives), which only heap buffers allow; the default allocator prefers direct ones.
 */
public final class LegacyBuffers {
    private static final UnpooledByteBufAllocator HEAP = new UnpooledByteBufAllocator(false);

    private LegacyBuffers() {}

    public static ByteBuf buffer() {
        return HEAP.heapBuffer();
    }

    /**
     * What FML's indexed codec gave {@code IMessage.fromBytes}: a slice after the discriminator byte of one buffer holding the
     * whole packet. Mods read {@code buf.array()} and skip index 0 themselves (DragonAPI's DataPacket copies
     * {@code array()[byteIndex + 1 ...]}); a buffer of the payload alone shifted their data by a byte (ChromatiCraft's packets
     * ended in EOFException).
     */
    public static ByteBuf message(int discriminator, byte[] data) {
        // allocated by HEAP so that readBytes(n)/copy() derive heap buffers too (Unpooled.wrappedBuffer allocates direct ones)
        ByteBuf whole = HEAP.heapBuffer(data.length + 1);
        whole.writeByte(discriminator);
        whole.writeBytes(data);
        return whole.readerIndex(1).slice();
    }

    public static ByteBuf wrap(byte[] data) {
        ByteBuf buf = HEAP.heapBuffer(Math.max(data.length, 1));
        buf.writeBytes(data);
        return buf;
    }
}
