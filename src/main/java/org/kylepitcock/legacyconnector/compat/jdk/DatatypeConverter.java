package org.kylepitcock.legacyconnector.compat.jdk;

import java.util.Base64;
import java.util.HexFormat;

/**
 * Replacement for {@code javax.xml.bind.DatatypeConverter}, removed from the JDK in Java 11 (JEP 320).
 * Legacy mods mostly used it for Base64 and hex encoding of hashes.
 */
public final class DatatypeConverter {
    private DatatypeConverter() {}

    public static String printBase64Binary(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] parseBase64Binary(String s) {
        return Base64.getMimeDecoder().decode(s.trim());
    }

    public static String printHexBinary(byte[] data) {
        return HexFormat.of().withUpperCase().formatHex(data);
    }

    public static byte[] parseHexBinary(String s) {
        return HexFormat.of().parseHex(s);
    }

    public static String printString(String s) {
        return s;
    }

    public static String parseString(String s) {
        return s;
    }

    public static int parseInt(String s) {
        return Integer.parseInt(s.trim());
    }

    public static String printInt(int v) {
        return Integer.toString(v);
    }

    public static long parseLong(String s) {
        return Long.parseLong(s.trim());
    }

    public static String printLong(long v) {
        return Long.toString(v);
    }

    public static boolean parseBoolean(String s) {
        return Boolean.parseBoolean(s.trim());
    }

    public static String printBoolean(boolean v) {
        return Boolean.toString(v);
    }
}
