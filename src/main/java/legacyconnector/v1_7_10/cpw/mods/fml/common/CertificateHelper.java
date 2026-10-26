package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.cert.Certificate;

/** SHA-1 fingerprints of signing certificates, as FML compared them with {@code @Mod(certificateFingerprint)}. */
public class CertificateHelper {
    private static final String HEXES = "0123456789abcdef";

    public static String getFingerprint(Certificate certificate) {
        if (certificate == null) return "NO VALID CERTIFICATE FOUND";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(certificate.getEncoded());
            return hexify(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String getFingerprint(ByteBuffer buffer) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(buffer);
            return hexify(digest.digest());
        } catch (Exception e) {
            return null;
        }
    }

    private static String hexify(byte[] chksum) {
        StringBuilder hex = new StringBuilder(2 * chksum.length);
        for (byte b : chksum) hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt(b & 0x0F));
        return hex.toString();
    }
}
