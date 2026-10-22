package org.kylepitcock.legacyconnector.compat.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Base64;

/** Replacement for the internal {@code sun.misc.BASE64Decoder}, removed in Java 9. */
public class BASE64Decoder {
    public byte[] decodeBuffer(String s) throws IOException {
        try {
            return Base64.getMimeDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    public byte[] decodeBuffer(InputStream in) throws IOException {
        return decodeBuffer(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII));
    }

    public ByteBuffer decodeBufferToByteBuffer(String s) throws IOException {
        return ByteBuffer.wrap(decodeBuffer(s));
    }
}
