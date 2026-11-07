package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class PayloadWireTest {
    @Test
    void bytesAreCutToAShorterLength() {
        byte[] data = {1, 2, 3, 4};
        assertArrayEquals(new byte[] {1, 2}, PayloadWire.bytes(data, 2));
        assertArrayEquals(data, PayloadWire.bytes(data, 0));
        assertArrayEquals(data, PayloadWire.bytes(data, 9));
        assertEquals(0, PayloadWire.bytes(null, 5).length);
    }

    @Test
    void stringsRoundTrip() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PayloadWire.writeString("MC|Brand", new DataOutputStream(bytes));
        assertEquals("MC|Brand", PayloadWire.readString(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 20));
        assertEquals(2 + 16, bytes.size());
    }

    @Test
    void overlongStringsAreRejected() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new DataOutputStream(bytes).writeShort(40);
        assertThrows(IOException.class, () -> PayloadWire.readString(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 20));
    }
}
