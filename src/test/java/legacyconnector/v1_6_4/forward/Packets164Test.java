package legacyconnector.v1_6_4.forward;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import org.junit.jupiter.api.Test;

/** The legacy Packet statics: string / byte array wire formats and the id registry mods add their own packets to. */
class Packets164Test {
    public static class CustomPacket extends Packet {}

    @Test
    void stringsAreAShortLengthThenUtf16Chars() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Packets164.writeString("abé", new DataOutputStream(bytes));
        assertArrayEquals(new byte[] {0, 3, 0, 'a', 0, 'b', 0, (byte) 0xe9}, bytes.toByteArray());
        assertEquals("abé", Packets164.readString(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 10));
        assertThrows(IOException.class, () -> Packets164.readString(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 2));
    }

    @Test
    void byteArraysAreAShortLengthThenTheBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Packets164.writeByteArray(new DataOutputStream(bytes), new byte[] {9, 8, 7});
        assertArrayEquals(new byte[] {0, 3, 9, 8, 7}, bytes.toByteArray());
        assertArrayEquals(new byte[] {9, 8, 7}, Packets164.readBytesFromStream(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        assertThrows(IOException.class, () -> Packets164.readBytesFromStream(new DataInputStream(new ByteArrayInputStream(new byte[] {-1, -1}))));
    }

    @Test
    void absentNbtIsAMinusOneLength() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Packets164.writeNBTTagCompound(null, new DataOutputStream(bytes));
        assertArrayEquals(new byte[] {-1, -1}, bytes.toByteArray());
        assertNull(Packets164.readNBTTagCompound(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }

    @Test
    void registeredPacketsGetTheirIdAndAreBuiltById() {
        Packets164.addIdClassMapping(231, true, true, CustomPacket.class);
        assertEquals(231, new CustomPacket().getPacketId());
        assertInstanceOf(CustomPacket.class, Packets164.getNewPacket(231));
        assertNull(Packets164.getNewPacket(232));
        assertThrows(IllegalArgumentException.class, () -> Packets164.addIdClassMapping(231, true, true, CustomPacket.class));
    }
}
