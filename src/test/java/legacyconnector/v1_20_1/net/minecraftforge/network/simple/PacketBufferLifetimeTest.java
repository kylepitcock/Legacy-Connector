package legacyconnector.v1_20_1.net.minecraftforge.network.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class PacketBufferLifetimeTest {
    @Test
    void heldBufferSurvivesTheNetworkReleaseAndIsFreedAfterTheHandler() {
        ByteBuf buf = Unpooled.buffer().writeInt(7);
        assertTrue(PacketBufferLifetime.hold(buf));
        buf.release();                       // NeoForge releases when the decode returns
        assertEquals(1, buf.refCnt());       // the handler can still read and retain() it
        buf.retain();                        // a mod keeping the buffer
        PacketBufferLifetime.release(buf, true);
        assertEquals(1, buf.refCnt());
        assertEquals(7, buf.readInt());
        buf.release();
        assertEquals(0, buf.refCnt());
    }

    @Test
    void anAlreadyReleasedBufferIsNotHeld() {
        ByteBuf buf = Unpooled.buffer();
        buf.release();
        assertFalse(PacketBufferLifetime.hold(buf));
        PacketBufferLifetime.release(buf, false);   // no double release
        assertEquals(0, buf.refCnt());
    }
}
