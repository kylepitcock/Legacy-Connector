package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/** The byte-level rules of 1.6.4's custom payload packet (no game types, so it is unit tested). */
public final class PayloadWire {
    private PayloadWire() {}

    /** The bytes to send: {@code data}, cut to {@code length} when a mod set a shorter one (a longer or zero length means all of it). */
    public static byte[] bytes(byte[] data, int length) {
        if (data == null) return new byte[0];
        if (length > 0 && length < data.length) return Arrays.copyOf(data, length);
        return data;
    }

    /** 1.6.4's string format: a short length, then UTF-16 characters. */
    public static String readString(DataInput in, int max) throws IOException {
        short len = in.readShort();
        if (len > max || len < 0) throw new IOException("Received string length longer than maximum allowed (" + len + " > " + max + ")");
        StringBuilder s = new StringBuilder(len);
        for (int i = 0; i < len; i++) s.append(in.readChar());
        return s.toString();
    }

    public static void writeString(String s, DataOutput out) throws IOException {
        String v = s == null ? "" : s;
        out.writeShort(v.length());
        out.writeChars(v);
    }
}
