package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import io.netty.buffer.ByteBuf;
import org.kylepitcock.legacyconnector.core.network.LegacyBuffers;

/**
 * The wire form of {@code ICrafting.sendProgressBarUpdate(container, id, value)}. 1.4.7 to 1.7.10 carried the value as a short; the
 * layer sends the full int, so a mod that splits a bigger value itself (IC2: low and high 16 bits, the client ORs them back, and a
 * signed short made the low half negative) gets exactly the int it sent, whatever its range.
 */
public final class ProgressBars {
    private ProgressBars() {}

    public static byte[] encode(int windowId, int id, int value) {
        ByteBuf buf = LegacyBuffers.buffer().writeInt(windowId).writeInt(id).writeInt(value);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    /** {windowId, id, value}. */
    public static int[] decode(byte[] data) {
        ByteBuf buf = LegacyBuffers.wrap(data);
        return new int[] {buf.readInt(), buf.readInt(), buf.readInt()};
    }
}
