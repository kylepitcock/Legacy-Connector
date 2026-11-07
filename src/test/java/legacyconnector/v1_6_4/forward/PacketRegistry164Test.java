package legacyconnector.v1_6_4.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet255KickDisconnect;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet32EntityLook;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import org.junit.jupiter.api.Test;

/** Reading whole packets (id byte, then data) by the layer's own classes. */
class PacketRegistry164Test {
    @Test
    void builtInPacketsAreCreatedByIdAndReadBack() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Packet.writePacket(new Packet32EntityLook(12, (byte) 7, (byte) -7), new DataOutputStream(bytes));
        Packet.writePacket(new Packet255KickDisconnect("bye"), new DataOutputStream(bytes));
        assertEquals(32, bytes.toByteArray()[0]);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        Packet look = Packets164.readPacket(null, in, true, null);
        assertInstanceOf(Packet32EntityLook.class, look);
        assertEquals(12, ((Packet32EntityLook) look).entityId);
        assertEquals(-7, ((Packet32EntityLook) look).pitch);
        Packet kick = Packets164.readPacket(in, false, null);
        assertEquals("bye", ((Packet255KickDisconnect) kick).reason);
        assertNull(Packets164.readPacket(in, false, null), "the end of the stream");
    }

    @Test
    void unknownIdsAreAnError() {
        assertNull(Packets164.getNewPacket(null, 199));
        assertThrows(IOException.class, () -> Packets164.readPacket(new DataInputStream(new ByteArrayInputStream(new byte[] {(byte) 199})), true, null));
    }
}
