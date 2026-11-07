package org.kylepitcock.legacyconnector.compat.jdk;

import java.util.HexFormat;

/**
 * Replacement for {@code javax.xml.bind.annotation.adapters.HexBinaryAdapter} (JAXB left the JDK in Java 11): hex text to bytes and
 * back, upper case like the original. Legacy mods used it to print file hashes.
 */
public class HexBinaryAdapter {
    public HexBinaryAdapter() {}

    public String marshal(byte[] data) {
        return data == null ? null : HexFormat.of().withUpperCase().formatHex(data);
    }

    public byte[] unmarshal(String text) {
        return text == null ? null : HexFormat.of().parseHex(text);
    }
}
