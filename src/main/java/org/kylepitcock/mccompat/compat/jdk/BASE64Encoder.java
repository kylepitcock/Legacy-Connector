package org.kylepitcock.mccompat.compat.jdk;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;

/** Replacement for the internal {@code sun.misc.BASE64Encoder}, removed in Java 9. MIME-style line wrapping. */
public class BASE64Encoder {
    public String encode(byte[] data) {
        return Base64.getMimeEncoder(76, new byte[] {'\n'}).encodeToString(data);
    }

    public String encode(ByteBuffer buffer) {
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return encode(data);
    }

    public String encodeBuffer(byte[] data) {
        return encode(data) + "\n";
    }

    public void encode(byte[] data, OutputStream out) throws IOException {
        out.write(encode(data).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    public void encodeBuffer(byte[] data, OutputStream out) throws IOException {
        out.write(encodeBuffer(data).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
}
