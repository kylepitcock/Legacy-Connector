package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import org.junit.jupiter.api.Test;

/** The 1.7.10 wire form of a tab list entry survives a round trip through the legacy buffer. */
class S38PacketPlayerListItemTest {
    @Test
    void wireFormRoundTrips() throws IOException {
        S38PacketPlayerListItem out = new S38PacketPlayerListItem("Herobrine", true, 42);
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        out.writePacketData(buffer);

        S38PacketPlayerListItem in = new S38PacketPlayerListItem();
        in.readPacketData(buffer);
        assertEquals("Herobrine", in.func_149122_c());
        assertTrue(in.func_149121_d());
        assertEquals(42, in.func_149120_e());
    }
}
