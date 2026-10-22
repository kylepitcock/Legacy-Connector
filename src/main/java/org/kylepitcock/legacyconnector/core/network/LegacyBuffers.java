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

    public static ByteBuf wrap(byte[] data) {
        ByteBuf buf = HEAP.heapBuffer(Math.max(data.length, 1));
        buf.writeBytes(data);
        return buf;
    }
}
